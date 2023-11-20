package com.samourai.wallet.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.samourai.wallet.util.tech.AppUtil

class ConnectionChangeReceiver(context: Context) {

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            AppUtil.getInstance(context).checkOfflineState()
        }
         // lost network connection
        override fun onLost(network: Network) {
            AppUtil.getInstance(context).checkOfflineState()
            super.onLost(network)
        }
    }

}
