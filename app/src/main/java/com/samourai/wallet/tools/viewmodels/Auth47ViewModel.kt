package com.samourai.wallet.tools.viewmodels

import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.util.tech.AppUtil
import com.samourai.wallet.util.func.MessageSignUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI


const val STATE_CHALLENGE_VALID = "challenge valid"
const val STATE_CHALLENGE_NOT_VALID = "challenge not valid"

class Auth47ViewModel : ViewModel() {

    companion object {
        const val AUTH_SCHEME = "auth47"
        const val AUTH_CALLBACK_HTTPS = "https"
        const val AUTH_CALLBACK_HTTP = "http"
        const val AUTH_CALLBACK_SRBN = "srbn"
        const val AUTH_CALLBACK_SRBNS = "srbns"
        const val TAG = "Auth47ViewModel"
    }

    class Auth47Exception(error: String) : Exception(error)

    private val errors = MutableLiveData<String?>(null)
    private val loading = MutableLiveData(false)
    private val authSuccess = MutableLiveData(false)
    private val authChallenge = MutableLiveData("")
    private val authCallbackDomain = MutableLiveData("")
    private val resourceHost = MutableLiveData("")
    private val page = MutableLiveData(0)
    private val challengeState = MutableLiveData("")

    val errorsLive: LiveData<String?> get() = errors
    val authCallbackDomainLive: LiveData<String> get() = authCallbackDomain
    val resourceHostLive: LiveData<String> get() = resourceHost
    val authChallengeLive: LiveData<String> get() = authChallenge
    val loadingLive: LiveData<Boolean> get() = loading
    val authSuccessLive: LiveData<Boolean> get() = authSuccess
    val pageLive: LiveData<Int> get() = page
    val challengeStateLive: LiveData<String> get() = challengeState


    fun setChallengeValue(authChallengeEdit: String) {
        authChallenge.postValue(authChallengeEdit)
        challengeState.postValue("")
        viewModelScope.launch {
            try {
                val isValid = validateChallenge(authChallengeEdit)
                if (isValid) {
                    page.postValue(1)
                    challengeState.postValue(STATE_CHALLENGE_VALID);
                } else {
                    challengeState.postValue(STATE_CHALLENGE_NOT_VALID)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                challengeState.postValue(STATE_CHALLENGE_NOT_VALID)
            }
        }
    }

    private suspend fun validateChallenge(challenge: String) = withContext(Dispatchers.Default) {
        val url = URI(challenge)
        if (url.scheme != AUTH_SCHEME) {
            throw Auth47Exception("invalid auth challenge")
        }
        val callbackValue = UrlQuerySanitizer(challenge).getValue("c")
        if (callbackValue.isNullOrEmpty()) {
            throw Auth47Exception("invalid auth callback")
        }
        val resourceParam = UrlQuerySanitizer(challenge).getValue("r")
        val callbackURI = URI(callbackValue)
        authCallbackDomain.postValue(callbackURI.host)
        if (!resourceParam.isNullOrEmpty()) {
            try {
                val resourceParamUri = URI(resourceParam)
                resourceHost.postValue(resourceParamUri.host)
            } catch (e: Exception) {
            }
        }
        if (callbackURI.scheme == AUTH_CALLBACK_HTTPS || callbackURI.scheme == AUTH_CALLBACK_HTTP) {
            return@withContext true
        }
        if (callbackURI.scheme == AUTH_CALLBACK_SRBN || callbackURI.scheme == AUTH_CALLBACK_SRBNS) {
            throw Auth47Exception("Soroban url not supported yet")
        }
        throw Auth47Exception("invalid callback url")
    }

    //start auth process by signing challenge and sending to callback url with signature
    fun initiateAuthentication(context: Context) {
        if (AppUtil.getInstance(context).isOfflineMode) {
            Toast.makeText(context, R.string.in_offline_mode, Toast.LENGTH_SHORT).show()
            return
        }

        loading.postValue(true)
        authSuccess.postValue(false)
        errors.postValue(null)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val pcode = BIP47Util.getInstance(context).paymentCode.toString();
                    val originalChallenge = authChallenge.value
                    val uri = Uri.parse(originalChallenge)
                    val callback = uri.getQueryParameter("c") ?: return@withContext
                    val challengePayload = signChallenge(originalChallenge, context)
                    //TODO: srbn support
                    val response = PayNymApiService.getInstance(pcode, context).auth47(callback, challengePayload)
                    val body = response.body?.string() ?: "";
                    if (response.isSuccessful) {
                        authSuccess.postValue(true)
                    } else {
                        throw  Exception(body)
                    }

                } catch (error: Exception) {
                    errors.postValue("Error ${error.message}")
                    throw  CancellationException(error.message)
                }
            }
        }.invokeOnCompletion {
            loading.postValue(false)
        }
    }


    //Checks if the challenge is valid and signs it and returns the json payload
    private fun signChallenge(originalChallengeUri: String?, context: Context): JSONObject {
        val pcode = BIP47Util.getInstance(context).paymentCode.toString();
        val uri = Uri.parse(originalChallengeUri)
        val scheme = uri.scheme
        val nonce = uri.host
        val callback = uri.getQueryParameter("c")
        var resource = uri.getQueryParameter("r")
        val expiry = uri.getQueryParameter("e")
        if (resource.isNullOrEmpty()) {
            resource = callback
        }
        val challenge = "$scheme://${nonce}?r=$resource${if (expiry == null) "" else "&e=$expiry"}"
        val singedChallenge = MessageSignUtil.getInstance().signMessage(BIP47Util.getInstance(context).notificationAddress.ecKey, challenge)
        val payload = JSONObject()
            .apply {
                put("auth47_response", "1.0")
                put("challenge", challenge)
                put("signature", singedChallenge)
                put("nym", pcode)
            }
        return payload
    }

    fun clear() {
        loading.postValue(false)
        authSuccess.postValue(false)
        errors.postValue(null)
        authChallenge.postValue("")
        page.postValue(0)
    }
}