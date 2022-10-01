package com.samourai.wallet.service

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.ricochet.RicochetMeta
import com.samourai.wallet.segwit.BIP49Util
import com.samourai.wallet.segwit.BIP84Util
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.CharSequenceX
import com.samourai.wallet.util.LogUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.wallet.whirlpool.WhirlpoolMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException
import org.json.JSONException
import java.io.IOException

class WalletRefreshWorker(private val context: Context, private val parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {

        val launch: Boolean = parameters.inputData.getBoolean(LAUNCHED, false)
        val notifTx: Boolean = parameters.inputData.getBoolean(NOTIF_TX, false)

        AppUtil.getInstance(applicationContext).setWalletLoading(true)
        APIFactory.getInstance(context).stayingAlive()
        APIFactory.getInstance(context).initWallet()

        val _intentDisplay = Intent("com.samourai.wallet.BalanceFragment.DISPLAY")
        LocalBroadcastManager.getInstance(context).sendBroadcast(_intentDisplay)

        PrefsUtil.getInstance(context).setValue(PrefsUtil.FIRST_RUN, false)

        if (notifTx && !AppUtil.getInstance(context).isOfflineMode) {
            //
            // check for incoming payment code notification tx
            //
            try {
                val pcode = BIP47Util.getInstance(context).paymentCode
                //                    Log.i("BalanceFragment", "payment code:" + pcode.toString());
//                    Log.i("BalanceFragment", "notification address:" + pcode.notificationAddress().getAddressString());
                APIFactory.getInstance(context).getNotifAddress(pcode.notificationAddress(SamouraiWallet.getInstance().currentNetworkParams).addressString)
            } catch (afe: AddressFormatException) {
                afe.printStackTrace()
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            //
            // check on outgoing payment code notification tx
            //
            val outgoingUnconfirmed: List<Pair<String?, String?>> = BIP47Meta.getInstance().outgoingUnconfirmed
            //                Log.i("BalanceFragment", "outgoingUnconfirmed:" + outgoingUnconfirmed.size());
            for (pair in outgoingUnconfirmed) {
//                    Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
//                    Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
                val confirmations = APIFactory.getInstance(context).getNotifTxConfirmations(pair.right)
                if (confirmations > 0) {
                    BIP47Meta.getInstance().setOutgoingStatus(pair.left, BIP47Meta.STATUS_SENT_CFM)
                }
                if (confirmations == -1) {
                    BIP47Meta.getInstance().setOutgoingStatus(pair.left, BIP47Meta.STATUS_NOT_SENT)
                }
            }
            val _intent = Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE")
            LocalBroadcastManager.getInstance(context).sendBroadcast(_intent)
        }

        if (launch) {
            if (PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID_V, 0) < 4) {
                Log.i(TAG, "guid_v < 4")
                try {
                    val _guid = AccessFactory.getInstance(context).createGUID()
                    val _hash = AccessFactory.getInstance(context).getHash(_guid, CharSequenceX(AccessFactory.getInstance(context).pin), AESUtil.DefaultPBKDF2Iterations)
                    PayloadUtil.getInstance(context).saveWalletToJSON(CharSequenceX(_guid + AccessFactory.getInstance().pin))
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ACCESS_HASH, _hash)
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ACCESS_HASH2, _hash)
                    Log.i(TAG, "guid_v == 4")
                } catch (e: MnemonicLengthException) {
                } catch (e: IOException) {
                } catch (e: JSONException) {
                } catch (e: DecryptionException) {
                }
            }
            if (!PrefsUtil.getInstance(context.applicationContext).getValue(PrefsUtil.XPUB44LOCK, false)) {
                val s = HD_WalletFactory.getInstance(context).get().xpuBs
                APIFactory.getInstance(context).lockXPUB(s[0], 44, null)
            }
            try {
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB49LOCK, false)) {
                    val ypub = BIP49Util.getInstance(context).wallet.getAccount(0).ypubstr()
                    APIFactory.getInstance(context).lockXPUB(ypub, 49, null)
                }
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB84LOCK, false)) {
                    val zpub = BIP84Util.getInstance(context).wallet.getAccount(0).zpubstr()
                    APIFactory.getInstance(context).lockXPUB(zpub, 84, null)
                }
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPRELOCK, false)) {
                    val zpub = BIP84Util.getInstance(context).wallet.getAccount(WhirlpoolMeta.getInstance(context).whirlpoolPremixAccount).zpubstr()
                    APIFactory.getInstance(context).lockXPUB(zpub, 84, PrefsUtil.XPUBPRELOCK)
                }
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPOSTLOCK, false)) {
                    val zpub = BIP84Util.getInstance(context).wallet.getAccount(WhirlpoolMeta.getInstance(context).whirlpoolPostmix).zpubstr()
                    APIFactory.getInstance(context).lockXPUB(zpub, 84, PrefsUtil.XPUBPOSTLOCK)
                }
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBBADBANKLOCK, false)) {
                    val zpub = BIP84Util.getInstance(context).wallet.getAccount(WhirlpoolMeta.getInstance(context).whirlpoolBadBank).zpubstr()
                    APIFactory.getInstance(context).lockXPUB(zpub, 84, PrefsUtil.XPUBBADBANKLOCK)
                }
                if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBRICOCHETLOCK, false)) {
                    val zpub = BIP84Util.getInstance(context).wallet.getAccount(RicochetMeta.getInstance(context).ricochetAccount).zpubstr()
                    APIFactory.getInstance(context).lockXPUB(zpub, 84, PrefsUtil.XPUBRICOCHETLOCK)
                }
            } catch (e: Exception) {
                LogUtil.error(TAG, e.message)
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
            try {
                val prevIdx = RicochetMeta.getInstance(context).index
                APIFactory.getInstance(context).parseRicochetXPUB()
                if (prevIdx > RicochetMeta.getInstance(context).index) {
                    RicochetMeta.getInstance(context).index = prevIdx
                }
            } catch (je: JSONException) {
            }
        }
        withContext(Dispatchers.IO){
            try {
                PayloadUtil.getInstance(context).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(context).guid + AccessFactory.getInstance(context).pin))
            } catch (ignored: Exception) {
            }
        }
        withContext(Dispatchers.Main) {
            val _intent = Intent("com.samourai.wallet.BalanceFragment.DISPLAY")
            LocalBroadcastManager.getInstance(context).sendBroadcast(_intent)
        }

        AppUtil.getInstance(applicationContext).setWalletLoading(false)
        val data = workDataOf();
        return Result.success(data)
    }

    companion object {
        const val LAUNCHED = "LAUNCHED"
        const val NOTIF_TX = "NOTIF_TX"
        private const val TAG = "WalletRefreshWorker"

        fun enqueue(context: Context, notifTx: Boolean = false, launched: Boolean = false): Operation {
            val workManager = WorkManager.getInstance(context)
            val workRequest = OneTimeWorkRequestBuilder<WalletRefreshWorker>().apply {
                setInputData(
                    workDataOf(
                        LAUNCHED to launched,
                        NOTIF_TX to notifTx
                    )
                )
            }.build()
            return workManager.enqueue(workRequest)
        }
    }


}