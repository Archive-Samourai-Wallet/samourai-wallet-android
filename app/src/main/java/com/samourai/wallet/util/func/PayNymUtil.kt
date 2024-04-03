package com.samourai.wallet.util.func

import android.content.Context
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.util.CharSequenceX

fun synPayNym(pcode : String?, context : Context) {
    val payment_code = PaymentCode(pcode)
    var idx = 0
    var loop = true
    val addrs = ArrayList<String>()
    while (loop) {
        addrs.clear()
        for (i in idx until (idx + 20)) {
            //                            Log.i("PayNymDetailsActivity", "sync receive from " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceivePubKey(payment_code, i));
            BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(context)
                .getReceivePubKey(payment_code, i)] = i
            BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(context)
                .getReceivePubKey(payment_code, i)] = payment_code.toString()
            addrs.add(BIP47Util.getInstance(context).getReceivePubKey(payment_code, i))
            //                            Log.i("PayNymDetailsActivity", "p2pkh " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceiveAddress(payment_code, i).getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
        }
        val s = addrs.toTypedArray()
        val nb = APIFactory.getInstance(context).syncBIP47Incoming(s)
        //                        Log.i("PayNymDetailsActivity", "sync receive idx:" + idx + ", nb == " + nb);
        if (nb == 0) {
            loop = false
        }
        idx += 20
    }
    idx = 0
    loop = true
    BIP47Meta.getInstance().setOutgoingIdx(pcode, 0)
    while (loop) {
        addrs.clear()
        for (i in idx until (idx + 20)) {
            val sendAddress = BIP47Util.getInstance(context).getSendAddress(payment_code, i)
            //                            Log.i("PayNymDetailsActivity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
            //                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
            BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(context)
                .getSendPubKey(payment_code, i)] = i
            BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(context)
                .getSendPubKey(payment_code, i)] = payment_code.toString()
            addrs.add(BIP47Util.getInstance(context).getSendPubKey(payment_code, i))
        }
        val s = addrs.toTypedArray()
        val nb = APIFactory.getInstance(context).syncBIP47Outgoing(s)
        //                        Log.i("PayNymDetailsActivity", "sync send idx:" + idx + ", nb == " + nb);
        if (nb == 0) {
            loop = false
        }
        idx += 20
    }
    BIP47Meta.getInstance().pruneIncoming()
    PayloadUtil.getInstance(context).saveWalletToJSON(
        CharSequenceX(
            AccessFactory.getInstance(context).guid + AccessFactory.getInstance(context).pin
        )
    )
}