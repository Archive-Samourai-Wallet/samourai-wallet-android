package com.samourai.wallet.tools.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.cahoots.psbt.PSBT
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction
import org.bouncycastle.util.encoders.Hex

class SignPSBTViewModel : ViewModel() {

    private val _validPSBT: MutableLiveData<PSBT?> = MutableLiveData(null)
    val validPSBT: LiveData<PSBT?> get() = _validPSBT;

    private val _signedTx: MutableLiveData<Transaction> = MutableLiveData(null)
    val signedTx: LiveData<Transaction> get() = _signedTx;

    private val _loading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _showCheck: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading;
    val showCheck: LiveData<Boolean> get() = _showCheck;

    private val _signError: MutableLiveData<String?> = MutableLiveData(null)
    val signError: LiveData<String?> get() = _signError;

    private val _hexValidationError: MutableLiveData<String?> = MutableLiveData(null)
    val hexValidationError: LiveData<String?> get() = _hexValidationError;

    private val _signSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    val signSuccess: LiveData<Boolean> get() = _signSuccess;

    private val _page: MutableLiveData<Int> = MutableLiveData(0)
    val page: LiveData<Int> get() = _page;


    fun setPSBT(psbtStr: String) {
        _signSuccess.postValue(false)
        _hexValidationError.postValue(null)
        _validPSBT.postValue(null)
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val psbt = PSBT.fromBytes(Hex.decode(psbtStr), SamouraiWallet.getInstance().currentNetworkParams)
                    _validPSBT.postValue(psbt)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _hexValidationError.postValue("Invalid transaction")
                }
            }
        }
    }
    fun  clear(){
        this._signError.postValue(null)
        this._validPSBT.postValue(null)
        this._page.postValue(0)
        this._loading.postValue(false)
        this._signSuccess.postValue(false)
        this._hexValidationError.postValue(null)
    }

    fun signPSBT(context: Context, psbtString: String) {
        _page.postValue(2)
        viewModelScope.launch {
            var _psbt: PSBT? = null
            _loading.postValue(true)
            withContext(Dispatchers.IO) {
                delay(2500)
                _psbt = try {
                    PSBT.fromBytes(
                        Hex.decode(psbtString),
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                } catch (e: java.lang.Exception) {
                    Toast.makeText(context, R.string.psbt_error, Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                val psbt = PSBT.fromBytes(
                    _psbt?.toBytes(),
                    SamouraiWallet.getInstance().currentNetworkParams
                )
                _signedTx.postValue(PSBTUtil.getInstance(context).doPSBTSignTx(psbt))
                _showCheck.postValue(true)
                delay(2000)
                _showCheck.postValue(false)
                _loading.postValue(false)
            }
        }
    }
}