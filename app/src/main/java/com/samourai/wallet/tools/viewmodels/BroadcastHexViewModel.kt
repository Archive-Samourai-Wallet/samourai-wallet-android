package com.samourai.wallet.tools.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.send.PushTx
import com.samourai.wallet.util.tech.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction
import org.bouncycastle.util.encoders.Hex

class BroadcastHexViewModel : ViewModel() {

    private val _validTransaction: MutableLiveData<Transaction?> = MutableLiveData(null)
    val validTransaction: LiveData<Transaction?> get() = _validTransaction;

    private val _loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading;

    private val _broadcastError: MutableLiveData<String?> = MutableLiveData(null)
    val broadcastError: LiveData<String?> get() = _broadcastError;

    private val _hexValidationError: MutableLiveData<String?> = MutableLiveData(null)
    val hexValidationError: LiveData<String?> get() = _hexValidationError;

    private val _broadCastSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    val broadCastSuccess: LiveData<Boolean> get() = _broadCastSuccess;

    private val _page: MutableLiveData<Int> = MutableLiveData(0)
    val page: LiveData<Int> get() = _page;


    fun setHex(strHexTx: String) {
        _broadCastSuccess.postValue(false)
        _hexValidationError.postValue(null)
        _validTransaction.postValue(null)
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val tx = Transaction(SamouraiWallet.getInstance().currentNetworkParams, Hex.decode(strHexTx.trim()))
                    _validTransaction.postValue(tx)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _hexValidationError.postValue("Invalid transaction")
                }
            }
        }
    }
    fun  clear(){
        this._broadcastError.postValue(null)
        this._validTransaction.postValue(null)
        this._page.postValue(0)
        this._loading.postValue(false)
        this._broadCastSuccess.postValue(false)
        this._hexValidationError.postValue(null)
    }

    fun broadcast(context: Context, hex: String) {
        if (AppUtil.getInstance(context).isOfflineMode) {
            Toast.makeText(context, R.string.in_offline_mode, Toast.LENGTH_SHORT).show()
            return
        }
        _page.postValue(1)
        _loading.postValue(true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val value = PushTx.getInstance(context).pushTx(hex)
                    _broadCastSuccess.postValue(true)
                    _page.postValue(2)
                    _loading.postValue(false)
                } catch (e: Exception) {
                    _broadCastSuccess.postValue(false)
                    _broadcastError.postValue(e.message)
                    _loading.postValue(false)
                }
            }
        }
    }


}