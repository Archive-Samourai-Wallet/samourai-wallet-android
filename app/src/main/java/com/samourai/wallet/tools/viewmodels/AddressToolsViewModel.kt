package com.samourai.wallet.tools

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.constants.WALLET_INDEX
import com.samourai.wallet.hd.HD_Address
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.ricochet.RicochetMeta
import com.samourai.wallet.segwit.BIP49Util
import com.samourai.wallet.segwit.BIP84Util
import com.samourai.wallet.segwit.SegwitAddress
import com.samourai.wallet.swaps.SwapsMeta
import com.samourai.wallet.util.func.AddressFactory
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.func.MessageSignUtil
import com.samourai.wallet.whirlpool.WhirlpoolMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.json.JSONException
import org.json.JSONObject
import org.spongycastle.util.encoders.Hex

data class AddressDetailsModel(
    val pubKey: String,
    val privateKey: String,
    val redeemScript: String,
    val keyType: String?,
    var selectedIndex: Int = 0,
    var currentIndex: Int = 0,
    val ecKey: ECKey?,
    val isExternal: Boolean
)

class AddressCalculatorViewModel : ViewModel() {


    private val addressLiveData: MutableLiveData<AddressDetailsModel> = MutableLiveData(
        AddressDetailsModel("", "", "", null, 0, currentIndex = 0, ecKey = null, true)
    )

    private val pageLiveData: MutableLiveData<Int> = MutableLiveData(0)

    private val signedMessage: MutableLiveData<String> = MutableLiveData("")
    private val message: MutableLiveData<String> = MutableLiveData("")
    private val verifiedMessage: MutableLiveData<Boolean?> = MutableLiveData()

    fun getAddressLiveData(): LiveData<AddressDetailsModel> {
        return addressLiveData
    }

    fun setPage(value: Int) {
        pageLiveData.value = value
    }

    fun getPage(): LiveData<Int> = pageLiveData

    fun calculateAddress(type: String, isExternal: Boolean, index: Int, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val addressDetailsModel = addressDetailsModel(type, isExternal, index, context)
                withContext(Dispatchers.Main) {
                    addressLiveData.postValue(addressDetailsModel)
                }
            }
        }

    }

    fun executeSignMessage(messageString: String, context: Context) {
        try {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    val ecKey = addressLiveData.value?.ecKey ?: return@withContext
                    val addr = addressLiveData.value?.pubKey ?: return@withContext
                    var msg = ""
                    if (FormatsUtil.getInstance().isValidBech32(addr) || Address.fromBase58(SamouraiWallet.getInstance().currentNetworkParams, addr).isP2SHAddress) {
                        msg = context.getString(R.string.utxo_sign_text3)
                        try {
                            val obj = JSONObject()
                            obj.put("pubkey", ecKey.publicKeyAsHex)
                            obj.put("address", addr)
                            msg += " $obj"
                        } catch (je: JSONException) {
                            msg += ":"
                            msg += addr
                            msg += ", "
                            msg += "pubkey:"
                            msg += ecKey.publicKeyAsHex
                        }
                    } else {
                        msg = context.getString(R.string.utxo_sign_text2)
                    }
                    val strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(ecKey, messageString.ifEmpty { msg })
                    signedMessage.postValue(strSignedMessage)
                }
            }
        } catch (e: Exception) {
            throw e
        }

    }

    fun getSignedMessage(): LiveData<String> {
        return signedMessage
    }

    fun getMessage(): LiveData<String> {
        return message
    }

    fun isVerifiedMessage(): LiveData<Boolean?> {
        return verifiedMessage
    }

    fun clearSignedMessage() {
        signedMessage.postValue("")
    }

    fun clearMessage() {
        message.postValue("")
    }

    fun setMessage(data:String) {
        message.postValue(data)
    }

    fun clearVerifiedMessageState() {
        try {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    verifiedMessage.postValue(null)
                }
            }
        } catch (e : Exception) {
            // coroutine issue
            verifiedMessage.postValue(null)
        }
    }

    fun executeVerifyMessage(address: String, message: String, signature: String) {
        try {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    verifiedMessage.postValue(verifyMessage(address, message, signature))
                }
            }
        } catch (e : Exception) {
            // coroutine issue
            verifiedMessage.postValue(null)
        }
    }

    fun verifyMessage(addr: String, msg: String, signature: String): Boolean {
        try {
            return MessageSignUtil.getInstance().verifySignedMessage(addr, msg, signature)
        } catch (e : Exception) {
            // functional issue (like SignatureException about signature length)
            return false
        }
    }

    companion object {

        fun addressDetailsModel(
            type: String,
            isExternal: Boolean,
            index: Int,
            context: Context
        ): AddressDetailsModel {

            val types = context.resources.getStringArray(R.array.account_types)
            val pair = createAddressPair(isExternal, type, context, index)

            var currentIndex = pair.first
            val pairAddress = pair.second

            val ecKey: ECKey?
            val strAddress: String?
            if (types.indexOf(type) == 2 || types.indexOf(type) == 6) {
                ecKey = pairAddress.first?.ecKey
                strAddress = pairAddress.first?.addressString
            } else {
                ecKey = pairAddress.second?.ecKey
                strAddress = if (types.indexOf(type) == 0 || types.indexOf(type) == 7) {
                    pairAddress.second?.addressAsString
                } else {
                    pairAddress.second?.bech32AsString
                }
            }

            val strPrivKey: String =
                ecKey?.getPrivateKeyAsWiF(SamouraiWallet.getInstance().currentNetworkParams) ?: ""
            val redeemScript: String = try {
                Hex.toHexString(pairAddress.second?.segwitRedeemScript()?.program)
            } catch (e: Exception) {
                ""
            }

            val addressDetailsModel = AddressDetailsModel(
                privateKey = strPrivKey,
                redeemScript = redeemScript,
                ecKey = ecKey,
                pubKey = strAddress ?: "",
                isExternal = isExternal,
                keyType = type,
                currentIndex = currentIndex,
                selectedIndex = index
            )
            return addressDetailsModel
        }

        fun createAddressPair(
            isExternal: Boolean,
            type: String,
            context: Context,
            index: Int
        ): Pair<Int, Pair<HD_Address?, SegwitAddress?>> {

            var chain = if (isExternal) 0 else 1
            var hdAddress: HD_Address? = null
            var segwitAddress: SegwitAddress? = null
            var currentIndex = 0;

            val types = context.resources.getStringArray(R.array.account_types)
            when (types.indexOf(type)) {
                //BIP49 Segwit
                0 -> {

                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.BIP49_RECEIVE else WALLET_INDEX.BIP49_CHANGE)
                    hdAddress = BIP49Util.getInstance(context).wallet.getAccount(0).getChain(chain)
                        .getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )

                }
                //                    BIP84 Segwit
                1 -> {
                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.BIP84_RECEIVE else WALLET_INDEX.BIP84_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(0).getChain(chain)
                        .getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }
                //                    BIP44 P2PKH
                2 -> {
                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.BIP44_RECEIVE else WALLET_INDEX.BIP44_CHANGE)
                    hdAddress =
                        HD_WalletFactory.getInstance(context).get().getAccount(0).getChain(chain)
                            .getAddressAt(index)
                    segwitAddress = null
                }
                //                    Ricochet Segwit
                3 -> {
                    currentIndex = RicochetMeta.getInstance(context).index;
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        RicochetMeta.getInstance(context).ricochetAccount
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }
                //                    Whirlpool Segwit pre-mix
                4 -> {
                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.PREMIX_RECEIVE else WALLET_INDEX.PREMIX_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        WhirlpoolMeta.getInstance(context).whirlpoolPremixAccount
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }
                //                    Whirlpool Segwit post-mix
                5 -> {
                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.POSTMIX_RECEIVE else WALLET_INDEX.POSTMIX_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        WhirlpoolMeta.getInstance(context).whirlpoolPostmix
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }
                //                    Whirlpool Segwit post-mix BIP44 change address
                6 -> {
                    chain = 1
                    currentIndex =
                        AddressFactory.getInstance(context).getIndex(WALLET_INDEX.POSTMIX_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        WhirlpoolMeta.getInstance(context).whirlpoolPostmix
                    ).getChain(chain).getAddressAt(index)
                }

                7 -> {
                    chain = 1
                    currentIndex =
                        AddressFactory.getInstance(context).getIndex(WALLET_INDEX.POSTMIX_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        WhirlpoolMeta.getInstance(context).whirlpoolPostmix
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }

                8 -> {
                    currentIndex = AddressFactory.getInstance(context)
                        .getIndex(if (chain == 0) WALLET_INDEX.BADBANK_RECEIVE else WALLET_INDEX.BADBANK_CHANGE)
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        WhirlpoolMeta.getInstance(context).whirlpoolBadBank
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }

                9 -> {
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        SwapsMeta.getInstance(context).getSwapsMainAccount()
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }

                10 -> {
                    hdAddress = BIP84Util.getInstance(context).wallet.getAccount(
                        SwapsMeta.getInstance(context).getSwapsRefundAccount()
                    ).getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }

                11 -> {
                    hdAddress =
                        BIP84Util.getInstance(context).wallet.getAccount(SwapsMeta.getInstance(context).swapsAsbMainAccount)
                            .getChain(chain).getAddressAt(index)
                    segwitAddress = SegwitAddress(
                        hdAddress.ecKey,
                        SamouraiWallet.getInstance().currentNetworkParams
                    )
                }
            }

            val pairAddress = Pair(hdAddress, segwitAddress)
            return Pair(currentIndex, pairAddress)
        }
    }
}