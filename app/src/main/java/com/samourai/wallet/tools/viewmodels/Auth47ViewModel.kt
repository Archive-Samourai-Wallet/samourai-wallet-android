package com.samourai.wallet.tools.viewmodels

import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.soroban.client.SorobanContext
import com.samourai.soroban.client.SorobanServer
import com.samourai.soroban.client.SorobanService
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.util.MessageSignUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URI


class Auth47ViewModel : ViewModel() {

    companion object {
        const val BIP47_AUTH_SCHEME = "auth47"
        const val TAG = "Auth47ViewModel"
    }

    class Auth47Exception(error: String) : Exception(error)

    private val errors = MutableLiveData<String?>(null)
    private val loading = MutableLiveData(false)
    private val authSuccess = MutableLiveData(false)
    private val authChallenge = MutableLiveData("")
    private val authCallbackDomain = MutableLiveData("")
    private val authWarnings = MutableLiveData("")
    private val page = MutableLiveData(0)

    val errorsLive: LiveData<String?> get() = errors
    val authCallbackDomainLive: LiveData<String> get() = authCallbackDomain
    val authWarningsLive: LiveData<String> get() = authWarnings
    val authChallengeLive: LiveData<String> get() = authChallenge
    val loadingLive: LiveData<Boolean> get() = loading
    val authSuccessLive: LiveData<Boolean> get() = authSuccess
    val pageLive: LiveData<Int> get() = page


    fun setChallengeValue(authChallengeEdit: String) {
        authChallenge.postValue(authChallengeEdit)
        viewModelScope.launch {
            try {
                val isValid = validateChallenge(authChallengeEdit)
                if (isValid) {
                    page.postValue(1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun validateChallenge(challenge: String) = withContext(Dispatchers.Default) {
        val url = URI(challenge)
        if (url.scheme != BIP47_AUTH_SCHEME) {
            throw Auth47Exception("invalid auth challenge")
        }
        val callbackValue = UrlQuerySanitizer(challenge).getValue("c")
        if (callbackValue.isNullOrEmpty()) {
            throw Auth47Exception("invalid auth callback")
        }
        val callbackURI = URI(callbackValue)
        if (callbackURI.scheme == "https" || callbackURI.scheme == "http") {
            authCallbackDomain.postValue(callbackURI.host)
            return@withContext true
        }
        if(callbackURI.scheme == "srbn"){
            throw Auth47Exception("Soroban url not supported yet")
        }
        throw Auth47Exception("invalid callback url")
    }

    fun initiateAuthentication(context: Context) {
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
                    val challenge = originalChallenge?.replace("r=$callback", "c=$callback") ?: ""
                    val singedMessage = MessageSignUtil.getInstance().signMessage(BIP47Util.getInstance(context).notificationAddress.ecKey, challenge)
                    val payload = JSONObject()
                        .apply {
                            put("auth47_response", "1.0")
                            put("challenge", challenge)
                            put("signature", singedMessage)
                            put("nym", pcode)
                        }
                    val response = PayNymApiService.getInstance(pcode, context).auth47(callback, payload)
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

    fun clear() {
        loading.postValue(false)
        authSuccess.postValue(false)
        errors.postValue(null)
        authChallenge.postValue("")
        page.postValue(0)
    }
}