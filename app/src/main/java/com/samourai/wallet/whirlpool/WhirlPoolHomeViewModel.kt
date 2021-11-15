package com.samourai.wallet.whirlpool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.service.JobRefreshService
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.beans.MixableStatus
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
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
    private val remixing = MutableLiveData(listOf<WhirlpoolUtxo>())
    private val remixBalanceLive = MutableLiveData(0L)
    private val mixingBalanceLive = MutableLiveData(0L)
    private val totalBalanceLive = MutableLiveData(0L)
    private val whirlpoolOnboarded = MutableLiveData(false)

    val mixingLive: LiveData<List<WhirlpoolUtxo>> get() = mixing
    val remixLive: LiveData<List<WhirlpoolUtxo>> get() = remixing
    val remixBalance: LiveData<Long> get() = remixBalanceLive
    val mixingBalance: LiveData<Long> get() = mixingBalanceLive
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
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull
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
        val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull;
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
                    wallet.refreshUtxos()
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
}