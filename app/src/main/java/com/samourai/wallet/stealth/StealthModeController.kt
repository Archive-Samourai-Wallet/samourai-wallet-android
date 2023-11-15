package com.samourai.wallet.stealth

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.samourai.wallet.MainActivity2
import com.samourai.wallet.R
import com.samourai.wallet.stealth.calculator.CalculatorActivity
import com.samourai.wallet.stealth.notepad.NotepadActivity
import com.samourai.wallet.stealth.qrscannerapp.QRStealthAppActivity
import com.samourai.wallet.stealth.vpn.VPNActivity
import com.samourai.wallet.tor.SamouraiTorManager
import com.samourai.wallet.util.tech.AppUtil
import com.samourai.wallet.util.TimeOutUtil


object StealthModeController {

    private const val PREF_APP = "stl_app"
    const val PREF_ENABLED = "stl_enabled"

    enum class StealthApp(packageId: String, @DrawableRes icon: Int, @StringRes name: Int) {

        //DEFAULT
        SAMOURAI(MainActivity2::class.qualifiedName.toString(), R.mipmap.ic_launcher, R.string.app_name),

        CALCULATOR(CalculatorActivity::class.qualifiedName.toString(), R.drawable.ic_stealth_calculator, R.string.calculator),
        VPN(VPNActivity::class.qualifiedName.toString(), R.drawable.stealth_vpn_icon, R.string.stealth_vpn_name),
        QRAPP(QRStealthAppActivity::class.qualifiedName.toString(), R.drawable.stealth_qr_app_icon, R.string.stealth_qr_scannerapp_title),
        NOTEPAD(NotepadActivity::class.qualifiedName.toString(), R.drawable.stealth_notepad_icon, R.string.stealth_notepad_title);

        private var appKey: String = packageId
        private var appIcon: Int = icon
        private var appName: Int = name
        private var componentName: ComponentName? = null

        fun getAppKey(): String {
            return appKey
        }

        fun getAppName(): Int {
            return appName
        }

        fun getIcon(): Int = appIcon

        open fun getComponentName(ctx: Context): ComponentName? {
            if (componentName == null) {
                componentName = ComponentName(ctx.packageName, appKey)
            }
            return componentName
        }
    }

    fun toggleStealthState(context: Context): Boolean {
        val prefs = getStealthPreferences(context)
        val enabled = prefs?.getBoolean(PREF_ENABLED, false)
        prefs?.edit()?.putBoolean(PREF_ENABLED, enabled != true)?.commit()
        return enabled != true;
    }

    fun enableStealth(context: Context) {
        if (SamouraiTorManager.isConnected() || SamouraiTorManager.isStarting()) {
            SamouraiTorManager.stop()
        }
        val prefs = getStealthPreferences(context)
        val key = prefs?.getString(PREF_APP, StealthApp.CALCULATOR.getAppKey()) ?: StealthApp.CALCULATOR.getAppKey()
        StealthApp.values().forEach {
            if (it.getAppKey() == key) {
                enableStealth(it, context)
                Toast.makeText(context, "Stealth mode enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun enableStealth(stealthApp: StealthApp, context: Context) {
        val pm = context.packageManager
        TimeOutUtil.getInstance().reset()
        repeat(StealthApp.values().count()) {
            val app = StealthApp.values()[it]
            val component = app.getComponentName(context)
            if (component != null) {
                pm.setComponentEnabledSetting(
                    component,
                    if (app == stealthApp) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    else
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        if (stealthApp == StealthApp.SAMOURAI) {
            Handler(Looper.getMainLooper()).postDelayed({
                AppUtil.getInstance(context).restartApp()
            }, 1700)
        }

    }

    private fun isAppEnabled(stealthApp: StealthApp, context: Context): Boolean {
        if (stealthApp.getComponentName(context) == null) {
            return false
        }
        val settings = context.packageManager.getComponentEnabledSetting(stealthApp.getComponentName(context)!!)
        return settings == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || settings == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    fun fixStealthModeIfNotActive(context: Context) {
        for (app in StealthApp.values()) {
            if (isAppEnabled(app, context)) {
                return
            }
        }
        enableStealth(StealthApp.SAMOURAI, context)
    }

    private fun getStealthPreferences(context: Context): SharedPreferences? {
        return context.getSharedPreferences(
            "${context.packageName}_stealth_prefs",
            Context.MODE_PRIVATE
        )
    }

    fun isStealthEnabled(context: Context): Boolean {
        val prefs = getStealthPreferences(context)
        val enabled = prefs?.getBoolean(PREF_ENABLED, false)
        return enabled == true
    }
    fun isStealthActive(context: Context): Boolean {
        return  !isAppEnabled(StealthApp.SAMOURAI, context);
    }

    fun getSelectedApp(context: Context): String {
        val prefs = getStealthPreferences(context)
        return prefs?.getString(PREF_APP, null) ?: StealthApp.CALCULATOR.getAppKey()
    }

    fun setSelectedApp(appKey: String, context: Context) {
        val prefs = getStealthPreferences(context)
        prefs?.edit()?.putString(PREF_APP, appKey)?.apply()
    }
}