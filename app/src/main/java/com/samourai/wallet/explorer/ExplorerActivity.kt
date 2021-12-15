package com.samourai.wallet.explorer

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
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
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.tor.TorManager
import com.samourai.wallet.util.BlockExplorerUtil
import com.samourai.wallet.util.LogUtil
import kotlinx.android.synthetic.main.activity_explorer.*
import kotlinx.coroutines.*
import android.webkit.WebView
import com.samourai.wallet.BuildConfig


class ExplorerActivity : AppCompatActivity() {

    var txId: String = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(TX_URI)) {
            txId = intent.extras?.getString(TX_URI, "")!!
        } else {
            finish()
            return
        }

        supportActionBar?.title = "Explorer"
        webView.setBackgroundColor(0)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
            swipeRefreshLayout.isRefreshing = false;
        }
        TorManager.getTorStateLiveData().observe(this, {
            if (it == TorManager.TorState.ON) {
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(800)
                        (Dispatchers.Main){
                           setProxy()
                        }
                    }
            }
        })

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (!TorManager.isConnected()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm)
                    .setMessage("Tor is not enabled, built in web browser supports tor proxy")
                    .setPositiveButton("Enable tor") { dialog, _ ->
                        dialog.dismiss()
                        TorManager.startTor()
                    }
                    .setNegativeButton("Load") { dialog, _ ->
                        dialog.dismiss()
                        load()
                    }.show()
            }

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
        val url = "$blockExplorer${txId}"
        webView.loadUrl(url)
    }

    private fun setProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("SOCKS:/${TorManager.getProxy()?.address().toString()}")
                .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, {
                load()
             }, {
            })
        }
    }

    override fun onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack()
        }else{
            super.onBackPressed()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.menu_web_copy_tx -> {
               copyText(this.txId)
            }
            R.id.menu_web_tor -> {
                val torEnabled = TorManager.torState == TorManager.TorState.ON
                MaterialAlertDialogBuilder(this)
                    .setTitle("Tor status : ${if(torEnabled) " Enabled" else " Disabled"}")
                    .setPositiveButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            R.id.menu_web_explorer -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.open_in_browser)
                    .setMessage("View link in external browser?")
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView.url)))
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
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
        TorManager.getTorStateLiveData().observe(this, {
            menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_on)
            val icon = menu.findItem(R.id.menu_web_tor).icon
            when (it) {
                TorManager.TorState.WAITING -> {
                    icon.setTint(ContextCompat.getColor(applicationContext, R.color.warning_yellow))
                }
                TorManager.TorState.ON -> {
                    icon.setTint(ContextCompat.getColor(applicationContext, R.color.green_ui_2))
                }
                TorManager.TorState.OFF -> {
                    menu.findItem(R.id.menu_web_tor).icon = ContextCompat.getDrawable(applicationContext, R.drawable.ic_tor_on)
                    menu.findItem(R.id.menu_web_tor).icon.setTint(ContextCompat.getColor(applicationContext,R.color.disabledRed))
                }
                else -> {
                }
            }
        })
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
        webView.stopLoading()
        webView.clearFormData()
        webView.clearHistory()
        webView.clearCache(false)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val TX_URI = "tx_uri";
        private const val TAG = "ExplorerActivity"
    }
}