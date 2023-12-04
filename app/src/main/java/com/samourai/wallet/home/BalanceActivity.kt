package com.samourai.wallet.home

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.dm.zbar.android.scanner.ZBarConstants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.gson.Gson
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.R
import com.samourai.wallet.ReceiveActivity
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.APIFactory.TxMostRecentDateComparator
import com.samourai.wallet.api.Tx
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.cahoots.Cahoots
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.collaborate.CollaborateActivity
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.databinding.ActivityBalanceBinding
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.fragments.ScanFragment
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.home.adapters.TxAdapter
import com.samourai.wallet.network.NetworkDashboard
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.payload.ExternalBackupManager.askPermission
import com.samourai.wallet.payload.ExternalBackupManager.hasPermissions
import com.samourai.wallet.payload.ExternalBackupManager.onActivityResult
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.PayNymHome
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.paynym.fragments.PayNymOnBoardBottomSheet
import com.samourai.wallet.paynym.models.NymResponse
import com.samourai.wallet.ricochet.RicochetMeta
import com.samourai.wallet.segwit.bech32.Bech32Util
import com.samourai.wallet.send.BlockedUTXO
import com.samourai.wallet.send.MyTransactionOutPoint
import com.samourai.wallet.send.SendActivity
import com.samourai.wallet.send.batch.BatchSpendActivity
import com.samourai.wallet.send.batch.InputBatchSpendHelper.*
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.service.WalletRefreshWorker
import com.samourai.wallet.settings.SettingsActivity
import com.samourai.wallet.stealth.StealthModeController
import com.samourai.wallet.tools.ToolsBottomSheet
import com.samourai.wallet.tools.viewmodels.Auth47ViewModel
import com.samourai.wallet.tor.EnumTorState
import com.samourai.wallet.tor.SamouraiTorManager
import com.samourai.wallet.tor.TorState
import com.samourai.wallet.tx.TxDetailsActivity
import com.samourai.wallet.util.tech.AppUtil
import com.samourai.wallet.util.network.BlockExplorerUtil
import com.samourai.wallet.util.CharSequenceX
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.tech.LogUtil
import com.samourai.wallet.util.func.MessageSignUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.wallet.util.PrivKeyReader
import com.samourai.wallet.util.TimeOutUtil
import com.samourai.wallet.util.activity.ActivityHelper
import com.samourai.wallet.util.tech.askNotificationPermission
import com.samourai.wallet.utxos.UTXOSActivity
import com.samourai.wallet.whirlpool.WhirlpoolHome
import com.samourai.wallet.whirlpool.WhirlpoolMeta
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService
import com.samourai.wallet.widgets.ItemDividerDecorator
import com.samourai.wallet.widgets.popUpMenu.popupMenu
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.bitcoinj.script.Script
import org.bouncycastle.util.encoders.Hex
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Collections


open class BalanceActivity : SamouraiActivity() {
    private var txs: MutableList<Tx>? = null
    private var ricochetQueueTask: RicochetQueueTask? = null
    private val balanceViewModel: BalanceViewModel by viewModels()
    private lateinit var binding: ActivityBalanceBinding
    private var menu: Menu? = null
    private val menuTorIcon: ImageView? = null

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_INTENT == intent.action) {
                if (binding.progressBar != null) {
                    showProgress()
                }
                val notifTx = intent.getBooleanExtra("notifTx", false)
                val fetch = intent.getBooleanExtra("fetch", false)
                val rbfHash: String?
                val blkHash: String?
                rbfHash = if (intent.hasExtra("rbf")) {
                    intent.getStringExtra("rbf")
                } else {
                    null
                }
                blkHash = if (intent.hasExtra("hash")) {
                    intent.getStringExtra("hash")
                } else {
                    null
                }
                val handler = Handler()
                handler.post {
                    refreshTx(notifTx, false, false)
                    if (this@BalanceActivity != null) {
                        if (rbfHash != null) {
                            MaterialAlertDialogBuilder(this@BalanceActivity)
                                .setTitle(R.string.app_name)
                                .setMessage(rbfHash + "\n\n" + getString(R.string.rbf_incoming))
                                .setCancelable(true)
                                .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, whichButton -> doExplorerView(rbfHash) })
                                .setNegativeButton(R.string.no, object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface, whichButton: Int) {
                                    }
                                }).show()
                        }
                    }
                }
            }
        }
    }
    private var receiverDisplay: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (DISPLAY_INTENT == intent.action) {
                updateDisplay(true)
                checkDust()
            }
        }
    }

    private fun checkDust() {
        balanceViewModel.viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val utxos = APIFactory.getInstance(this@BalanceActivity).getUtxos(false)
                val utxoWarnings = arrayListOf<MyTransactionOutPoint>()
                for (utxo in utxos) {
                    val outpoints = utxo.outpoints
                    for (out in outpoints) {
                        val scriptBytes = out.scriptBytes
                        var address: String? = null
                        try {
                            address = if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(scriptBytes))) {
                                Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(scriptBytes))
                            } else {
                                Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().currentNetworkParams).toString()
                            }
                        } catch (e: Exception) {
                        }
                        val path = APIFactory.getInstance(this@BalanceActivity).unspentPaths[address]
                        if (path != null && path.startsWith("M/1/")) {
                            continue
                        }
                        val hash = out.hash.toString()
                        val idx = out.txOutputN
                        val amount = out.value.longValue()
                        val contains = BlockedUTXO.getInstance().contains(hash, idx) || BlockedUTXO.getInstance().containsNotDusted(hash, idx)
                        val containsInPostMix = BlockedUTXO.getInstance().containsPostMix(hash, idx) || BlockedUTXO.getInstance().containsNotDustedPostMix(hash, idx)
                        if (amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && !contains && !containsInPostMix) {
                            utxoWarnings.add(out);
//                            BalanceActivity.this.runOnUiThread(new Runnable() {
//                            @Override
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    utxoWarnings.forEach {
                        val hash = it.hash.toString()
                        val idx = it.txOutputN
                        val amount = it.value.longValue()
                        var message: String? = this@BalanceActivity.getString(R.string.dusting_attempt)
                        message += "\n\n"
                        message += this@BalanceActivity.getString(R.string.dusting_attempt_amount)
                        message += " "
                        message += FormatsUtil.formatBTC(amount)
                        message += this@BalanceActivity.getString(R.string.dusting_attempt_id)
                        message += " "
                        message += "$hash-$idx"
                        val dlg = MaterialAlertDialogBuilder(this@BalanceActivity)
                            .setTitle(R.string.dusting_tx)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.dusting_attempt_mark_unspendable) { dialog, whichButton ->
                                if (account == WhirlpoolMeta.getInstance(this@BalanceActivity).whirlpoolPostmix) {
                                    BlockedUTXO.getInstance().addPostMix(hash, idx, amount)
                                } else {
                                    BlockedUTXO.getInstance().add(hash, idx, amount)
                                }
                                saveState()
                            }.setNegativeButton(R.string.dusting_attempt_ignore) { dialog, whichButton ->
                                if (account == WhirlpoolMeta.getInstance(this@BalanceActivity).whirlpoolPostmix) {
                                    BlockedUTXO.getInstance().addNotDustedPostMix(hash, idx)
                                } else {
                                    BlockedUTXO.getInstance().addNotDusted(hash, idx)
                                }
                                saveState()
                            }
                        if (!isFinishing) {
                            dlg.show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //Switch themes based on accounts (blue theme for whirlpool account)
        setSwitchThemes(true)
        super.onCreate(savedInstanceState)
        binding = ActivityBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        balanceViewModel.setAccount(account)
        makePaynymAvatarCache()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setSupportActionBar(binding.toolbar)
        binding.rvTxes.layoutManager = LinearLayoutManager(this)
        val drawable = ContextCompat.getDrawable(this, R.drawable.divider_grey)
        binding.rvTxes.addItemDecoration(ItemDividerDecorator(drawable))
        txs = ArrayList()
        val is_sat_prefs = PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.IS_SAT, false)
        findViewById<View>(R.id.whirlpool_fab).setOnClickListener { view: View? ->
            val intent = Intent(this@BalanceActivity, WhirlpoolHome::class.java)
            startActivity(intent)
            binding.fabMenu.toggle(true)
        }
        binding.sendFab.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@BalanceActivity, AccountSelectionActivity::class.java)
            intent.putExtra("via_menu", true)
            startActivity(intent)
            binding.fabMenu.toggle(true)
        })
        var payload: JSONObject? = null
        payload = try {
            PayloadUtil.getInstance(this@BalanceActivity).payload
        } catch (e: Exception) {
            AppUtil.getInstance(applicationContext).restartApp()
            e.printStackTrace()
            return
        }
        if (account == 0 && payload != null && payload.has("prev_balance")) {
            try {
                setBalance(payload.getLong("prev_balance"), is_sat_prefs)
            } catch (e: Exception) {
                setBalance(0L, is_sat_prefs)
            }
        } else {
            setBalance(0L, is_sat_prefs)
        }
        binding.receiveFab.setOnClickListener { view: View? ->
            binding.fabMenu.toggle(true)
            val hdw = HD_WalletFactory.getInstance(this@BalanceActivity).get()
            if (hdw != null) {
                val intent = Intent(this@BalanceActivity, ReceiveActivity::class.java)
                startActivity(intent)
            }
        }
        binding.paynymFab.setOnClickListener { view: View? ->
            binding.fabMenu.toggle(true)
            val intent = Intent(this@BalanceActivity, PayNymHome::class.java)
            startActivity(intent)
        }
        binding.txSwipeContainer.setOnRefreshListener(OnRefreshListener {
            doClipboardCheck()
            refreshTx(false, true, false)
            binding.txSwipeContainer.isRefreshing = false
            showProgress()
        })
        val filter = IntentFilter(ACTION_INTENT)
        LocalBroadcastManager.getInstance(this@BalanceActivity).registerReceiver(receiver, filter)
        val filterDisplay = IntentFilter(DISPLAY_INTENT)
        LocalBroadcastManager.getInstance(this@BalanceActivity).registerReceiver(receiverDisplay, filterDisplay)
        if (PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.AUTO_BACKUP, true)) {
            if (!hasPermissions()) askPermission(this)
        }
        if (PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.PAYNYM_CLAIMED, false) && !PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, false)) {
            doFeaturePayNymUpdate()
        } else if (!PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.PAYNYM_CLAIMED, false) &&
            !PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.PAYNYM_REFUSED, false)
        ) {
            val payNymOnBoardBottomSheet = PayNymOnBoardBottomSheet()
            payNymOnBoardBottomSheet.show(supportFragmentManager, payNymOnBoardBottomSheet.tag)
        }
        if (RicochetMeta.getInstance(this@BalanceActivity).queue.size > 0) {
            if (ricochetQueueTask == null || ricochetQueueTask!!.status == AsyncTask.Status.FINISHED) {
                ricochetQueueTask = RicochetQueueTask()
                ricochetQueueTask!!.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR)
            }
        }
        if (!AppUtil.getInstance(this@BalanceActivity).isClipboardSeen) {
            doClipboardCheck()
        }
        setUpTor()
        initViewModel()
        showProgress()
        if (account == 0) {
            BIP47Util.getInstance(applicationContext)
                .payNymLogoLive.observe(this) {
                    binding.toolbarIcon.setImageBitmap(it)
                }
            binding.toolbarIcon.setOnClickListener {
                showToolOptions(it)
            }
            val delayedHandler = Handler()
            delayedHandler.postDelayed({
                var notifTx = false
                val extras = intent.extras
                if (extras != null && extras.containsKey("notifTx")) {
                    notifTx = extras.getBoolean("notifTx")
                }
                refreshTx(notifTx, false, true)
                updateDisplay(false)
            }, 100L)
        } else {
            binding.toolbarIcon.visibility = View.GONE
            binding.toolbar.setTitleMargin(0, 0, 0, 0)
            binding.toolbar.titleMarginEnd = -50
            binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
            binding.toolbar.setNavigationOnClickListener {
                super.onBackPressed()
            }
            binding.receiveFab.visibility = View.GONE
            binding.whirlpoolFab.visibility = View.GONE
            binding.paynymFab.visibility = View.GONE
            Handler().postDelayed({ updateDisplay(true) }, 600L)
        }
        balanceViewModel.loadOfflineData()

        updateDisplay(false)
        checkDeepLinks()
        doExternalBackUp()
        AppUtil.getInstance(applicationContext).walletLoading.observe(this) {
            if (it) {
                showProgress()
            } else {
                hideProgress()
            }
        }
        if (intent.getBooleanExtra("refresh", false)) {
            balanceViewModel.viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    delay(800)
                    WalletRefreshWorker.enqueue(applicationContext, notifTx = false, launched = false)
                }
            }
        }

        balanceViewModel.viewModelScope.launch {
            withContext(Dispatchers.Main) {
                askNotificationPermission(this@BalanceActivity)
            }
        }
    }

    private fun showToolOptions(it: View) {
        val bitmapImage = BIP47Util.getInstance(applicationContext).payNymLogoLive.value
        var drawable = ContextCompat.getDrawable(this, R.drawable.ic_samourai_logo)
        var nym = PrefsUtil.getInstance(applicationContext)
            .getValue(PrefsUtil.PAYNYM_BOT_NAME, BIP47Meta.getInstance().getDisplayLabel(BIP47Util.getInstance(applicationContext).paymentCode.toString()))
        if (bitmapImage != null) {
            drawable = BitmapDrawable(resources, bitmapImage)
        }
        if (nym.isNullOrEmpty()) {
            nym = BIP47Meta.getInstance().getDisplayLabel(BIP47Util.getInstance(applicationContext).paymentCode.toString())
        }
        val toolWindowSize = applicationContext.resources.displayMetrics.density * 220;
        val popupMenu = popupMenu {
            fixedContentWidthInPx = toolWindowSize.toInt()
            style = R.style.Theme_Samourai_Widget_MPM_Menu_Dark
            section {
                item {
                    label = nym
                    iconDrawable = drawable
                    iconSize = 34
                    labelColor = ContextCompat.getColor(applicationContext, R.color.white)
                    disableTint = true
                    iconShapeAppearanceModel = ShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(resources.getDimension(R.dimen.qr_image_corner_radius))
                        .build()
                    callback = {
                        val intent = Intent(this@BalanceActivity, PayNymHome::class.java)
                        startActivity(intent)
                    }
                }
                item {
                    label = "Collaborate"
                    iconSize = 18
                    callback = {
                        val intent = Intent(this@BalanceActivity, CollaborateActivity::class.java)
                        startActivity(intent)
                    }
                    icon = R.drawable.ic_connect_without_contact
                }
                item {
                    label = "Tools"
                    icon = R.drawable.ic_tools
                    iconSize = 18
                    hasNestedItems
                    callback = {
                        ToolsBottomSheet.showTools(supportFragmentManager)
                    }
                }

            }
            section {
                item {
                    label = getString(R.string.action_settings)
                    icon = R.drawable.ic_cog
                    iconSize = 18
                    callback = {
                        TimeOutUtil.getInstance().updatePin()
                        val intent = Intent(this@BalanceActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                item {
                    label = "Exit Wallet"
                    iconSize = 18
                    iconColor = ContextCompat.getColor(this@BalanceActivity, R.color.mpm_red)
                    labelColor = ContextCompat.getColor(this@BalanceActivity, R.color.mpm_red)
                    icon = R.drawable.ic_baseline_power_settings_new_24
                    callback = {
                        this@BalanceActivity.onBackPressed()
                    }
                }
            }
        }
        popupMenu.show(this@BalanceActivity, it)
    }

    private fun hideProgress() {
        binding.progressBar.hide()
    }

    private fun showProgress() {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.show()
    }

    private fun checkDeepLinks() {
        val bundle = intent.extras ?: return
        if (bundle.containsKey("pcode") || bundle.containsKey("uri") || bundle.containsKey("amount")) {
            if (bundle.containsKey("uri")) {
                if (bundle.getString("uri")?.startsWith("auth47") == true) {
                    ToolsBottomSheet.showTools(supportFragmentManager, ToolsBottomSheet.ToolType.AUTH47,
                        bundle = Bundle().apply {
                            putString("KEY", bundle.getString("uri"))
                        })
                    return;
                }
            }
            if (balanceViewModel.balance.value != null) bundle.putLong("balance", balanceViewModel.balance.value!!)
            val intent = Intent(this, AccountSelectionActivity::class.java)
            intent.putExtra("_account", account)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun initViewModel() {
        val adapter = TxAdapter(applicationContext, ArrayList(), account)
        adapter.setClickListener { position: Int, tx: Tx -> txDetails(tx) }
        binding.rvTxes.adapter = adapter
        val is_sat_prefs = PrefsUtil.getInstance(this@BalanceActivity).getValue(PrefsUtil.IS_SAT, false)
        balanceViewModel.balance.observe(this) { balance: Long? ->
            if (balance == null) {
                return@observe
            }
            if (balance < 0) {
                return@observe
            }
            if (binding.progressBar.visibility == View.VISIBLE && balance <= 0) {
                return@observe
            }
            setBalance(balance, is_sat_prefs)
        }
        adapter.setTxes(balanceViewModel.txs.value)
        setBalance(balanceViewModel.balance.value, is_sat_prefs)
        balanceViewModel.satState.observe(this) { state: Boolean? ->
            var isSats = false
            if (state != null) {
                isSats = state
            }
            setBalance(balanceViewModel.balance.value, isSats)
            adapter.notifyDataSetChanged()
        }
        balanceViewModel.txs.observe(this) { list -> adapter.setTxes(list) }
        binding.toolbarLayout.setOnClickListener { v: View? ->
            val is_sat = balanceViewModel!!.toggleSat()
            PrefsUtil.getInstance(this@BalanceActivity).setValue(PrefsUtil.IS_SAT, is_sat)
        }
        binding.toolbarLayout.setOnLongClickListener {
            val intent = Intent(this@BalanceActivity, UTXOSActivity::class.java)
            intent.putExtra("_account", account)
            startActivityForResult(intent, UTXO_REQUESTCODE)
            false
        }
    }

    private fun setBalance(balance: Long?, isSat: Boolean) {
        if (balance == null) {
            return
        }
        if (supportActionBar != null) {
            TransitionManager.beginDelayedTransition(binding.toolbarLayout, ChangeBounds())
            val displayAmount = if (isSat) FormatsUtil.formatSats(balance) else FormatsUtil.formatBTC(balance)
            binding.toolbar.title = displayAmount
            title = displayAmount
            binding.toolbarLayout.title = displayAmount
        }
    }

    public override fun onResume() {
        super.onResume()

//        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(BalanceActivity.this).registerReceiver(receiver, filter);
        AppUtil.getInstance(this@BalanceActivity).checkTimeOut()
        try {
            val isSatPrefs = PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.IS_SAT, false)
            if (isSatPrefs != balanceViewModel.satState.value) {
                balanceViewModel.toggleSat()
            }
        } catch (e: Exception) {
            LogUtil.error(TAG, e)
        }
        //
//        Intent intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
//        LocalBroadcastManager.getInstance(BalanceActivity.this).sendBroadcast(intent);
    }

    fun createTag(text: String?): View {
        val scale = resources.displayMetrics.density
        val lparams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val textView = TextView(applicationContext)
        textView.text = text
        textView.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
        textView.layoutParams = lparams
        textView.setBackgroundResource(R.drawable.tag_round_shape)
        textView.setPadding((8 * scale + 0.5f).toInt(), (6 * scale + 0.5f).toInt(), (8 * scale + 0.5f).toInt(), (6 * scale + 0.5f).toInt())
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        return textView
    }

    private fun makePaynymAvatarCache() {
        try {
            val paymentCodes = ArrayList(BIP47Meta.getInstance().getSortedByLabels(false, true))
            if (PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.PAYNYM_BOT_NAME, "").isNullOrEmpty()
                && PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.PAYNYM_CLAIMED,false))  {
                val strPaymentCode = BIP47Util.getInstance(application).paymentCode.toString()
                val apiService = PayNymApiService.getInstance(strPaymentCode, getApplication());
                balanceViewModel.viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val response = apiService.getNymInfo()
                            if (response.isSuccessful) {
                                val responseJson = response.body?.string()
                                if (responseJson != null) {
                                    val jsonObject = JSONObject(responseJson)
                                    val nym = Gson().fromJson(jsonObject.toString(), NymResponse::class.java);
                                    PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.PAYNYM_BOT_NAME, nym.nymName)

                                } else
                                    throw Exception("Invalid response ")
                            }
                        } catch (_: Exception) {

                        }
                    }
                }
            }
            if (!BIP47Util.getInstance(applicationContext).avatarImage().exists()) {
                BIP47Util.getInstance(applicationContext).fetchBotImage()
                    .subscribe()
                    .apply {
                        registerDisposable(this)
                        balanceViewModel.viewModelScope.launch {
                            withContext(Dispatchers.Default) {
                                val bitmap = BitmapFactory.decodeFile(BIP47Util.getInstance(applicationContext).avatarImage().path)
                                BIP47Util.getInstance(applicationContext)
                                    .setAvatar(bitmap)
                            }
                        }
                    }
            }
            balanceViewModel.viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    if (BIP47Util.getInstance(applicationContext).avatarImage().exists()) {
                        val bitmap = BitmapFactory.decodeFile(BIP47Util.getInstance(applicationContext).avatarImage().path)
                        BIP47Util.getInstance(applicationContext)
                            .setAvatar(bitmap)
                    }
                }
            }
            for (code in paymentCodes) {
                Picasso.get()
                    .load(WebUtil.PAYNYM_API + code + "/avatar").fetch(object : Callback {
                        override fun onSuccess() {
                            /*NO OP*/
                        }

                        override fun onError(e: Exception) {
                            /*NO OP*/
                        }
                    })
            }
        } catch (ignored: Exception) {
        }
    }

    public override fun onDestroy() {
        LocalBroadcastManager.getInstance(this@BalanceActivity).unregisterReceiver(receiver)
        LocalBroadcastManager.getInstance(this@BalanceActivity).unregisterReceiver(receiverDisplay)

        if (PrefsUtil.getInstance(this.application).getValue(StealthModeController.PREF_ENABLED, false)) {
            StealthModeController.enableStealth(applicationContext)
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (BuildConfig.FLAVOR == "staging") menu.findItem(R.id.action_mock_fees).isVisible = true
        menu.findItem(R.id.action_refresh).isVisible = false
        menu.findItem(R.id.action_share_receive).isVisible = false
        menu.findItem(R.id.action_ricochet).isVisible = false
        menu.findItem(R.id.action_empty_ricochet).isVisible = false
        menu.findItem(R.id.action_sign).isVisible = false
        menu.findItem(R.id.action_fees).isVisible = false
        menu.findItem(R.id.action_batch).isVisible = false
        WhirlpoolMeta.getInstance(applicationContext)
        if (account == WhirlpoolMeta.getInstance(applicationContext).whirlpoolPostmix) {
            menu.findItem(R.id.action_backup).isVisible = false
            menu.findItem(R.id.action_network_dashboard).isVisible = false
            menu.findItem(R.id.action_postmix_balance).isVisible = false
            val item = menu.findItem(R.id.action_menu_account)
            item.actionView = createTag(" POST-MIX ")
            item.isVisible = true
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return super.onOptionsItemSelected(item)
        }
        if (id == R.id.action_mock_fees) {
            SamouraiWallet.MOCK_FEE = !SamouraiWallet.MOCK_FEE
            refreshTx(false, true, false)
            binding.txSwipeContainer.isRefreshing = false
            showProgress()
            return super.onOptionsItemSelected(item)
        }
        if (id == R.id.action_postmix_balance) {
            startActivity(Intent(this, BalanceActivity::class.java).apply {
                putExtra("_account", SamouraiAccountIndex.POSTMIX)
            })
            return super.onOptionsItemSelected(item)
        }

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_network_dashboard) {
            startActivity(Intent(this, NetworkDashboard::class.java))
        } // noinspection SimplifiableIfStatement
        if (id == R.id.action_support) {
            ActivityHelper.launchSupportPageInBrowser(this, SamouraiTorManager.isConnected())
        } else if (id == R.id.action_utxo) {
            doUTXO()
        } else if (id == R.id.action_backup) {
            if (SamouraiWallet.getInstance().hasPassphrase(this@BalanceActivity)) {
                if (HD_WalletFactory.getInstance(this@BalanceActivity).get() != null && SamouraiWallet.getInstance().hasPassphrase(this@BalanceActivity)) {
                    doBackup(HD_WalletFactory.getInstance(this@BalanceActivity).get().passphrase)
                }
            } else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle(R.string.enter_backup_password)
                val view = layoutInflater.inflate(R.layout.password_input_dialog_layout, null)
                val password = view.findViewById<EditText>(R.id.restore_dialog_password_edittext)
                val message = view.findViewById<TextView>(R.id.dialogMessage)
                message.setText(R.string.backup_password)
                builder.setPositiveButton(R.string.confirm) { dialog: DialogInterface, which: Int ->
                    val pw = password.text.toString()
                    if (pw.length >= AppUtil.MIN_BACKUP_PW_LENGTH && pw.length <= AppUtil.MAX_BACKUP_PW_LENGTH) {
                        doBackup(pw)
                    } else {
                        Toast.makeText(applicationContext, R.string.password_error, Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                builder.setView(view)
                builder.show()
            }
        } else if (id == R.id.action_scan_qr) {
            doScan()
        } else {
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpTor() {
        SamouraiTorManager.getTorStateLiveData().observe(this) { torState: TorState ->
            if (torState.state == EnumTorState.ON) {
                PrefsUtil.getInstance(this).setValue(PrefsUtil.ENABLE_TOR, true)
                binding.progressBar.visibility = View.INVISIBLE
                menuTorIcon?.setImageResource(R.drawable.tor_on)
            } else if (torState.state == EnumTorState.STARTING) {
                binding.progressBar.visibility = View.VISIBLE
                menuTorIcon?.setImageResource(R.drawable.tor_on)
            } else {
                binding.progressBar.visibility = View.INVISIBLE
                menuTorIcon?.setImageResource(R.drawable.tor_off)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResult(requestCode, resultCode, data, application)
        if (resultCode == RESULT_OK && requestCode == SCAN_COLD_STORAGE) {
            if (data?.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {
                val strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT)
                doPrivKey(strResult)
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == SCAN_COLD_STORAGE) {
        } else if (resultCode == RESULT_OK && requestCode == SCAN_QR) {

            if (data?.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {
                val strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT)
                val params = SamouraiWallet.getInstance().currentNetworkParams
                val privKeyReader = PrivKeyReader(strResult, params)
                try {
                    if (privKeyReader.format != null) {
                        doPrivKey(strResult!!.trim { it <= ' ' })
                    } else if (strResult?.lowercase()?.startsWith(Auth47ViewModel.AUTH_SCHEME.lowercase()) == true) {
                        ToolsBottomSheet.showTools(supportFragmentManager, ToolsBottomSheet.ToolType.AUTH47,
                            bundle = Bundle().apply {
                                putString("KEY", strResult)
                            })
                    } else if (Cahoots.isCahoots(strResult!!.trim { it <= ' ' })) {
                        val cahootIntent = ManualCahootsActivity.createIntentResume(this, account, strResult.trim { it <= ' ' })
                        startActivity(cahootIntent)
                    } else if (FormatsUtil.getInstance().isPSBT(strResult.trim { it <= ' ' })) {
                        PSBTUtil.getInstance(this@BalanceActivity).doPSBT(strResult.trim { it <= ' ' })
                    } else if (DojoUtil.getInstance(this@BalanceActivity).isValidPairingPayload(strResult.trim { it <= ' ' })) {
                        val intent = Intent(this@BalanceActivity, NetworkDashboard::class.java)
                        intent.putExtra("params", strResult.trim { it <= ' ' })
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@BalanceActivity, AccountSelectionActivity::class.java)
                        intent.putExtra("uri", strResult.trim { it <= ' ' })
                        intent.putExtra("_account", account)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == UTXO_REQUESTCODE) {
            refreshTx(false, false, false)
            showProgress()
        } else {
        }
    }

    override fun onBackPressed() {
        if (account == 0) {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setMessage(R.string.ask_you_sure_exit)
            val alert = builder.create()
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes)) { dialog: DialogInterface?, id: Int ->
                doExternalBackUp()
                // disconnect Whirlpool on app back key exit
                if (WhirlpoolNotificationService.isRunning(applicationContext)) WhirlpoolNotificationService.stopService(applicationContext)
                if (SamouraiTorManager.isConnected()) {
                    SamouraiTorManager.stop()
                }
                TimeOutUtil.getInstance().reset()
                if (StealthModeController.isStealthEnabled(applicationContext)) {
                    StealthModeController.enableStealth(applicationContext)
                }
                finishAffinity()
                finish()
                super.onBackPressed()
            }
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no)) { dialog: DialogInterface, id: Int -> dialog.dismiss() }
            alert.show()
        } else {
            super.onBackPressed()
        }
    }


    private fun doExternalBackUp() {
        try {
            if (hasPermissions() && PrefsUtil.getInstance(application).getValue(PrefsUtil.AUTO_BACKUP, false)) {
                val disposable = Observable.fromCallable {
                    PayloadUtil.getInstance(this@BalanceActivity).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(this@BalanceActivity).guid + AccessFactory.getInstance(this@BalanceActivity).pin))
                    true
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe({ t: Boolean? -> }) { throwable: Throwable? -> LogUtil.error(TAG, throwable) }
                registerDisposable(disposable)
            }
        } catch (exception: Exception) {
            LogUtil.error(TAG, exception)
        }
    }

    private fun updateDisplay(fromRefreshService: Boolean) {
        val txDisposable = loadTxes(account)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { txes: List<Tx>?, throwable: Throwable? ->
                throwable?.printStackTrace()
                if (txes != null) {
                    if (txes.isNotEmpty()) {
                        balanceViewModel.setTx(txes)
                    } else {
                        if (balanceViewModel.txs.value != null && balanceViewModel.txs.value!!.size == 0) {
                            balanceViewModel.setTx(txes)
                        }
                    }
                    Collections.sort(txes, TxMostRecentDateComparator())
                    txs!!.clear()
                    txs!!.addAll(txes)
                }
                if (binding.progressBar.visibility == View.VISIBLE && fromRefreshService) {
                    hideProgress()
                }
            }
        val balanceDisposable = loadBalance(account)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { balance: Long?, throwable: Throwable? ->
                throwable?.printStackTrace()
                if (balanceViewModel.balance.value != null) {
                    balanceViewModel.setBalance(balance)
                } else {
                    balanceViewModel.setBalance(balance)
                }
            }
        registerDisposable(balanceDisposable)
        registerDisposable(txDisposable)
        //        displayBalance();
//        txAdapter.notifyDataSetChanged();
    }

    private fun loadTxes(account: Int): Single<List<Tx>> {
        return Single.fromCallable {
            var loadedTxes: List<Tx> = ArrayList()
            if (account == 0) {
                loadedTxes = APIFactory.getInstance(this@BalanceActivity).allXpubTxs
            } else if (account == WhirlpoolMeta.getInstance(applicationContext).whirlpoolPostmix) {
                loadedTxes = APIFactory.getInstance(this@BalanceActivity).allPostMixTxs
            }
            loadedTxes
        }
    }

    private fun loadBalance(account: Int): Single<Long> {
        return Single.fromCallable {
            var loadedBalance = 0L
            if (account == 0) {
                loadedBalance = APIFactory.getInstance(this@BalanceActivity).xpubBalance
            } else if (account == WhirlpoolMeta.getInstance(applicationContext).whirlpoolPostmix) {
                loadedBalance = APIFactory.getInstance(this@BalanceActivity).xpubPostMixBalance
            }
            loadedBalance
        }
    }

    private fun doSettings() {
        TimeOutUtil.getInstance().updatePin()
        val intent = Intent(this@BalanceActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    var utxoListResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        run {
            refreshTx(false, false, false)
            showProgress()
        }
    }

    private fun doUTXO() {
        val intent = Intent(this@BalanceActivity, UTXOSActivity::class.java)
        intent.putExtra("_account", account)
        utxoListResult.launch(intent)
    }

    private fun doScan() {
        val cameraFragmentBottomSheet = ScanFragment()
        cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
        cameraFragmentBottomSheet.setOnScanListener { code ->
            cameraFragmentBottomSheet.dismissAllowingStateLoss()
            val params = SamouraiWallet.getInstance().currentNetworkParams
            val privKeyReader = PrivKeyReader(code, params)
            try {
                when {
                    canParseAsBatchSpend(code) -> {
                        launchBatchSpend(code)
                    }
                    privKeyReader.format != null -> {
                        doPrivKey(code.trim { it <= ' ' })
                    }
                    code.lowercase().startsWith(Auth47ViewModel.AUTH_SCHEME) -> {
                        ToolsBottomSheet.showTools(supportFragmentManager, ToolsBottomSheet.ToolType.AUTH47,
                            bundle = Bundle().apply {
                                putString("KEY", code)
                            })
                    }
                    Cahoots.isCahoots(code.trim { it <= ' ' }) -> {
                        val cahootIntent = ManualCahootsActivity.createIntentResume(this, account, code.trim { it <= ' ' })
                        startActivity(cahootIntent)
                    }
                    FormatsUtil.getInstance().isPSBT(code.trim { it <= ' ' }) -> {
                        ToolsBottomSheet.showTools(supportFragmentManager, ToolsBottomSheet.ToolType.PSBT,
                            bundle = Bundle().apply {
                                putString("KEY", code)
                            })
                    }
                    DojoUtil.getInstance(this@BalanceActivity).isValidPairingPayload(code.trim { it <= ' ' }) -> {
                        val intent = Intent(this@BalanceActivity, NetworkDashboard::class.java)
                        intent.putExtra("params", code.trim { it <= ' ' })
                        startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(this@BalanceActivity, AccountSelectionActivity::class.java)
                        intent.putExtra("uri", code.trim { it <= ' ' })
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun launchBatchSpend(inputBatchSpendAsJson: String) {
        val intent = Intent(this@BalanceActivity, BatchSpendActivity::class.java)
        intent.putExtra("inputBatchSpend", inputBatchSpendAsJson)
        startActivity(intent)
    }

    private fun doSweepViaScan() {
        val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
        cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
        cameraFragmentBottomSheet.setQrCodeScanListener { code: String ->
            cameraFragmentBottomSheet.dismissAllowingStateLoss()
            val params = SamouraiWallet.getInstance().currentNetworkParams
            val privKeyReader = PrivKeyReader(code, params)
            try {
                when {
                    privKeyReader.format != null -> {
                        doPrivKey(code.trim { it <= ' ' })
                    }
                    Cahoots.isCahoots(code.trim { it <= ' ' }) -> {
                        val cahootIntent = ManualCahootsActivity.createIntentResume(this, account, code.trim { it <= ' ' })
                        startActivity(cahootIntent)
                    }
                    FormatsUtil.getInstance().isPSBT(code.trim { it <= ' ' }) -> {
                        PSBTUtil.getInstance(this@BalanceActivity).doPSBT(code.trim { it <= ' ' })
                    }
                    DojoUtil.getInstance(this@BalanceActivity).isValidPairingPayload(code.trim { it <= ' ' }) -> {
                        Toast.makeText(this@BalanceActivity, "Samourai Dojo full node coming soon.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val intent = Intent(this@BalanceActivity, AccountSelectionActivity::class.java)
                        intent.putExtra("uri", code.trim { it <= ' ' })
                        intent.putExtra("_account", account)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun doPrivKey(data: String?) {

        val params = SamouraiWallet.getInstance().currentNetworkParams
        var privKeyReader: PrivKeyReader? = null
        var format: String? = null
        try {
            privKeyReader = PrivKeyReader(data, params)
            format = privKeyReader.format
        } catch (e: Exception) {
            Toast.makeText(this@BalanceActivity, e.message, Toast.LENGTH_SHORT).show()
            return
        }
        if (format != null) {
            ToolsBottomSheet.showTools(supportFragmentManager, ToolsBottomSheet.ToolType.SWEEP,
                bundle = Bundle().apply {
                    putString("KEY", data)
                })
        } else {
            Toast.makeText(this@BalanceActivity, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show()
        }
    }

    private fun doBackup(passphrase: String) {
        val export_methods = arrayOfNulls<String>(2)
        export_methods[0] = getString(R.string.export_to_clipboard)
        export_methods[1] = getString(R.string.export_to_email)
        MaterialAlertDialogBuilder(this@BalanceActivity)
            .setTitle(R.string.options_export)
            .setSingleChoiceItems(export_methods, 0, DialogInterface.OnClickListener { dialog, which ->
                try {
                    PayloadUtil.getInstance(this@BalanceActivity).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(this@BalanceActivity).guid + AccessFactory.getInstance(this@BalanceActivity).pin))
                } catch (ioe: IOException) {
                } catch (je: JSONException) {
                } catch (de: DecryptionException) {
                } catch (mle: MnemonicLengthException) {
                }
                var encrypted: String? = null
                try {
                    encrypted = AESUtil.encryptSHA256(PayloadUtil.getInstance(this@BalanceActivity).payload.toString(), CharSequenceX(passphrase))
                } catch (e: Exception) {
                    Toast.makeText(this@BalanceActivity, e.message, Toast.LENGTH_SHORT).show()
                } finally {
                    if (encrypted == null) {
                        Toast.makeText(this@BalanceActivity, R.string.encryption_error, Toast.LENGTH_SHORT).show()
                        return@OnClickListener
                    }
                }
                val obj = PayloadUtil.getInstance(this@BalanceActivity).putPayload(encrypted, true)
                if (which == 0) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    var clip: ClipData? = null
                    clip = ClipData.newPlainText("Wallet backup", obj.toString())
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@BalanceActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                } else {
                    val email = Intent(Intent.ACTION_SEND)
                    email.putExtra(Intent.EXTRA_SUBJECT, "Samourai Wallet backup")
                    email.putExtra(Intent.EXTRA_TEXT, obj.toString())
                    email.type = "message/rfc822"
                    startActivity(Intent.createChooser(email, getText(R.string.choose_email_client)))
                }
                dialog.dismiss()
            }
            ).show()
    }

    private fun doClipboardCheck() {
        val clipboard = this@BalanceActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            val item = clip!!.getItemAt(0)
            if (item.text != null) {
                val text = item.text.toString()
                val s = text.split("\\s+").toTypedArray()
                try {
                    for (i in s.indices) {
                        val params = SamouraiWallet.getInstance().currentNetworkParams
                        val privKeyReader = PrivKeyReader(s[i], params)
                        if (privKeyReader.format != null &&
                            (privKeyReader.format == PrivKeyReader.WIF_COMPRESSED || privKeyReader.format == PrivKeyReader.WIF_UNCOMPRESSED || privKeyReader.format == PrivKeyReader.BIP38 ||
                                    FormatsUtil.getInstance().isValidXprv(s[i]))
                        ) {
                            MaterialAlertDialogBuilder(this@BalanceActivity)
                                .setTitle(R.string.app_name)
                                .setMessage(R.string.privkey_clipboard)
                                .setCancelable(false)
                                .setPositiveButton(R.string.yes) { _, _ -> clipboard.setPrimaryClip(ClipData.newPlainText("", "")) }.setNegativeButton(R.string.no) { _, _ -> }.show()
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun refreshTx(notifTx: Boolean, dragged: Boolean, launch: Boolean) {
        if (AppUtil.getInstance(this@BalanceActivity).isOfflineMode) {
            Toast.makeText(this@BalanceActivity, R.string.in_offline_mode, Toast.LENGTH_SHORT).show()
            /*
            CoordinatorLayout coordinatorLayout = new CoordinatorLayout(BalanceActivity.this);
            Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.in_offline_mode, Snackbar.LENGTH_LONG);
            snackbar.show();
            */
        }
        WalletRefreshWorker.enqueue(applicationContext, launched = launch, notifTx = notifTx)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent);
//        } else {
//            startService(intent);
//        }
    }

    private fun doExplorerView(strHash: String?) {
        if (strHash != null) {
            val blockExplorer = BlockExplorerUtil.getInstance().getUri(true)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + strHash))
            startActivity(browserIntent)
        }
    }

    private fun txDetails(tx: Tx) {
        if (account == WhirlpoolMeta.getInstance(applicationContext).whirlpoolPostmix && tx.amount == 0.0) {
            return
        }
        val txIntent = Intent(this, TxDetailsActivity::class.java)
        txIntent.putExtra("TX", tx.toJSON().toString())
        txIntent.putExtra("_account", account)
        startActivity(txIntent)
    }

    private inner class RicochetQueueTask : AsyncTask<String?, Void?, String>() {

        override fun onPostExecute(result: String) {
        }

        override fun onPreExecute() {
        }

        override fun doInBackground(vararg params: String?): String {
            if (RicochetMeta.getInstance(this@BalanceActivity).queue.size > 0) {
                var count = 0
                val itr = RicochetMeta.getInstance(this@BalanceActivity).iterator
                while (itr.hasNext()) {
                    if (count == 3) {
                        break
                    }
                    try {
                        val jObj = itr.next()
                        val jHops = jObj.getJSONArray("hops")
                        if (jHops.length() > 0) {
                            val jHop = jHops.getJSONObject(jHops.length() - 1)
                            val txHash = jHop.getString("hash")
                            val txObj = APIFactory.getInstance(this@BalanceActivity).getTxInfo(txHash)
                            if (txObj != null && txObj.has("block_height") && txObj.getInt("block_height") != -1) {
                                itr.remove()
                                count++
                            }
                        }
                    } catch (je: JSONException) {
                    }
                }
            }
            if (RicochetMeta.getInstance(this@BalanceActivity).staggered.size > 0) {
                var count = 0
                val staggered = RicochetMeta.getInstance(this@BalanceActivity).staggered
                val _staggered: MutableList<JSONObject> = ArrayList()
                for (jObj in staggered) {
                    if (count == 3) {
                        break
                    }
                    try {
                        val jHops = jObj.getJSONArray("script")
                        if (jHops.length() > 0) {
                            val jHop = jHops.getJSONObject(jHops.length() - 1)
                            val txHash = jHop.getString("tx")
                            val txObj = APIFactory.getInstance(this@BalanceActivity).getTxInfo(txHash)
                            if (txObj != null && txObj.has("block_height") && txObj.getInt("block_height") != -1) {
                                count++
                            } else {
                                _staggered.add(jObj)
                            }
                        }
                    } catch (je: JSONException) {
                    } catch (cme: ConcurrentModificationException) {
                    }
                }
            }
            return "OK"
        }
    }

    private fun doFeaturePayNymUpdate() {
        val disposable = Observable.fromCallable {
            var obj = JSONObject()
            obj.put("code", BIP47Util.getInstance(this@BalanceActivity).paymentCode.toString())
            //                    Log.d("BalanceActivity", obj.toString());
            var res = WebUtil.getInstance(this@BalanceActivity).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/token", obj.toString())
            //                    Log.d("BalanceActivity", res);
            var responseObj = JSONObject(res)
            if (responseObj.has("token")) {
                val token = responseObj.getString("token")
                val sig = MessageSignUtil.getInstance(this@BalanceActivity).signMessage(BIP47Util.getInstance(this@BalanceActivity).notificationAddress.ecKey, token)
                //                        Log.d("BalanceActivity", sig);
                obj = JSONObject()
                obj.put("nym", BIP47Util.getInstance(this@BalanceActivity).paymentCode.toString())
                obj.put("code", BIP47Util.getInstance(this@BalanceActivity).featurePaymentCode.toString())
                obj.put("signature", sig)

//                        Log.d("BalanceActivity", "nym/add:" + obj.toString());
                res = WebUtil.getInstance(this@BalanceActivity).postURL("application/json", token, WebUtil.PAYNYM_API + "api/v1/nym/add", obj.toString())
                //                        Log.d("BalanceActivity", res);
                responseObj = JSONObject(res)
                if (responseObj.has("segwit") && responseObj.has("token")) {
                    PrefsUtil.getInstance(this@BalanceActivity).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true)
                } else if (responseObj.has("claimed") && responseObj.getBoolean("claimed") == true) {
                    PrefsUtil.getInstance(this@BalanceActivity).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, true)
                }
            }
            true
        }.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ aBoolean: Boolean? -> Log.i(TAG, "doFeaturePayNymUpdate: Feature update complete") }) { error: Throwable? -> Log.i(TAG, "doFeaturePayNymUpdate: Feature update Fail") }
        registerDisposable(disposable)
    }

    companion object {
        private const val SCAN_COLD_STORAGE = 2011
        private const val SCAN_QR = 2012
        private const val UTXO_REQUESTCODE = 2012
        private const val TAG = "BalanceActivity"
        const val ACTION_INTENT = "com.samourai.wallet.BalanceFragment.REFRESH"
        const val DISPLAY_INTENT = "com.samourai.wallet.BalanceFragment.DISPLAY"
    }
}