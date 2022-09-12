package com.samourai.wallet.stealth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.TileService

/**
 * Stealth Tile service for quick action tile
 */
@SuppressLint("NewApi")
class StealthTileService : TileService() {

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
    override fun onClick() {
        if (StealthModeController.isStealthEnabled(applicationContext)) {
            StealthModeController.enableStealthFromPrefs(applicationContext)
        }
        super.onClick()
    }
}