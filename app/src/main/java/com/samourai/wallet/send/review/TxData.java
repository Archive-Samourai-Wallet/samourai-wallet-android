package com.samourai.wallet.send.review;

import static com.samourai.wallet.util.func.TransactionOutPointHelper.toTxOutPoints;
import static com.samourai.wallet.util.func.TransactionOutPointHelper.toUtxos;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

import android.content.Context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.ricochet.RicochetTransactionInfo;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.utxos.UTXOUtil;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TxData {

    private int idxBIP44Internal = 0;
    private int idxBIP49Internal = 0;
    private int idxBIP84Internal = 0;
    private int idxBIP84PostMixInternal = 0;

    private final List<UTXO> selectedUTXO = Lists.newArrayList();
    private final Map<String, BigInteger> receivers = Maps.newLinkedHashMap();
    private long change = 0L;
    private RicochetTransactionInfo ricochetTransactionInfo;

    private TxData() {}

    public static TxData create(final Context context) {
        final TxData txData = new TxData();
        final AddressFactory addressFactory = AddressFactory.getInstance(context);
        txData.idxBIP84PostMixInternal = addressFactory.getIndex(WALLET_INDEX.POSTMIX_CHANGE);
        txData.idxBIP84Internal = addressFactory.getIndex(WALLET_INDEX.BIP84_CHANGE);
        txData.idxBIP49Internal = addressFactory.getIndex(WALLET_INDEX.BIP49_CHANGE);
        txData.idxBIP44Internal = addressFactory.getIndex(WALLET_INDEX.BIP44_CHANGE);
        return txData;
    }

    public static TxData copy(final TxData source) {
        final TxData txData = new TxData();
        txData.idxBIP84PostMixInternal = source.idxBIP84PostMixInternal;
        txData.idxBIP84Internal = source.idxBIP84Internal;
        txData.idxBIP49Internal = source.idxBIP49Internal;
        txData.idxBIP44Internal = source.idxBIP44Internal;
        txData.selectedUTXO.addAll(source.selectedUTXO);
        txData.receivers.putAll(source.receivers);
        txData.change = source.change;
        txData.ricochetTransactionInfo = source.ricochetTransactionInfo;
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
        if (nonNull(ricochetTransactionInfo)) {
            return ImmutableList.copyOf(toUtxos(ricochetTransactionInfo.getSelectedUTXOPoints()));
        }
        return ImmutableList.copyOf(selectedUTXO);
    }

    public TxData addSelectedUTXO(final Collection<UTXO> utxos) {
        selectedUTXO.addAll(utxos);
        return this;
    }

    public TxData addSelectedUTXO(final UTXO utxo) {
        selectedUTXO.add(utxo);
        return this;
    }

    public List<MyTransactionOutPoint> getSelectedUTXOPoints() {
        if (nonNull(ricochetTransactionInfo)) {
            return ricochetTransactionInfo.getSelectedUTXOPoints();
        }
        return toTxOutPoints(selectedUTXO);
    }

    public Set<String> getSelectedUTXOPointAddresses() {
        final Set<String> addresses = Sets.newHashSet();
         for (final MyTransactionOutPoint outPoint : getSelectedUTXOPoints()) {
             addresses.add(txOutPointId(outPoint));
         }
        return addresses;
    }

    public static String txOutPointId(final MyTransactionOutPoint outPoint) {
        return outPoint.getTxHash() + "_" + outPoint.getTxOutputN();
    }

    public long getTotalAmountInTxInput() {
        long amount = 0L;
        for (final MyTransactionOutPoint outPoint : getSelectedUTXOPoints()) {
            amount += outPoint.getValue().value;
        }
        return amount;
    }

    public static String getNoteOrAddress(final MyTransactionOutPoint transactionOutPoint) {
        return defaultIfBlank(
                UTXOUtil.getInstance().getNote(transactionOutPoint.getTxHash().toString()),
                transactionOutPoint.getAddress());
    }

    public static boolean hasNote(final MyTransactionOutPoint transactionOutPoint) {
        return isNotBlank(UTXOUtil.getInstance().getNote(transactionOutPoint.getTxHash().toString()));
    }

    public Map<String, BigInteger> getReceivers() {
        return receivers;
    }

    public long getAggregatedReceiversAmount() {
        long amount = 0L;
        for (final BigInteger amountForReceiver : receivers.values()) {
            amount += amountForReceiver.longValue();
        }
        return amount;
    }

    public RicochetTransactionInfo getRicochetTransactionInfo() {
        return ricochetTransactionInfo;
    }

    public void setRicochetTransactionInfo(final RicochetTransactionInfo ricochetTransactionInfo) {
        this.ricochetTransactionInfo = ricochetTransactionInfo;
    }

    public long getChange() {
        if (nonNull(ricochetTransactionInfo)) {
            return ricochetTransactionInfo.getChange();
        }
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
