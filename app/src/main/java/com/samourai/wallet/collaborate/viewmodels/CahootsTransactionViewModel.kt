package com.samourai.wallet.collaborate.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.math.MathUtils
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.cahoots.CahootsMode
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.constants.SamouraiAccountIndex
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity
import com.samourai.wallet.send.review.ReviewTxModel.findTransactionPriority
import com.samourai.wallet.util.func.FormatsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.ceil

class CahootsTransactionViewModel : ViewModel() {

    enum class CahootTransactionType(val cahootsType: CahootsType?, val cahootsMode: CahootsMode?) {
        STONEWALLX2_MANUAL(CahootsType.STONEWALLX2, CahootsMode.MANUAL),
        STONEWALLX2_SAMOURAI(CahootsType.STONEWALLX2, CahootsMode.SAMOURAI),
        STONEWALLX2_SOROBAN(CahootsType.STONEWALLX2, CahootsMode.SOROBAN),
        STOWAWAY_MANUAL(CahootsType.STOWAWAY, CahootsMode.MANUAL),
        STOWAWAY_SOROBAN(CahootsType.STOWAWAY, CahootsMode.SOROBAN),
        MULTI_SOROBAN(CahootsType.MULTI, CahootsMode.SOROBAN),
    }

    private val feesPerByte: MutableLiveData<String> = MutableLiveData("")
    private val estBlocks: MutableLiveData<String> = MutableLiveData("")
    private val currentPage = MutableLiveData(0)
    private val feeRange: MutableLiveData<Float> = MutableLiveData(0.5f)
    private val customFee: MutableLiveData<Long?> = MutableLiveData(null)
    private val transactionAccountType = MutableLiveData(SamouraiAccountIndex.DEPOSIT)
    private val validTransaction = MutableLiveData(false)
    private val cahootsType = MutableLiveData<CahootTransactionType?>(null)
    private val destinationAddress = MutableLiveData<String?>(null)
    private val amount = MutableLiveData(0L)
    private var collaboratorPcode = MutableLiveData<String?>(null)
    private val showSpendFromPaynymChooser = MutableLiveData(false)
    private val decimalFormatSatPerByte = DecimalFormat("#").also {
        it.isDecimalSeparatorAlwaysShown = false
        it.minimumFractionDigits = 0
    }

    fun getFeeSatsValueLive(): LiveData<String> = feesPerByte

    var balance: Long = 0L
    var feeLow: Long = 0L
    var feeHigh: Long = 0L
    var feeMed: Long = 0L

    val transactionAccountTypeLive: LiveData<Int>
        get() = transactionAccountType

    val showSpendFromPaynymChooserLive: LiveData<Boolean>
        get() = showSpendFromPaynymChooser

    val estBlockLive: LiveData<String>
        get() = estBlocks

    val validTransactionLive: LiveData<Boolean>
        get() = validTransaction

    val amountLive: LiveData<Long>
        get() = amount

    val destinationAddressLive: LiveData<String?>
        get() = destinationAddress

    val cahootsTypeLive: LiveData<CahootTransactionType?>
        get() = cahootsType

    val collaboratorPcodeLive: LiveData<String?>
        get() = collaboratorPcode

    val feeSliderValue: LiveData<Float>
        get() = feeRange

    val pageLive: LiveData<Int>
        get() = currentPage


    init {
        initFee();
    }

    private fun initFee() {
        feeLow = FeeUtil.getInstance().lowFee.defaultPerKB.toLong()
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
                calculateFees(feeRange.value ?: 0.5f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAccountType(account: Int, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    transactionAccountType.value = account
                    transactionAccountType.postValue(account)
                }
                if (account == SamouraiAccountIndex.DEPOSIT) {
                    balance = APIFactory.getInstance(context)
                        .xpubBalance
                }
                if (account == SamouraiAccountIndex.POSTMIX) {
                    balance = APIFactory.getInstance(context)
                        .xpubPostMixBalance
                }
                validate()
                setPage(1)
            }
        }
    }

    private fun validate(): Boolean {
        var valid = true
        val amountSats = (amount.value ?: 0L).toLong()
        if (balance < amountSats) {
            valid = false
        }
        if (amountSats == 0L) {
            valid = false
        }
        if (cahootsType.value?.cahootsMode == CahootsMode.SOROBAN) {
            if (!FormatsUtil.getInstance().isValidBitcoinAddress(destinationAddress.value ?: "")
                && !FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value ?: "")
            ) {
                valid = false
            }
        } else {
            if (!FormatsUtil.getInstance().isValidBitcoinAddress(destinationAddress.value ?: "")
                && !FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value ?: "")
                && !(cahootsType.value?.cahootsType == CahootsType.STOWAWAY && cahootsType.value?.cahootsMode == CahootsMode.MANUAL)
            ) {
                valid = false
            }
        }
        if (cahootsType.value?.cahootsMode == CahootsMode.SOROBAN || cahootsType.value?.cahootsMode == CahootsMode.SAMOURAI) {
            if (collaboratorPcode.value == null) {
                valid = false
            }
        }
        if (cahootsType.value?.cahootsType == CahootsType.STOWAWAY) {
            if (collaboratorPcode.value == BIP47Meta.getMixingPartnerCode()) {
                if (!FormatsUtil.getInstance().isValidBitcoinAddress(destinationAddress.value ?: "")
                    && !FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value ?: "")
                ) {
                    valid = false
                }
            }
        }
        validTransaction.postValue(valid)
        return valid;
    }

    fun setAmount(amount: Long) {
        this.amount.value = amount
        this.amount.postValue(amount)
        validate()
    }

    fun setCahootType(type: CahootTransactionType?) {
        this.collaboratorPcode.value = null
        this.collaboratorPcode.postValue(null)
        this.cahootsType.postValue(type)
        this.collaboratorPcode.value = null
        this.collaboratorPcode.postValue(null)
        this.destinationAddress.value = null
        this.destinationAddress.postValue(null)
        this.amount.value = 0
        this.amount.postValue(0)
        this.setPage(0)
    }

    fun setCollaborator(pcode: String) {
        this.collaboratorPcode.value = pcode
        this.collaboratorPcode.postValue(pcode)
        if (cahootsType.value?.cahootsMode == CahootsMode.SOROBAN && !isMultiCahoots()) {
            this.destinationAddress.postValue(pcode)
        }
        validate()
    }

    fun isMultiCahoots(): Boolean {
        return collaboratorPcode.value == BIP47Meta.getMixingPartnerCode()
    }

    fun setPage(page: Int) {
        currentPage.postValue(page)
    }

    fun setFeeRange(it: Float) {
        feeRange.value = it
        feeRange.postValue(it)
        customFee.postValue(null)
        customFee.value = null
        calculateFees(it)
    }

    fun getFeeRange() : LiveData<Float> {
        return feeRange
    }

    private fun calculateFees(it: Float) {

        FeeUtil.getInstance().normalize()

        feeLow = FeeUtil.getInstance().lowFee.defaultPerKB.toLong()
        feeMed = FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong()
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()


        //If the custom fee is not null the value will be taken as fee
        val fees = if (customFee.value != null) customFee.value!!.times(1000).toFloat() else MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), it).coerceAtLeast(1f)
        val feesPerByteValue = decimalFormatSatPerByte.format(fees / 1000);
        feesPerByte.postValue(feesPerByteValue)

        if (FeeUtil.getInstance().feeRepresentation.is1DolFeeEstimator) {

            var transactionPriority = findTransactionPriority(fees.toLong(), feeHigh, feeLow)
            var priorityDesc = transactionPriority!!.getDescription(
                FeeUtil.getInstance().feeRepresentation,
                fees.toLong(),
                feeLow,
                feeMed,
                feeHigh
            )
            estBlocks.postValue(priorityDesc)
        } else {
            //Calculate Block confirm estimation
            val pct: Double
            var nbBlocks = 6
            if (fees <= feeLow.toDouble()) {
                pct = feeLow.toDouble() / fees
                nbBlocks = ceil(pct * 24.0).toInt()
            } else if (fees >= feeHigh.toDouble()) {
                pct = feeHigh.toDouble() / fees
                nbBlocks = ceil(pct * 2.0).toInt()
                if (nbBlocks < 1) {
                    nbBlocks = 1
                }
            } else {
                pct = feeMed.toDouble() / fees
                nbBlocks = ceil(pct * 6.0).toInt()
            }
            var strBlocks = "$nbBlocks blocks"
            if (nbBlocks > 50) {
                strBlocks = "50+ blocks"
            }
            estBlocks.postValue(strBlocks)
        }
    }

    fun setAddress(addressEdit: String) {
        this.destinationAddress.value = addressEdit
        this.destinationAddress.postValue(addressEdit)
        validate()
    }

    fun send(context: Context) {
        val account = if (transactionAccountType.value == SamouraiAccountIndex.DEPOSIT) SamouraiAccountIndex.DEPOSIT else SamouraiAccountIndex.POSTMIX
        var type = cahootsType.value ?: return
        // choose Cahoots counterparty
        val value = amount.value ?: 0L
        if (value == 0L) {
            return
        }
        var address = destinationAddress.value

        if (collaboratorPcode.value == BIP47Meta.getMixingPartnerCode()) {
            type = CahootTransactionType.MULTI_SOROBAN
        }
        if (type.cahootsType == CahootsType.STOWAWAY && !isMultiCahoots()) {
            address = collaboratorPcode.value
        }
        if (FormatsUtil.getInstance().isValidPaymentCode(address)) {
            address = BIP47Util.getInstance(context).getSendAddressString(address)
        }
        val amountInSats = amount.value?.toLong() ?: 0.0
        //If the custom fee is not null the value will be taken as fee
        val feePerKb = if (customFee.value != null) customFee.value!!
        else MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), feeRange.value!!.toFloat()).coerceAtLeast(1000f).div(1000.0).toLong()
        if (CahootsMode.MANUAL == type.cahootsMode) {

            var destinationPcode: String? = null;
            if (FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value)) {
                destinationPcode = destinationAddress.value
            }

            // Cahoots manual
            val intent = ManualCahootsActivity.createIntentSender(
                context,
                account,
                type.cahootsType,
                feePerKb,
                amountInSats.toLong(),
                address,
                destinationPcode,
                null)

            context.startActivity(intent)
            return
        }
        if (CahootsMode.SOROBAN == type.cahootsMode) {
            var destinationPcode: String? = null;
            if (FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value)) {
                destinationPcode = destinationAddress.value
            }
            // Cahoots online
            val intent = SorobanCahootsActivity.createIntentSender(
                context,
                account,
                type.cahootsType,
                amountInSats.toLong(),
                feePerKb,
                address,
                collaboratorPcode.value,
                destinationPcode,
                null)

            context.startActivity(intent)
            return
        }
    }

    fun setCustomFee(fee: Long) {
        customFee.postValue(fee)
        customFee.value = fee
        calculateFees(1f)
        if (fee.times(1000) >= feeHigh) {
            feeRange.postValue(1f)
        } else {
            val currentSlider = (fee.times(1000) - feeLow.toFloat()).div(feeHigh.toFloat().minus(feeLow.toFloat()))
            feeRange.value = currentSlider
            feeRange.postValue(currentSlider)
        }
    }

    fun clearTransaction() {
        this.cahootsType.value = null
        this.cahootsType.postValue(null)
        this.destinationAddress.value = null
        this.destinationAddress.postValue(null)
        this.amount.value = 0
        this.amount.postValue(0)
        this.currentPage.value = 0
        this.currentPage.postValue(0)
        this.collaboratorPcode.value = null
        this.collaboratorPcode.postValue(null)
        this.validate()
    }

    fun showSpendPaynymChooser(show: Boolean = true) {
        showSpendFromPaynymChooser.postValue(show)
    }

}
