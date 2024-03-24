package com.samourai.wallet.whirlpool

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.cahoots.Cahoots
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.constants.SamouraiAccount
import com.samourai.wallet.databinding.ActivityWhirlpoolHomeBinding
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.network.NetworkDashboard
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.SendActivity
import com.samourai.wallet.send.SendActivity.isPSBT
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.service.WalletRefreshWorker
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.wallet.utxos.PreSelectUtil
import com.samourai.wallet.utxos.UTXOSActivity
import com.samourai.wallet.whirlpool.fragments.SectionsPagerAdapter
import com.samourai.wallet.whirlpool.fragments.WhirlPoolLoaderDialog
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity
import com.samourai.wallet.whirlpool.newPool.WhirlpoolDialog
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhirlpoolHome : SamouraiActivity() {

    private lateinit var binding: ActivityWhirlpoolHomeBinding
    private val whirlPoolHomeViewModel by viewModels<WhirlPoolHomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhirlpoolHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.loading)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
        val whirlPoolLoaderDialog = WhirlPoolLoaderDialog()
        whirlPoolLoaderDialog.setOnInitComplete {
            initPager()
        }
        val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet()
        if (wallet == null) {
            whirlPoolLoaderDialog.show(supportFragmentManager, whirlPoolLoaderDialog.tag)
        } else {
            if (!wallet.isStarted) {
                whirlPoolLoaderDialog.show(supportFragmentManager, whirlPoolLoaderDialog.tag)
            } else {
                initPager()
            }
        }
        binding.fab.setOnClickListener {
            val whirlpoolDialog = WhirlpoolDialog()
            whirlpoolDialog.show(supportFragmentManager, whirlpoolDialog.tag)
        }
        val displaySats = PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.IS_SAT,false)
        whirlPoolHomeViewModel.toggleSats(displaySats)
        binding.viewPager.addOnPageChangeListener(pageListener)
        val filterDisplay = IntentFilter(BalanceActivity.DISPLAY_INTENT)
        LocalBroadcastManager.getInstance(this@WhirlpoolHome)
            .registerReceiver(receiver, filterDisplay)
        AppUtil.getInstance(applicationContext).walletLoading.observe(this) {
            whirlPoolHomeViewModel.setRefresh(it)
            if(!it){
                whirlPoolHomeViewModel.loadTransactions(applicationContext)
            }
        }
        if(intent.getBooleanExtra("refresh",false)){
           whirlPoolHomeViewModel.viewModelScope.launch {
              withContext(Dispatchers.Default){
                  delay(800)
                  WalletRefreshWorker.enqueue(applicationContext, notifTx = false, launched = false);
              }
           }
        }
    }

    //Checks if there is any previous postmix or premix activities
    private fun checkOnboardStatus() {
        whirlPoolHomeViewModel.viewModelScope
            .launch(Dispatchers.Default) {
                val postmix = APIFactory.getInstance(applicationContext).allPostMixTxs.size == 0
                val premix = APIFactory.getInstance(applicationContext).premixXpubTxs.isEmpty()
                var hasMixUtxos = false;
                if (AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.isPresent) {
                    hasMixUtxos =
                        AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.get().utxoSupplier.findUtxos(
                            SamouraiAccount.PREMIX,
                            SamouraiAccount.POSTMIX
                        ).isEmpty()
                }
                withContext(Dispatchers.Main) {
                    if (whirlPoolHomeViewModel.onboardStatus.value != postmix && premix && hasMixUtxos) {
                        whirlPoolHomeViewModel.setOnBoardingStatus(postmix && premix && hasMixUtxos)
                    }
                }
            }
    }

    private fun initPager() {
        val mixingStr = resources.getString(R.string.mixing_title);
        val remixingStr = resources.getString(R.string.remixing);
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
        whirlPoolHomeViewModel.remixBalance.observe(this, {
            if (viewPager.currentItem == 3) {
                setBalance(3)
            }
        })

        whirlPoolHomeViewModel.totalBalance.observe(this, {
            if (viewPager.currentItem == 0) {
                setBalance(0)
            }
        })

        whirlPoolHomeViewModel.mixingBalance.observe(this, {
            if (viewPager.currentItem == 1) {
                setBalance(1)
            }
        })
        //Show mixing/remixing count in tabs title
        whirlPoolHomeViewModel.mixingLive.observe(this, {
            if (it.isNotEmpty()) {
                tabs.getTabAt(1)?.text = "$mixingStr (${it.size})"
            } else {
                tabs.getTabAt(1)?.text = mixingStr
            }
        })
        whirlPoolHomeViewModel.remixLive.observe(this, {
            if (it.isNotEmpty()) {
                tabs.getTabAt(2)?.text = "$remixingStr (${it.size})"
            } else {
                tabs.getTabAt(2)?.text = remixingStr
            }
        })
        binding.toolbar.setOnClickListener {
            whirlPoolHomeViewModel.toggleSats()
            PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.IS_SAT,whirlPoolHomeViewModel.displaySatsLive.value ?:false)
        }
        binding.whirlpoolToolbar.setOnClickListener {
            whirlPoolHomeViewModel.toggleSats()
            PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.IS_SAT,whirlPoolHomeViewModel.displaySatsLive.value ?:false)
        }
        try {
            validateIntentAndStartNewPool()
        } catch (ex: Exception) {

        }
        whirlPoolHomeViewModel.displaySatsLive.observe(this, Observer {
            setBalance(binding.viewPager.currentItem)
        })
        checkOnboardStatus()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkOnboardStatus()
        }
    }

    private val pageListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            setBalance(position)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    fun setBalance(position: Int) {
        var balance = 0L;
        when (position) {
            0 -> {
                binding.whirlpoolAmountCaption.text = getString(R.string.total_balance)
                whirlPoolHomeViewModel.totalBalance.value?.let {
                    balance = it
                }
            }
            1 -> {
                binding.whirlpoolAmountCaption.text = getString(R.string.total_in_progress_balance)
                whirlPoolHomeViewModel.mixingBalance.value?.let {
                    balance = it
                }
            }
            2 -> {
                binding.whirlpoolAmountCaption.text = getString(R.string.total_remixable_balance)
                whirlPoolHomeViewModel.remixBalance.value?.let {
                    balance = it
                }
            }
        }
        val showSats = whirlPoolHomeViewModel.displaySatsLive.value ?: false;
        val balanceString =
            if (showSats) FormatsUtil.formatSats(balance) else FormatsUtil.formatBTC(balance)
        binding.whirlpoolToolbar.title = balanceString
        binding.toolbar.title = balanceString
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEWPOOL_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.isPresent) {
                initPager()
                checkOnboardStatus()
                WalletRefreshWorker.enqueue(applicationContext, launched = false, notifTx = false)
            }
        }
    }


    private fun validateIntentAndStartNewPool() {
        if (intent.extras != null && intent.extras!!.containsKey("preselected")) {
            val intent = Intent(applicationContext, NewPoolActivity::class.java)
            val account = getIntent().extras!!.getInt("_account")
            intent.putExtra("_account", getIntent().extras!!.getInt("_account"))
            intent.putExtra("preselected", getIntent().extras!!.getString("preselected"))
            if (account == WhirlpoolMeta.getInstance(application).whirlpoolPostmix) {
                val coins = PreSelectUtil.getInstance().getPreSelected(
                    getIntent().extras!!.getString("preselected")
                )
                val mediumFee = FeeUtil.getInstance().normalFee.defaultPerKB.toLong() / 1000L
                val tx0 = WhirlpoolTx0(
                    WhirlpoolMeta.getMinimumPoolDenomination(),
                    mediumFee,
                    1,
                    coins
                )
                try {
                    tx0.make()
                } catch (ex: Exception) {
                    Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                    ex.printStackTrace()
                    super.onBackPressed()
                    return
                }
                if (tx0.tx0 == null) {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setMessage(getString(R.string.tx0_is_not_possible_with))
                        .setCancelable(true)
                    builder.setPositiveButton(R.string.ok) { dialogInterface, i -> dialogInterface.dismiss() }
                    builder.create().show()
                } else {
                    startActivityForResult(intent, NEWPOOL_REQ_CODE)
                }
            } else {
                startActivityForResult(intent, NEWPOOL_REQ_CODE)
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this@WhirlpoolHome)
            .unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.whirlpool_main, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) finish()
        if (id == R.id.action_utxo) {
            val intent = Intent(this@WhirlpoolHome, UTXOSActivity::class.java)
            intent.putExtra(
                "_account",
                WhirlpoolMeta.getInstance(this@WhirlpoolHome).whirlpoolPostmix
            )
            startActivity(intent)
        } else if (id == R.id.action_scode) {
            doSCODE()
        } else if (id == R.id.action_scan_qr) {
            val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
            cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
            cameraFragmentBottomSheet.setQrCodeScanListener { code: String ->
                cameraFragmentBottomSheet.dismissAllowingStateLoss()
                try {
                    if (Cahoots.isCahoots(code.trim { it <= ' ' })) {
                        val cahootIntent = ManualCahootsActivity.createIntentResume(
                            this,
                            WhirlpoolMeta.getInstance(application).whirlpoolPostmix,
                            code.trim { it <= ' ' })
                        startActivity(cahootIntent)
                    } else if (isPSBT(code.trim { it <= ' ' })) {
                        PSBTUtil.getInstance(application).doPSBT(code.trim { it <= ' ' })
                    } else if (DojoUtil.getInstance(application)
                            .isValidPairingPayload(code.trim { it <= ' ' })
                    ) {
                        val intent = Intent(application, NetworkDashboard::class.java)
                        intent.putExtra("params", code.trim { it <= ' ' })
                        startActivity(intent)
                    } else {
                        val intent = Intent(application, SendActivity::class.java)
                        intent.putExtra("uri", code.trim { it <= ' ' })
                        intent.putExtra(
                            "_account",
                            WhirlpoolMeta.getInstance(application).whirlpoolPostmix
                        )
                        startActivity(intent)
                    }
                } catch (e: java.lang.Exception) {
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doSCODE() {
        val scode = EditText(this@WhirlpoolHome)

        val strCurrentCode = WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode
        if (strCurrentCode != null && strCurrentCode.length > 0) {
            scode!!.setText(strCurrentCode)
        }

        val layout = LinearLayout(this@WhirlpoolHome)
        scode.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layout.addView(scode)
        MaterialAlertDialogBuilder(this@WhirlpoolHome)
            .setTitle(R.string.app_name)
            .setMessage(R.string.enter_scode)
            .setView(layout)
            .setNeutralButton("Remove SCODE") { dialog: DialogInterface?, which: Int ->
                WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode = ""
                WhirlpoolNotificationService.stopService(applicationContext)
                saveState()
                whirlPoolHomeViewModel.viewModelScope.launch(Dispatchers.Default) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        val _intent = Intent(this@WhirlpoolHome, BalanceActivity::class.java)
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(_intent)
                    }
                }
            }
            .setPositiveButton(R.string.ok) { dialog, whichButton ->

                val strSCODE = scode.text.toString().trim { it <= ' ' }
                if (strSCODE.isNotBlank()) {
                    WhirlpoolMeta.getInstance(this@WhirlpoolHome).scode = strSCODE
                    WhirlpoolNotificationService.stopService(applicationContext)
                    saveState()
                    whirlPoolHomeViewModel.viewModelScope.launch(Dispatchers.Default) {
                        delay(1000)
                        withContext(Dispatchers.Main) {
                            val _intent = Intent(this@WhirlpoolHome, BalanceActivity::class.java)
                            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivity(_intent)
                        }
                    }
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.show()

    }


    companion object {
        const val NEWPOOL_REQ_CODE = 6102
        private const val TAG = "WhirlpoolHome"
    }

}
