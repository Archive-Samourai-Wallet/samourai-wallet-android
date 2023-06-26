package com.samourai.wallet.tools.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.math.MathUtils
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.SamouraiWalletConst
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.backend.beans.UnspentOutput
import com.samourai.wallet.bipFormat.BIP_FORMAT
import com.samourai.wallet.bipFormat.BipFormat
import com.samourai.wallet.bipFormat.BipFormatSupplier
import com.samourai.wallet.hd.WALLET_INDEX
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.MyTransactionOutPoint
import com.samourai.wallet.send.PushTx
import com.samourai.wallet.send.SendFactory
import com.samourai.wallet.send.beans.SweepPreview
import com.samourai.wallet.service.WalletRefreshWorker
import com.samourai.wallet.tools.viewmodels.fidelitybonds.FidelityBondsTimelockedBipFormat
import com.samourai.wallet.tools.viewmodels.fidelitybonds.FidelityBondsTimelockedBipFormatSupplier
import com.samourai.wallet.util.AddressFactory
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.BackendApiAndroid
import com.samourai.wallet.util.PrefsUtil
import com.samourai.wallet.util.PrivKeyReader
import com.samourai.wallet.util.TxUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction
import java.text.DecimalFormat
import kotlin.math.ceil

class SweepViewModel : ViewModel() {

    private val addressLiveData: MutableLiveData<String?> = MutableLiveData(null)
    private val page: MutableLiveData<Int> = MutableLiveData(0)
    private val addressValidationError: MutableLiveData<String?> = MutableLiveData()
    private val privateKeyFormat: MutableLiveData<String?> = MutableLiveData()
    private val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val validFees: MutableLiveData<Boolean> = MutableLiveData(true)
    private val dustOutput: MutableLiveData<Boolean> = MutableLiveData(false)
    private val broadcastLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    private val broadcastError: MutableLiveData<String?> = MutableLiveData(null)
    private val feeRange: MutableLiveData<Float> = MutableLiveData(0.5f)
    private val bipFormat: MutableLiveData<BipFormat?> = MutableLiveData(null)
    private val bip38Passphrase: MutableLiveData<String?> = MutableLiveData("")
    private val unspentOutputs: MutableLiveData<Collection<UnspentOutput>> = MutableLiveData(mutableListOf())
    private val foundAmount: MutableLiveData<Long> = MutableLiveData(0L)
    private val fees: MutableLiveData<Long> = MutableLiveData(0L)
    private val feesPerByte: MutableLiveData<String> = MutableLiveData("")
    private val sweepAddressLive: MutableLiveData<String> = MutableLiveData("")
    private val receiveAddressType: MutableLiveData<WALLET_INDEX> = MutableLiveData(WALLET_INDEX.BIP84_RECEIVE)
    private val decimalFormatSatPerByte = DecimalFormat("#.##").also {
        it.isDecimalSeparatorAlwaysShown = true
    }
    private var sweepPreview: SweepPreview? = null
    private val params = SamouraiWallet.getInstance().currentNetworkParams
    private val blocks: MutableLiveData<String> = MutableLiveData("6 blocks")
    var feeLow: Long = 0L
    var feeHigh: Long = 0L
    var feeMed: Long = 0L
    var privKeyReader: PrivKeyReader? = null

    fun getAddressLive(): LiveData<String?> = addressLiveData
    fun getAddressValidationLive(): LiveData<String?> = addressValidationError
    fun getFeeSatsValueLive(): LiveData<String> = feesPerByte
    fun getFeeRangeLive(): LiveData<Float> = feeRange
    fun getAmountLive(): LiveData<Long> = foundAmount
    fun getPrivateKeyFormatLive(): LiveData<String?> = privateKeyFormat
    fun getPageLive(): LiveData<Int> = page
    fun getLoadingLive(): LiveData<Boolean> = loading
    fun getValidFees(): LiveData<Boolean> = validFees
    fun getDustStatus(): LiveData<Boolean> = dustOutput
    fun getBroadcastStateLive(): LiveData<Boolean> = broadcastLoading
    fun getBroadcastErrorStateLive(): LiveData<String?> = broadcastError
    fun getBIP38PassphraseLive(): LiveData<String?> = bip38Passphrase
    fun getSweepFees(): LiveData<Long> = fees
    fun getSweepAddressLive(): LiveData<String> = sweepAddressLive
    fun getReceiveAddressType(): LiveData<WALLET_INDEX> = receiveAddressType
    fun getBlockWaitTime(): LiveData<String> = blocks

    init {
        initFeeRange()
    }

    private fun initFeeRange() {
        feeLow = 1000L
        feeMed = FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong()
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()
        if (feeHigh == 1000L && feeLow == 1000L) {
            feeHigh = 3000L
        }
        if (feeHigh > feeLow && (feeMed - feeHigh) != 0L) {
            try {
                val currentSlider = (feeMed.toFloat() - feeLow.toFloat()).div(feeHigh.toFloat().minus(feeLow.toFloat()))
                feeRange.value = currentSlider
                feeRange.postValue(currentSlider)
            } catch (e: Exception) {
            }
        }
    }

    private fun setPage(selectedPage: Int) {
        page.postValue(selectedPage)
    }

    fun clear() {
        addressValidationError.postValue(null)
        feesPerByte.postValue("")
        fees.postValue(0L)
        foundAmount.postValue(0L)
        validFees.postValue(true)
        dustOutput.postValue(false)
        unspentOutputs.postValue(listOf())
        bip38Passphrase.postValue("")
        addressLiveData.postValue("")
        page.postValue(0)
        initFeeRange()
    }

    fun setAddressType(type: WALLET_INDEX) {
        receiveAddressType.postValue(type)
    }

    fun initWithContext(context: Context, keyParameter: String) {
        addressLiveData.postValue(keyParameter)
        val walletIndex = if (PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_SEGWIT, true)) WALLET_INDEX.BIP84_RECEIVE else WALLET_INDEX.BIP44_RECEIVE
        receiveAddressType.postValue(walletIndex)
        if (keyParameter.isNotEmpty()) {
            setAddress(privKey = keyParameter, context = context, timelockDerivationIndex = -1)
        }
    }

    fun setAddress(
        privKey: String?,
        context: Context,
        bip38Passphrase: String? = null,
        timelockDerivationIndex: Int = -1) {

        if (privKey.isNullOrEmpty()) {
            addressValidationError.postValue(null)
            return
        }
        try {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    privKeyReader = PrivKeyReader(privKey, params)
                    val format = privKeyReader?.format
                    if (format == null) {
                        addressValidationError.postValue(context.getString(R.string.cannot_recognize_privkey))
                    } else {
                        privateKeyFormat.postValue(format)
                        addressValidationError.postValue(null)
                        if (privKeyReader!!.format == PrivKeyReader.BIP38 && bip38Passphrase.isNullOrEmpty()) {
                            return@withContext
                        } else {
                            privKeyReader = PrivKeyReader(privKey, params, bip38Passphrase);
                        }
                        if (AppUtil.getInstance(context).isOfflineMode) {
                            addressValidationError.postValue(context.getString(R.string.in_offline_mode))
                            throw  CancellationException()
                        }
                        try {
                            findUTXOs(context, timelockDerivationIndex)
                        } catch (e: Exception) {
                            throw CancellationException(e.message)
                        }
                        if (unspentOutputs.value?.size == 0) {
                            loading.postValue(false)
                            addressValidationError.postValue(context.resources.getString(R.string.sweep_no_amount))
                        }
                    }
                }
            }.invokeOnCompletion {
                if (it != null) {
                    it.printStackTrace()
                    if (it.message?.contains(context.getString(R.string.in_offline_mode)) == false) {
                        addressValidationError.postValue("Error $it")
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            loading.postValue(false)
            addressValidationError.postValue("Error $e")
            return
        }
        this.addressLiveData.postValue(privKey)
    }


    //Check if the amount and fees are feasible for a tx
    private suspend fun validateFees(): Boolean {
        val totalValue = UnspentOutput.sumValue(unspentOutputs.value)
        val feePerKb = MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), feeRange.value ?: 0f).coerceAtLeast(1f)
        val fee: Long = computeFee(bipFormat.value!!, unspentOutputs.value!!, feePerKb.div(1000.0).toLong())
        val amount = totalValue - fee
        val isDust = amount <= SamouraiWallet.bDust.toLong();
        withContext(Dispatchers.Main) {
            dustOutput.postValue(isDust)
            fees.postValue(fee)
            validFees.postValue(!(amount == 0L || fee > totalValue))
        }
        return !(amount == 0L || fee > totalValue || amount <= SamouraiWalletConst.bDust.toLong())
    }

    private suspend fun findUTXOs(context: Context, timelockDerivationIndex: Int = -1) {

        withContext(Dispatchers.IO) {

            loading.postValue(true)

            val bipFormats: Collection<BipFormat> = getBipFormats(timelockDerivationIndex)
            bipFormats.forEach {
                // find utxo
                val address = it.getToAddress(privKeyReader!!.key, privKeyReader!!.params)
                val items = BackendApiAndroid.getInstance(context).fetchAddressForSweep(address)
                if (items != null && items.isNotEmpty()) {
                    unspentOutputs.postValue(items)
                    withContext(Dispatchers.Main) {
                        bipFormat.value = it
                        unspentOutputs.value = items
                    }
                    bipFormat.postValue(it)
                    makeTransaction(context)
                }
            }
        }
    }

    private fun getBipFormats(timelockDerivationIndex: Int = -1) : List<BipFormat> {
        if (timelockDerivationIndex >= 0) {
            return listOf(FidelityBondsTimelockedBipFormat.create(timelockDerivationIndex))
        } else {
            return listOf(
                BIP_FORMAT.LEGACY,
                BIP_FORMAT.SEGWIT_COMPAT,
                BIP_FORMAT.SEGWIT_NATIVE,
                BIP_FORMAT.TAPROOT
            )
        }
    }

    private fun makeTransaction(context: Context) {
        if (bipFormat.value == null || unspentOutputs.value == null || privKeyReader == null) {
            return
        }
        feeLow = 1000L
        feeMed = FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong()
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()
        if (feeHigh == 1000L && feeLow == 1000L) {
            feeHigh = 3000L
        }
        viewModelScope.launch {
            try {
                Log.i(TAG, "makeTransaction: Making Tx")
                withContext(Dispatchers.Default) {
                    val receiveAddress = AddressFactory.getInstance(context).getAddress(receiveAddressType.value).right
                    val rbfOptin = PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false)
                    val blockHeight = APIFactory.getInstance(context).latestBlockHeight
                    val totalValue = UnspentOutput.sumValue(unspentOutputs.value)
                    val address: String? = bipFormat.value?.getToAddress(privKeyReader!!.key, privKeyReader!!.params)
                    var feePerKb = MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), feeRange.value ?: 0f).coerceAtLeast(1f)
                    var fee: Long = computeFee(bipFormat.value!!, unspentOutputs.value!!, feePerKb.div(1000.0).toLong())
                    var amount = totalValue - fee
                    //Check if the amount too low for a tx or miner fee is high
                    if (amount == 0L || fee > totalValue || amount <= SamouraiWalletConst.bDust.toLong()) {
                        //check if the tx is possible with 1sat/b rate
                        withContext(Dispatchers.Main) {
                            feeRange.value = 0.1f
                            feeRange.postValue(0.1f)
                        }
                        feePerKb = MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), 0.0f).coerceAtLeast(1f)
                        fee = computeFee(bipFormat.value!!, unspentOutputs.value!!, feePerKb.div(1000.0).toLong())
                        amount = totalValue - fee
                    }
                    sweepPreview = SweepPreview(amount, address, bipFormat.value, fee, unspentOutputs.value, privKeyReader!!.key, privKeyReader!!.params)
                    val params = sweepPreview!!.params
                    val receivers: MutableMap<String, Long> = LinkedHashMap()
                    receivers[receiveAddress] = sweepPreview!!.amount
                    val outpoints: MutableCollection<MyTransactionOutPoint> = mutableListOf()
                    sweepPreview!!.utxos
                        .map { unspentOutput: UnspentOutput -> unspentOutput.computeOutpoint(params) }.toCollection(outpoints);
                    val bipFormatSupplier: BipFormatSupplier = getBipFormatSupplier(bipFormat.value);
                    val tr = createTransaction(receivers, outpoints, bipFormatSupplier, rbfOptin, blockHeight)
                    val transaction = TransactionForSweepHelper.signTransactionForSweep(tr, sweepPreview!!.privKey, params, bipFormatSupplier)
                    sweepAddressLive.postValue(address ?: "")
                    fees.postValue(transaction.fee.value)
                    foundAmount.postValue(totalValue)
                    val currentFeesPerByte = decimalFormatSatPerByte.format(transaction.fee.value.toFloat() / transaction.virtualTransactionSize.toFloat())
                    feesPerByte.postValue(currentFeesPerByte)
                }

                /**
                 * creation of a new context to allow the update of the model (postValue execution)
                 * before consuming it to calculate the number of estimated waiting blocks
                 */
                withContext(Dispatchers.Default) {
                    val pct: Double
                    var nbBlocks = 6
                    val feeForBlocks = if (getFeeSatsValueLive().value?.isEmpty() == true) 0.0 else getFeeSatsValueLive().value?.toDouble()?.times(1000.0)

                    if (feeForBlocks != null) {
                        if (feeForBlocks <= feeLow.toDouble()) {
                            pct = feeLow.toDouble() / feeForBlocks
                            nbBlocks = ceil(pct * 24.0).toInt()
                        } else if (feeForBlocks >= feeHigh.toDouble()) {
                            pct = feeHigh.toDouble() / feeForBlocks
                            nbBlocks = ceil(pct * 2.0).toInt()
                            if (nbBlocks < 1) {
                                nbBlocks = 1
                            }
                        } else {
                            pct = feeMed.toDouble() / feeForBlocks
                            nbBlocks = ceil(pct * 6.0).toInt()
                        }
                    }
                    var strBlocks = "$nbBlocks blocks"
                    if (nbBlocks > 50) {
                        strBlocks = "50+ blocks"
                    }
                    blocks.postValue(strBlocks)
                    loading.postValue(false)
                    setPage(1)
                }
            } catch (e: Exception) {
                addressValidationError.postValue("${e.message}")
                loading.postValue(false)
                e.printStackTrace()
            }
        }

    }

    private fun getBipFormatSupplier(bipFormat: BipFormat?): BipFormatSupplier {
        if (FidelityBondsTimelockedBipFormat.ID.equals(bipFormat?.id)) {
            return FidelityBondsTimelockedBipFormatSupplier.create(bipFormat as FidelityBondsTimelockedBipFormat?);
        }
        return BIP_FORMAT.PROVIDER;
    }

    fun setFeeRange(it: Float, context: Context) {
        feeRange.postValue(it)
        feeRange.value = it
        viewModelScope.launch {
            if (validateFees()) {
                makeTransaction(context = context)
            }
        }

    }

    private fun computeFee(bipFormat: BipFormat, unspentOutputs: Collection<UnspentOutput?>, feePerB: Long): Long {
        var inputsP2PKH = 0
        var inputsP2WPKH = 0
        var inputsP2SH_P2WPKH = 0
        if (bipFormat === BIP_FORMAT.SEGWIT_COMPAT) {
            inputsP2SH_P2WPKH = unspentOutputs.size
        } else if (bipFormat === BIP_FORMAT.SEGWIT_NATIVE) {
            inputsP2WPKH = unspentOutputs.size
        } else {
            inputsP2PKH = unspentOutputs.size
        }
        return FeeUtil.getInstance().estimatedFeeSegwit(inputsP2PKH, inputsP2SH_P2WPKH, inputsP2WPKH, 1, 0, feePerB)
    }

    fun initiateSweep(context: Context) {
        setPage(2)
        broadcastError.postValue(null)
        broadcastLoading.postValue(true)
        viewModelScope.launch {
            var transaction: Transaction? = null
            withContext(Dispatchers.IO) {
                delay(1500)
                try {
                    val receiveAddress = AddressFactory.getInstance(context).getAddressAndIncrement(receiveAddressType.value).right
                    val rbfOptin = PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false)
                    val blockHeight = APIFactory.getInstance(context).latestBlockHeight
                    val receivers: MutableMap<String, Long> = LinkedHashMap()
                    receivers[receiveAddress] = sweepPreview!!.amount
                    val bipFormatSupplier: BipFormatSupplier = getBipFormatSupplier(bipFormat.value);
                    val outpoints: MutableCollection<MyTransactionOutPoint> = mutableListOf()
                    sweepPreview!!.utxos
                        .map { unspentOutput: UnspentOutput -> unspentOutput.computeOutpoint(params) }.toCollection(outpoints)

                    val tr = createTransaction(
                        receivers,
                        outpoints,
                        bipFormatSupplier,
                        rbfOptin,
                        blockHeight)

                    transaction = TransactionForSweepHelper.signTransactionForSweep(tr, sweepPreview!!.privKey, params, bipFormatSupplier)
                } catch (e: Exception) {
                    throw  CancellationException("Sign: ${e.message}")
                }
                try {
                    if (transaction != null) {
                        val hexTx = TxUtil.getInstance().getTxHex(transaction)
                        PushTx.getInstance(context).pushTx(hexTx)
                        WalletRefreshWorker.enqueue(context, notifTx = false, launched = false)
                    }
                } catch (e: Exception) {
                    throw  CancellationException("pushTx : ${e.message}")
                }

            }
        }
            .invokeOnCompletion {
                broadcastLoading.postValue(false)
                if (it != null) {
                    broadcastError.postValue(it.message)
                }
            }

    }

    private fun createTransaction(
        receivers: MutableMap<String, Long>,
        outpoints: MutableCollection<MyTransactionOutPoint>,
        bipFormatSupplier: BipFormatSupplier,
        rbfOptin: Boolean,
        blockHeight: Long
    ): Transaction? {

        if (FidelityBondsTimelockedBipFormat.ID.equals(bipFormat?.value?.id)) {
            return TransactionForSweepHelper.makeTimelockTransaction(receivers, outpoints,
                bipFormat!!.value as FidelityBondsTimelockedBipFormat?, params)
        } else {
            return SendFactory.getInstance()
                .makeTransaction(receivers, outpoints, bipFormatSupplier, rbfOptin, params, blockHeight)
        }
    }


    companion object {
        private const val TAG = "SweepViewModel"
    }

}
