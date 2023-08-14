package com.samourai.wallet.send.batch

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.util.BatchSendUtil
import com.samourai.wallet.whirlpool.WhirlpoolConst
import kotlinx.coroutines.launch

class BatchSpendViewModel() : ViewModel() {


    private val batchList: MutableLiveData<ArrayList<BatchSendUtil.BatchSend>> = MutableLiveData()
    private val balance: MutableLiveData<Long> = MutableLiveData()
    private val feeLivedata: MutableLiveData<Long> by lazy {
        MutableLiveData(0L)
    }

    init {
        BatchSendUtil.getInstance().sends?.let {
            if (it.size != 0) {
                batchList.postValue(ArrayList(it))
            }
        }
    }

    fun getBatchListLive(): LiveData<ArrayList<BatchSendUtil.BatchSend>> {
        return batchList
    }

    fun getBatchList():   ArrayList<BatchSendUtil.BatchSend> {
        return batchList.value ?: arrayListOf()
    }

    fun getFee(): LiveData<Long> {
        return feeLivedata
    }

    fun setFee(fee: Long) {
        feeLivedata.postValue(fee)
    }

    fun totalWalletBalance(): Long? {
        return balance.value;
    }

    fun isValidBatchSpend(): Boolean {
        if (getBatchList().size <= 0) return false
        val bip47Meta = BIP47Meta.getInstance()
        for (item in getBatchList()) {
            if (item.pcode == null) continue
            if (bip47Meta.getOutgoingStatus(item.pcode) != BIP47Meta.STATUS_SENT_CFM) {
                return false
            }
        }
        return true
    }

    //A live-data instance that returns balance
    //balance will be recalculated when batch list changed
    fun getBalance(): LiveData<Long?> {
        return MediatorLiveData<Long?>().apply {
            fun update() {
                try {
                    val totalBatchAmount = getBatchAmount()
                    value = balance.value?.minus(totalBatchAmount)
                } catch (e: Exception) { }
            }
            addSource(batchList) { update() }
            addSource(balance) { update() }
        }
    }

    fun getBatchAmount(): Long {
        return  batchList.value?.map { it.amount }
                .takeIf { it?.isNotEmpty() ?: false }
                ?.reduce { acc, l -> acc + l }
                ?: 0L
    }

    fun add(batchItem: BatchSendUtil.BatchSend) {
        viewModelScope.launch {
            val list = ArrayList<BatchSendUtil.BatchSend>().apply { batchList.value?.let { addAll(it) } }

            val exist = list.find { it.UUID == batchItem.UUID }
            if (exist == null){
                list.add(batchItem)
            } else {
                list[list.indexOf(exist)] = batchItem
            }
            list.sortByDescending { it.UUID }

            list.let {
                batchList.value = list
                BatchSendUtil.getInstance().sends.clear()
                BatchSendUtil.getInstance().sends.addAll(list)
            }
        }

    }

    fun remove(it: BatchSendUtil.BatchSend) {
        val list = ArrayList<BatchSendUtil.BatchSend>().apply { batchList.value?.let { addAll(it) } }
        list.remove(it)
        batchList.postValue(list)
        BatchSendUtil.getInstance().sends.clear()
        BatchSendUtil.getInstance().sends.addAll(list)
    }

    fun setBalance(context:Context, account:Int) {
        try {
            var balance = 0L
            if (account == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
                balance = APIFactory.getInstance(context).xpubPostMixBalance
            } else {
                val tempBalance = APIFactory.getInstance(context).xpubAmounts[HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr()]
                if (tempBalance != null) {
                    balance = tempBalance
                }
            }
            this.balance.postValue(balance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearBatch(now:Boolean) {
        if (now) {
            viewModelScope.launch {
                batchList.value = arrayListOf()
                BatchSendUtil.getInstance().sends.clear()
            }
        } else {
            batchList.postValue(arrayListOf())
            BatchSendUtil.getInstance().sends.clear()
        }
    }
}
