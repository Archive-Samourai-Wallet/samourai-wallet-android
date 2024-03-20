package com.samourai.wallet.whirlpool.newPool

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.api.backend.MinerFeeTarget
import com.samourai.wallet.api.backend.beans.UnspentOutput
import com.samourai.wallet.constants.SamouraiAccount
import com.samourai.wallet.utxos.models.UTXOCoin
import com.samourai.wallet.whirlpool.WhirlpoolMeta
import com.samourai.wallet.whirlpool.WhirlpoolTx0
import com.samourai.wallet.whirlpool.models.PoolCyclePriority
import com.samourai.wallet.whirlpool.models.PoolViewModel
import com.samourai.whirlpool.client.tx0.Tx0Info
import com.samourai.whirlpool.client.tx0.Tx0Previews
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewPoolViewModel : ViewModel() {
    protected val TAG = "NewPoolViewModel"

    private val poolsLiveData: MutableLiveData<ArrayList<PoolViewModel>> = MutableLiveData(arrayListOf())
    private val utxos: MutableLiveData<List<UTXOCoin>> = MutableLiveData(arrayListOf())
    private val loadingPools: MutableLiveData<Boolean> = MutableLiveData(false)
    private val tx0PoolPriority: MutableLiveData<PoolCyclePriority> = MutableLiveData(PoolCyclePriority.NORMAL)
    private var pool: MutableLiveData<PoolViewModel?> = MutableLiveData(null);

    private val poolLoadError: MutableLiveData<Exception?> = MutableLiveData(null)
    var tx0Info: Tx0Info? = null;

    val getTx0PoolPriority: LiveData<PoolCyclePriority> get() = tx0PoolPriority
    val getPool: LiveData<PoolViewModel?> get() = pool
    val getPools: LiveData<ArrayList<PoolViewModel>> get() = poolsLiveData
    val getUtxos: LiveData<List<UTXOCoin>> get() = utxos
    val getLoadingPools: LiveData<Boolean> get() = loadingPools
    val getPoolLoadError: LiveData<Exception?> get() = poolLoadError

    fun loadTx0Info() {
        // fetch tx0Info only once (you should refresh it after each TX0)
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet()
        val scode = whirlpoolWallet.config.scode
        tx0Info = whirlpoolWallet.whirlpoolInfo.fetchTx0Info(scode)
        Log.w(TAG, "loadTx0Info success")
    }

    fun setPoolPriority(poolCyclePriority: PoolCyclePriority) {
        this.tx0PoolPriority.value = poolCyclePriority
        this.tx0PoolPriority.postValue(poolCyclePriority)
    }

    fun setUtxos(utxos: List<UTXOCoin>) {
        this.utxos.value = utxos
        this.utxos.postValue(utxos)
    }

    fun loadPools() {
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet()
                ?: return
        loadingPools.postValue(true)
        poolsLiveData.postValue(arrayListOf())
        viewModelScope.launch(Dispatchers.IO) {
            val pools = whirlpoolWallet.poolSupplier.pools
            val feeSupplier = whirlpoolWallet.minerFeeSupplier;
            try {
                var mixFeeTarget = Tx0FeeTarget.BLOCKS_2
                when (tx0PoolPriority.value) {
                    PoolCyclePriority.HIGH -> {
                        mixFeeTarget = Tx0FeeTarget.BLOCKS_2
                    }
                    PoolCyclePriority.NORMAL -> {
                        mixFeeTarget = Tx0FeeTarget.BLOCKS_6
                    }
                    PoolCyclePriority.LOW -> {
                        mixFeeTarget = Tx0FeeTarget.BLOCKS_24
                    }
                    else -> {}
                }
                var fee = feeSupplier.getFee(MinerFeeTarget.BLOCKS_2)
                if (tx0PoolPriority.value != null) {
                    when (tx0PoolPriority.value) {
                        PoolCyclePriority.HIGH -> fee = feeSupplier.getFee(MinerFeeTarget.BLOCKS_2)
                        PoolCyclePriority.NORMAL -> fee = feeSupplier.getFee(MinerFeeTarget.BLOCKS_6)
                        PoolCyclePriority.LOW -> {
                            fee = feeSupplier.getFee(MinerFeeTarget.BLOCKS_24)
                        }
                        else -> {}
                    }
                }

                val tx0 = WhirlpoolTx0(WhirlpoolMeta.getMinimumPoolDenomination(), fee.toLong(), 0, utxos.value)

                val spendFroms: MutableCollection<UnspentOutput> =
                        WhirlpoolUtils.getInstance().toUnspentOutputs(tx0.outpoints, tx0.xpub)

                val poolModels = ArrayList<PoolViewModel>();

                var tx0Previews:Tx0Previews? = null;
                if (tx0Info != null) {
                    val tx0Config = tx0Info!!.getTx0Config(mixFeeTarget, mixFeeTarget)
                    tx0Config.changeWallet = SamouraiAccount.DEPOSIT
                    tx0Previews = tx0Info!!.tx0Previews(tx0Config, spendFroms)
                }

                for (whirlpoolPool in pools) {
                    val poolViewModel = PoolViewModel(whirlpoolPool)

                    if (getPool.value != null) {
                        poolViewModel.isSelected = getPool.value!!.poolId == poolViewModel.poolId
                    }

                    if (tx0Previews!=null) {
                        poolViewModel.tx0Preview = tx0Previews.getTx0Preview(poolViewModel.pool.poolId);
                    }

                    poolModels.add(poolViewModel)
                    poolViewModel.setMinerFee(fee.toLong(),utxos.value)
                }
                if(pool.value!=null){
                    pool.postValue(poolModels.find { it.pool.poolId == pool.value!!.poolId })
                }
                poolsLiveData.postValue(poolModels)
                loadingPools.postValue(false)
            } catch (ex: Exception) {
                ex.printStackTrace()
                loadingPools.postValue(false)
                poolLoadError.postValue(ex)
                throw CancellationException(ex.message)
            }
        }
    }

    fun setSelectedPool(position: Int) {
        if (this.poolsLiveData.value != null) {
            var pools: List<PoolViewModel> = this.poolsLiveData.value!!
            pools = pools.mapIndexed { index, poolViewModel ->
                if( index == position){
                    poolViewModel.isSelected = !poolViewModel.isSelected
                }else{
                    poolViewModel.isSelected = false
                }
                poolViewModel
            }
            this.poolsLiveData.postValue(ArrayList(pools))
            this.pool.postValue(pools.find { it.isSelected })
        }
    }

}