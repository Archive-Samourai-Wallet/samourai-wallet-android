package com.samourai.wallet.send.review;

import android.content.Context;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.AddressFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class TxData {

    private int idxBIP44Internal = 0;
    private int idxBIP49Internal = 0;
    private int idxBIP84Internal = 0;
    private int idxBIP84PostMixInternal = 0;

    private final List<UTXO> selectedUTXO = Lists.newArrayList();
    private final Map<String, BigInteger> receivers = Maps.newHashMap();
    private long change = 0l;

    private TxData() {
    }

    public static TxData create(final Context context) {
        final TxData txData = new TxData();
        final AddressFactory addressFactory = AddressFactory.getInstance(context);
        txData.idxBIP84PostMixInternal = addressFactory.getIndex(WALLET_INDEX.POSTMIX_CHANGE);
        txData.idxBIP84Internal = addressFactory.getIndex(WALLET_INDEX.BIP84_CHANGE);
        txData.idxBIP49Internal = addressFactory.getIndex(WALLET_INDEX.BIP49_CHANGE);
        txData.idxBIP44Internal = addressFactory.getIndex(WALLET_INDEX.BIP44_CHANGE);
        return txData;
    }

    public void restoreChangeIndexes(final Context context) {
        AddressFactory.getInstance(context).setWalletIdx(
                WALLET_INDEX.POSTMIX_CHANGE,
                idxBIP84PostMixInternal,
                true);
        AddressFactory.getInstance(context).setWalletIdx(
                WALLET_INDEX.BIP84_CHANGE,
                idxBIP84Internal,
                true);
        AddressFactory.getInstance(context).setWalletIdx(
                WALLET_INDEX.BIP49_CHANGE,
                idxBIP49Internal,
                true);
        AddressFactory.getInstance(context).setWalletIdx(
                WALLET_INDEX.BIP44_CHANGE,
                idxBIP44Internal,
                true);
    }

    public List<UTXO> getSelectedUTXO() {
        return selectedUTXO;
    }

    public Map<String, BigInteger> getReceivers() {
        return receivers;
    }

    public long getChange() {
        return change;
    }

    public void setChange(long change) {
        this.change = change;
    }

    public int getIdxBIP44Internal() {
        return idxBIP44Internal;
    }

    public int getIdxBIP49Internal() {
        return idxBIP49Internal;
    }

    public int getIdxBIP84Internal() {
        return idxBIP84Internal;
    }

    public int getIdxBIP84PostMixInternal() {
        return idxBIP84PostMixInternal;
    }

    public void clear() {
        receivers.clear();
        selectedUTXO.clear();
    }
}
