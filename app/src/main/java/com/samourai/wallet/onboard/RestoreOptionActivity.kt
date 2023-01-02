package com.samourai.wallet.onboard

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.RestoreSeedWalletActivity
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.databinding.ActivityRestoreOptionBinding
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.payload.ExternalBackupManager
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.CharSequenceX
import com.samourai.wallet.util.PrefsUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*


class RestoreOptionActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var binding: ActivityRestoreOptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestoreOptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.window)
        setSupportActionBar(binding.restoreOptionToolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        binding.samouraiMnemonicRestore.setOnClickListener {
            val intent = Intent(this@RestoreOptionActivity, RestoreSeedWalletActivity::class.java)
            intent.putExtra("mode", "mnemonic")
            intent.putExtra("type", "samourai")
            startActivity(intent)
        }
        binding.samouraiBackupFileRestore.setOnClickListener {
            val intent = Intent(this@RestoreOptionActivity, RestoreSeedWalletActivity::class.java)
            intent.putExtra("mode", "backup")
            startActivity(intent)
        }
        binding.externalWalletRestore.setOnClickListener {
            val intent = Intent(this@RestoreOptionActivity, RestoreSeedWalletActivity::class.java)
            intent.putExtra("mode", "mnemonic")
            startActivity(intent)
        }

        binding.restoreBtn.setOnClickListener { restoreWalletFromBackup() }

        ExternalBackupManager.getPermissionStateLiveData().observe(this, Observer {
            if (ExternalBackupManager.backupAvailable()) {
                binding.restoreFromBackupSnackbar.visibility = View.VISIBLE
            } else {
                binding.restoreFromBackupSnackbar.visibility = View.GONE
            }
        })

        if (ExternalBackupManager.backupAvailable()) {
            binding.restoreFromBackupSnackbar.visibility = View.VISIBLE
        } else {
            binding.restoreFromBackupSnackbar.visibility = View.GONE
        }

        binding.restoreOptionToolBar.setNavigationOnClickListener {
            this.onBackPressed()
        }

    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun showLoading(show: Boolean) {
        val  loaderRestore = binding.loaderRestore
        val  restoreBtn = binding.restoreBtn
        if (show) {
            loaderRestore.visibility = View.VISIBLE
            restoreBtn.visibility = View.GONE
        } else {
            loaderRestore.visibility = View.GONE
            restoreBtn.visibility = View.VISIBLE
        }
    }

    private fun restoreWalletFromBackup() {

        fun initializeRestore(decrypted: String, skipDojo: Boolean) {
            showLoading(true)
            scope.launch(Dispatchers.IO) {
                val json = JSONObject(decrypted)
                PayloadUtil.getInstance(applicationContext).restoreWalletfromJSON(json, skipDojo)
                val guid = AccessFactory.getInstance(applicationContext).createGUID()
                val hash = AccessFactory.getInstance(applicationContext).getHash(
                    guid,
                    CharSequenceX(AccessFactory.getInstance(applicationContext).pin),
                    AESUtil.DefaultPBKDF2Iterations
                )
                PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.ACCESS_HASH, hash)
                PrefsUtil.getInstance(applicationContext).setValue(PrefsUtil.ACCESS_HASH2, hash)
                PayloadUtil.getInstance(applicationContext)
                    .saveWalletToJSON(CharSequenceX(guid + AccessFactory.getInstance().pin))
            }.invokeOnCompletion {
                it?.printStackTrace()
                scope.launch(Dispatchers.Main) {
                    showLoading(false)
                }
                if (it != null) {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(this@RestoreOptionActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    AppUtil.getInstance(this@RestoreOptionActivity).restartApp()
                }
            }
        }

        fun checkRestoreOptions(decrypted: String) {
            val json = JSONObject(decrypted)
            var existDojo = false
            if (json.has("meta") && json.getJSONObject("meta").has("dojo")) {
                if (json.getJSONObject("meta").getJSONObject("dojo").has("pairing")) {
                    existDojo = true
                }
            }

            if (existDojo && DojoUtil.getInstance(application).dojoParams != null)
                initializeRestore(decrypted, true)
            else
                initializeRestore(decrypted, false)

        }

        suspend fun readBackUp(password: String) = withContext(Dispatchers.IO) {
            try {
                val backupData = ExternalBackupManager.read()
                if (backupData != null) {
                    val decrypted = PayloadUtil.getInstance(applicationContext).getDecryptedBackupPayload(backupData, CharSequenceX(password))
                    if (decrypted != null && decrypted.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            checkRestoreOptions(decrypted)
                        }
                    }
                    return@withContext
                }
                return@withContext
            } catch (e: Exception) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@RestoreOptionActivity, R.string.decryption_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.restore_backup)
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.password_input_dialog_layout, null)
        view.findViewById<TextView>(R.id.dialogMessage).setText(R.string.enter_your_wallet_passphrase)
        val password = view.findViewById<EditText>(R.id.restore_dialog_password_edittext)
        builder.setView(view)
        builder.setPositiveButton(R.string.restore) { dialog, which ->
            dialog.dismiss()
            scope.launch {
                readBackUp(password.text.toString())
            }.invokeOnCompletion {

            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.get_help_menu_create -> {
                doSupportCreate()
                false
            }
            R.id.get_help_menu_restore -> {
                doSupportRestore()
                false
            }
            else -> {
                false
            }
        }

    }


    private fun doSupportCreate() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.samourai.io/wallet/start#create-new-wallet"))
        startActivity(intent)
    }

    private fun doSupportRestore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.samourai.io/wallet/restore-recovery"))
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.landing_activity_menu, menu)
        return true
    }
}
