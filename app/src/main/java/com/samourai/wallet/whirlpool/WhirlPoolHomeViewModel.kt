package com.samourai.wallet.whirlpool

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Pair
import androidx.lifecycle.*
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.APIFactory.TxMostRecentDateComparator
import com.samourai.wallet.api.Tx
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.home.BalanceViewModel
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.send.BlockedUTXO
import com.samourai.wallet.service.JobRefreshService
import com.samourai.wallet.util.LogUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.beans.MixableStatus
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService.ConnectionStates as Connection

/**
 * samourai-wallet-android
 */
class WhirlPoolHomeViewModel : ViewModel() {


    private val whirlpoolLoading = MutableLiveData(true)
    private val refreshStatus = MutableLiveData(false)
    private val compositeDisposable = CompositeDisposable()
    private val wallet = AndroidWhirlpoolWalletService.getInstance();

    private val mixing = MutableLiveData(listOf<WhirlpoolUtxo>())
    private val mixTransactions = MutableLiveData<Map<WhirlpoolAccount,List<Tx>>>(
              mapOf()
    )
    private val remixing = MutableLiveData(listOf<WhirlpoolUtxo>())
    private val remixBalanceLive = MutableLiveData(0L)
    private val mixingBalanceLive = MutableLiveData(0L)
    private val totalBalanceLive = MutableLiveData(0L)
    private val whirlpoolOnboarded = MutableLiveData(false)
    private val displaySats = MutableLiveData(false)

    val mixingLive: LiveData<List<WhirlpoolUtxo>> get() = mixing
    val remixLive: LiveData<List<WhirlpoolUtxo>> get() = remixing
    val displaySatsLive: LiveData<Boolean> get() = displaySats
    val remixBalance: LiveData<Long> get() = remixBalanceLive
    val mixingBalance: LiveData<Long> get() = mixingBalanceLive
    val mixTransactionsList: LiveData<Map<WhirlpoolAccount,List<Tx>>> get() = mixTransactions
    val totalBalance: LiveData<Long> get() = totalBalanceLive
    val onboardStatus: LiveData<Boolean> get() = whirlpoolOnboarded
    val listRefreshStatus: LiveData<Boolean> get() = refreshStatus

    init {
        val disposable = wallet.listenConnectionStatus()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it?.let {
                    when (it) {
                        Connection.CONNECTED -> {
                            toggleLoader(false)
                            loadUtxos()
                            loadBalances()
                        }
                        Connection.STARTING -> {
                            toggleLoader(true)

                        }
                        Connection.LOADING -> {
                            toggleLoader(true)
                        }
                        Connection.DISCONNECTED -> {
                            toggleLoader(false)
                        }
                    }
                }
            }, {
            })
         compositeDisposable.add(disposable)


         viewModelScope.launch(Dispatchers.Default){
                while (viewModelScope.isActive){
                    delay(1800)
                    loadUtxos()
                    loadBalances()
                }
        }
    }

    private fun loadBalances() {
        if (wallet.whirlpoolWallet.isPresent) {
            val postMix =
                wallet.whirlpoolWallet.get().utxoSupplier.findUtxos(WhirlpoolAccount.POSTMIX)
            val preMix =
                wallet.whirlpoolWallet.get().utxoSupplier.findUtxos(WhirlpoolAccount.PREMIX)

            val premixBalance =  preMix
                    .filter { it.utxoState.mixableStatus == MixableStatus.MIXABLE }
                    .map { it.utxo.value }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { acc, l -> acc + l } ?: 0L

            val balance =
                (premixBalance  + wallet.whirlpoolWallet.get().utxoSupplier.getBalance(WhirlpoolAccount.POSTMIX))
            try {
                //Filter non-mixable utxo's from postmix account
                val remixBalance = postMix
                    .filter { it.utxoState.mixableStatus == MixableStatus.MIXABLE }
                    .map { it.utxo.value }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { acc, l -> acc + l } ?: 0L
                remixBalanceLive.postValue(remixBalance)
                mixingBalanceLive.postValue(premixBalance)
                totalBalanceLive.postValue(balance)
            } catch (ex: Exception) {
            }
        }
    }

    private fun loadUtxos() {
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet()
        if (whirlpoolWallet != null) {

            val disposable = whirlpoolWallet.mixingState.observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mixingState ->
                    val utxoPremix: List<WhirlpoolUtxo> = ArrayList(
                        whirlpoolWallet.utxoSupplier.findUtxos(WhirlpoolAccount.PREMIX)
                    )
                    loadBalances()
                    val remixingUtxoState: List<WhirlpoolUtxo> = ArrayList(
                        whirlpoolWallet.utxoSupplier.findUtxos(WhirlpoolAccount.POSTMIX)
                    )

                    val remixingUtxo = mutableListOf<WhirlpoolUtxo>()
                    remixingUtxo.addAll(remixingUtxoState.filter { it.utxoState.mixableStatus != MixableStatus.NO_POOL })
                    remixing.postValue(remixingUtxo)
                    mixing.postValue(utxoPremix.filter { it.utxoState.mixableStatus != MixableStatus.NO_POOL })
                }, {

                })
            compositeDisposable.add(disposable)
        }
    }

    private fun toggleLoader(loading: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            whirlpoolLoading.postValue(loading)
        }
    }


    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }

    fun setOnBoardingStatus(status: Boolean) {
        this.whirlpoolOnboarded.postValue(status);
    }

    fun refresh() {
        loadUtxos()
        loadBalances()
    }


    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (BalanceActivity.DISPLAY_INTENT == intent.action) {
                   setRefresh(false)
                }
            }
        }
    }



    fun refreshList(context:Context) {
        val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet();
        if (wallet != null) {
            setRefresh(true)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main){
                        val intent = Intent(context, JobRefreshService::class.java)
                        intent.putExtra("notifTx", false)
                        intent.putExtra("dragged", true)
                        intent.putExtra("launch", false)
                        JobRefreshService.enqueueWork(context, intent)
                    }
                    wallet.refreshUtxosAsync().blockingAwait()
                    refresh()
                }catch (e:Exception){
                    throw  CancellationException()
                }

            }
        }
    }

    fun setRefresh(b: Boolean) {
        refreshStatus.postValue(b)
    }

    fun loadOfflineTxData(context: Context) {
        try {
            val response = PayloadUtil.getInstance(context).deserializeMultiAddrMix()
            val apiFactory = APIFactory.getInstance(context)
            if (response != null) {
                val parser = apiFactory.parseMixXPUBObservable(JSONObject(response.toString()))
                val disposable = parser
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pairValues: Pair<List<Tx>, Long> ->
                        setTx(apiFactory.allPostMixTxs,apiFactory.allPremixTx)
                    }) { error: Throwable ->
                         LogUtil.error("WhirlPoolViewModel",error)
                    }
                compositeDisposable.add(disposable)
            }
        } catch (e: IOException) {
            LogUtil.error("WhirlPoolViewModel",e)
        } catch (e: JSONException) {
            LogUtil.error("WhirlPoolViewModel",e)
        }
    }




    fun setTx(postMixList: List<Tx>, premixList: List<Tx>) {
        mixTransactions.postValue(mapOf(
            WhirlpoolAccount.POSTMIX to postMixList,
            WhirlpoolAccount.PREMIX to premixList
        ))
    }

    fun toggleSats(boolean: Boolean?=null) {
        if(boolean!=null){
            this.displaySats.value = boolean
        }else{
            this.displaySats.value = !(this.displaySats.value ?: false)
        }
    }
}