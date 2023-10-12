package com.samourai.wallet.tor

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.samourai.wallet.util.PrefsUtil
import io.matthewnelson.kmp.tor.sample.kotlin.android.TorKmpManager
import org.json.JSONException
import org.json.JSONObject
import java.net.Proxy

object SamouraiTorManager {

    private const val TAG = "SamouraiTorManager"

    private var torKmpManager: TorKmpManager? = null
        get() = field

    private var appContext: Application? = null

    fun setUp(application: Application) {
        appContext = application
        torKmpManager = TorKmpManager(application)
    }

    fun getTorStateLiveData(): MutableLiveData<TorState> {
        return torKmpManager!!.torStateLiveData
    }

    fun getTorState(): TorState {
        return torKmpManager!!.torState
    }

    fun isRequired(): Boolean {
        return PrefsUtil.getInstance(appContext).getValue(PrefsUtil.ENABLE_TOR, false);
    }

    fun isConnected(): Boolean {
        return torKmpManager?.isConnected() ?: false
    }

    fun isStarting(): Boolean {
        return torKmpManager?.isStarting() ?: false
    }

    fun stop() {
        torKmpManager?.torOperationManager?.stopQuietly();
    }

    fun start() {
        torKmpManager?.torOperationManager?.startQuietly();
    }

    fun getProxy(): Proxy? {
        return torKmpManager?.proxy;
    }

    @JvmStatic
    fun newIdentity() {
        torKmpManager?.newIdentity(appContext!!);
    }

    fun toJSON(): JSONObject {

        val jsonPayload = JSONObject();

        try {
            jsonPayload.put("active", PrefsUtil.getInstance(appContext).getValue(PrefsUtil.ENABLE_TOR,false));
        } catch (ex: JSONException) {
            Log.d(TAG, "JSONException issue on toJSON:" + ex.message)
        } catch (ex: ClassCastException) {
            Log.d(TAG, "ClassCastException issue on toJSON:" + ex.message)
        }

        return jsonPayload
    }

    fun fromJSON(jsonPayload: JSONObject) {
        try {
            if (jsonPayload.has("active")) {
                PrefsUtil.getInstance(appContext).setValue(PrefsUtil.ENABLE_TOR, jsonPayload.getBoolean("active"));
            }
        } catch (ex: JSONException) {
            Log.d(TAG, "JSONException issue on fromJSON:" + ex.message)
        }
    }
}