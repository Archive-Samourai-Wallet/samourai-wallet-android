package com.samourai.wallet.explorer

import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.R
import com.samourai.wallet.databinding.ActivityExplorerBinding
import com.samourai.wallet.tor.EnumTorState
import com.samourai.wallet.tor.SamouraiTorManager
import com.samourai.wallet.util.BlockExplorerUtil
import kotlinx.coroutines.*


class ExplorerActivity : AppCompatActivity() {

    var txId: String = "";
    var supportURL: String = "";
   private lateinit var  binding : ActivityExplorerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (intent.hasExtra(TX_URI)) {
            txId = intent.extras?.getString(TX_URI, "")!!
        } else if (intent.hasExtra(SUPPORT)) {
            supportURL = intent.extras?.getString(SUPPORT, "")!!
        } else {
            finish()
            return
        }

        supportActionBar?.title = "Explorer"
        if (supportURL != "")
            supportActionBar?.title = "Support"

        binding.webView.setBackgroundColor(0)

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
            binding.swipeRefreshLayout.isRefreshing = false;
        }
        SamouraiTorManager.getTorStateLiveData().observe(this) {
            if (it.state == EnumTorState.ON) {
                CoroutineScope(Dispatchers.Default).launch {
                    delay(800)
                    (Dispatchers.Main){
                        setProxy()
                    }
                }
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            load()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm)
                .setMessage("Your android does not support proxy enabled WebView")
                .setPositiveButton("Continue without Tor") { dialog, _ ->
                    dialog.dismiss()
                    load()
                }
                .setNegativeButton("Exit") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }.show()
        }

    }



    @SuppressLint("SetJavaScriptEnabled")
    private fun load() {
        val progressWeb = binding.progressWeb
        val webView = binding.webView
        progressWeb.progress = 8
        progressWeb.isIndeterminate = false
        CookieManager.getInstance().setAcceptCookie(false)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressWeb.visibility = View.VISIBLE
                progressWeb.isIndeterminate = false
                progressWeb.progress = newProgress
                if(newProgress == 100){
                    progressWeb.visibility = View.INVISIBLE
                }
                super.onProgressChanged(view, newProgress)
            }

            override fun onJsBeforeUnload(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                return super.onJsBeforeUnload(view, url, message, result)
            }
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.i(TAG, "onConsoleMessage: Console  messages ${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }


        webView.settings.javaScriptEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setGeolocationEnabled(false)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.settings.safeBrowsingEnabled  = true
        }

        val blockExplorer = BlockExplorerUtil.getInstance().getUri(true)
        var url = "$blockExplorer${txId}"
        if (supportURL != "") {
            url = supportURL
        }
        webView.loadUrl(url)
    }

    private fun setProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("SOCKS:/${SamouraiTorManager.getProxy()?.address().toString()}")
                .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, {
                load()
             }, {
            })
        }
    }

    override fun onBackPressed() {
        val webView = binding.webView
        if(webView.canGoBack()){
            webView.goBack()
        }else{
            super.onBackPressed()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val webView = binding.webView
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.menu_web_copy_tx -> {
               copyText(this.txId)
            }
            R.id.menu_web_tor -> {
                val torStatus = when( SamouraiTorManager.getTorState().state) {
                    EnumTorState.STARTING ->  "Waiting"
                    EnumTorState.ON -> "Enabled"
                    EnumTorState.OFF -> "Disabled"
                    EnumTorState.STOPPING -> "Stopping"
                }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Tor status : $torStatus")
                    .setPositiveButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            R.id.menu_web_clear_cache -> {
                webView.clearCache(true)
                Toast.makeText(webView.context,"Cache cleared",Toast.LENGTH_SHORT).show()
            }
            R.id.menu_web_copy_url -> {
                webView.url?.let { copyText(it) }
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_explorer, menu)
        SamouraiTorManager.getTorStateLiveData().observe(this) {
            menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_on)
            val icon = menu.findItem(R.id.menu_web_tor).icon
            when (it.state) {
                EnumTorState.STARTING -> {
                    icon?.setTint(ContextCompat.getColor(applicationContext, R.color.warning_yellow))
                }
                EnumTorState.STOPPING -> {
                    icon?.setTint(ContextCompat.getColor(applicationContext, R.color.warning_yellow))
                }
                EnumTorState.ON -> {
                    icon?.setTint(ContextCompat.getColor(applicationContext, R.color.green_ui_2))
                }
                EnumTorState.OFF -> {
                    menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_on)
                    menu.findItem(R.id.menu_web_tor).icon?.setTint(ContextCompat.getColor(applicationContext, R.color.disabledRed))
                }
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun copyText(string: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clipData = ClipData
            .newPlainText("", string)
        if (cm != null) {
            cm.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        val webView = binding.webView
        webView.stopLoading()
        webView.clearFormData()
        webView.clearHistory()
        webView.clearCache(false)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val TX_URI = "tx_uri";
        const val SUPPORT = "support_extra";
        private const val TAG = "ExplorerActivity"
    }
}