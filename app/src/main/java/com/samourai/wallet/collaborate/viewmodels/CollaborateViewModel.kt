package com.samourai.wallet.collaborate.viewmodels

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.samourai.soroban.client.meeting.SorobanRequestMessage
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.cahoots.AndroidSorobanWalletService
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.paynym.models.NymResponse
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.json.JSONObject

class CollaborateViewModel : ViewModel() {

    enum class CahootListenState {
        WAITING,
        TIMEOUT,
        STOPPED
    }

    private var timer: CountDownTimer? = null;
    private val errors = MutableLiveData<String?>(null);
    private var compositeDisposable = CompositeDisposable();
    private var followingList = ArrayList<String>()
    private var followingListLive = MutableLiveData<ArrayList<String>>()
    private var loading = MutableLiveData<Boolean>()
    private var cahootsListenState = MutableLiveData(CahootListenState.STOPPED)
    private var sorobanTimeout = MutableLiveData<Long?>()
    private var meetingAccount = MutableLiveData(-1)
    private var sorobanWalletCounterparty: SorobanWalletCounterparty? = null;
    private var sorobanRequest = MutableLiveData<SorobanRequestMessage?>()

    private var sorobanListenJob: Job? = null;

    val following: LiveData<ArrayList<String>>
        get() = followingListLive

    val sorobanRequestLive: LiveData<SorobanRequestMessage?>
        get() = sorobanRequest

    val meetingAccountLive: LiveData<Int>
        get() = meetingAccount

    val loadingLive: LiveData<Boolean>
        get() = this.loading

    val sorobanTimeoutLive: LiveData<Long?>
        get() = sorobanTimeout

    val cahootsListenStateLive: LiveData<CahootListenState>
        get() = cahootsListenState

    val errorsLive: LiveData<String?>
        get() = errors

    fun initWithContext(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var androidSorobanWalletService = AndroidSorobanWalletService.getInstance(context);
                sorobanWalletCounterparty = androidSorobanWalletService.sorobanWalletCounterparty;
                withContext(Dispatchers.Main) {
                    this@CollaborateViewModel.loading.postValue(true)
                }
                val strPaymentCode = BIP47Util.getInstance(context).paymentCode.toString()
                val apiService = PayNymApiService.getInstance(strPaymentCode, context)
                try {
                    val response = apiService.getNymInfo()
                    withContext(Dispatchers.Main) {
                        this@CollaborateViewModel.loading.postValue(false)
                    }
                    if (response.isSuccessful) {
                        val responseJson = response.body?.string()
                        if (responseJson != null) {
                            val jsonObject = (JSONObject(responseJson))
                            val nym = Gson().fromJson(jsonObject.toString(), NymResponse::class.java);
                            val array = jsonObject.getJSONArray("codes")
                            if (array.getJSONObject(0).has("claimed") &&
                                array.getJSONObject(0).getBoolean("claimed")
                            ) {
                                nym.following?.let { codes ->
                                    codes.forEach { paynym ->
                                        BIP47Meta.getInstance().setSegwit(paynym.code, paynym.segwit)
                                        if (BIP47Meta.getInstance().getDisplayLabel(paynym.code).contains(paynym.code.substring(0, 4))) {
                                            BIP47Meta.getInstance().setLabel(paynym.code, paynym.nymName)
                                        }
                                    }
                                    val followings = ArrayList(codes.distinctBy { it.code }.map { it.code })
                                    BIP47Meta.getInstance().addFollowings(followings)
                                    sortByLabel(followings);
                                    viewModelScope.launch(Dispatchers.Main) {
                                        followingListLive.postValue(followings)
                                        followingList = followings
                                    }
                                }

                            }
                        } else
                            throw Exception("Invalid response ")
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        this@CollaborateViewModel.loading.postValue(false)
                    }
                    setError(ex.message ?: "Error fetching paynym")
                    throw CancellationException("Error ${ex}")
                }
            }
        }
    }

    fun setMeetingAccountIndex(index: Int) {
        meetingAccount.postValue(index)
    }

    private fun sortByLabel(list: ArrayList<String>) {
        list.sortWith { pcode1: String?, pcode2: String? ->
            var res = java.lang.String.CASE_INSENSITIVE_ORDER.compare(BIP47Meta.getInstance().getDisplayLabel(pcode1), BIP47Meta.getInstance().getDisplayLabel(pcode2))
            if (res == 0) {
                res = BIP47Meta.getInstance().getDisplayLabel(pcode1).compareTo(BIP47Meta.getInstance().getDisplayLabel(pcode2))
            }
            res
        }
    }

    fun applySearch(query: String?) {
        if (query == null) {
            followingListLive.postValue(followingList);
            return
        }
        viewModelScope.launch {
            val items = followingList.filter {
                BIP47Meta.getInstance().getDisplayLabel(it).lowercase().indexOf(query.lowercase()) != -1
            }.toList()
            followingListLive.postValue(ArrayList(items))
        }
    }

    fun startListen() {
        cahootsListenState.postValue(CahootListenState.WAITING)
        sorobanRequest.postValue(null)
        runTimer()
        sorobanListenJob = viewModelScope.launch {
            withContext(Dispatchers.Default) {
                sorobanWalletCounterparty!!.receiveMeetingRequest()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ cahootsRequest: SorobanRequestMessage ->
                        timer?.cancel()
                        sorobanRequest.postValue(cahootsRequest)
                    }, {
                        setError(it.message ?: "Unable to listen cahoot request")
                        timer?.cancel();
                    })
                    .apply {
                        compositeDisposable.add(this)
                    }
            }
        }
    }

    private fun runTimer() {
        viewModelScope.launch {
            timer = object : CountDownTimer(TIMEOUT_MS, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    sorobanTimeout.postValue(millisUntilFinished / 1000)
                }

                override fun onFinish() {
                    cahootsListenState.postValue(CahootListenState.TIMEOUT)
                }
            }
            timer?.start()
        }
    }

    fun cancelSorobanRequest() {
        val request = sorobanRequest.value ?: return
        sorobanWalletCounterparty?.sendMeetingResponse(request, false)
            ?.subscribe ({
                sorobanRequest.postValue(null)
                cahootsListenState.postValue(CahootListenState.STOPPED)
            },{
                setError(it.message ?: "Unable to cancel cahoots")
            })
            ?.apply {
                compositeDisposable.add(this)
            }
    }

    fun acceptRequest(context: Context) {
        val request = sorobanRequest.value ?: return
        sorobanWalletCounterparty?.sendMeetingResponse(request, true)
            ?.subscribe({
                sorobanRequest.postValue(null)
                timer?.cancel()
                sorobanTimeout.postValue(null)
                cahootsListenState.postValue(CahootListenState.STOPPED)
                if (meetingAccount.value == null) {
                    return@subscribe
                }
                val intent = SorobanCahootsActivity.createIntentCounterparty(context, meetingAccount.value!!, request.type, request.sender)
                context.startActivity(intent)
            }, {
                setError(it.message ?: "Unable to accept cahoots")
            })
            ?.apply {
                compositeDisposable.add(this)
            }
    }

    private fun setError(message: String) {
        errors.postValue(message)
        viewModelScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
                errors.postValue(null)
            }
        }
    }

    fun clearSorobanListen() {
        sorobanRequest.postValue(null)
        timer?.cancel()
        sorobanTimeout.postValue(null)
        cahootsListenState.postValue(CahootListenState.STOPPED)
        sorobanListenJob?.cancel()
        timer?.cancel()
    }

    override fun onCleared() {
        clearSorobanListen()
        compositeDisposable.dispose()
        super.onCleared()
    }

    companion object {
        private const val TIMEOUT_MS = 60000L
    }
}