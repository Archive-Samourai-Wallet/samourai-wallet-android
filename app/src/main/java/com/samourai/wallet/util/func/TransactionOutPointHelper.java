package com.samourai.wallet.util.func;

import static java.util.Objects.nonNull;

import com.google.common.collect.Lists;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;

public class TransactionOutPointHelper {
    private TransactionOutPointHelper() {}

    public static List<MyTransactionOutPoint> toTxOutPoints(final Collection<UTXO> utxos) {
        final List<MyTransactionOutPoint> myTransactionOutPoints = Lists.newArrayList();
        for (final UTXO utxo : CollectionUtils.emptyIfNull(utxos)) {
            myTransactionOutPoints.addAll(utxo.getOutpoints());
        }
        return myTransactionOutPoints;
    }

    public static List<UTXO> toUtxoPoints(final Collection<UTXO> utxos) {
        return toUtxos(toTxOutPoints(utxos));
    }

    public static UTXO toUtxo(final MyTransactionOutPoint transactionOutPoint) {
        final UTXO utxo = new UTXO();
        utxo.setOutpoints(Lists.newArrayList(transactionOutPoint));
        return utxo;
    }

    public static List<UTXO> toUtxos(final Collection<MyTransactionOutPoint> transactionOutPoints) {
        final List<UTXO> utxoList = Lists.newArrayList();
        for (final MyTransactionOutPoint outpoint : CollectionUtils.emptyIfNull(transactionOutPoints)) {
            utxoList.add(toUtxo(outpoint));
        }
        return utxoList;
    }

    public static long retrievesAggregatedAmount(final Collection<MyTransactionOutPoint> points) {
        long amount = 0L;
        for (final MyTransactionOutPoint point : CollectionUtils.emptyIfNull(points)) {
            if (nonNull(point.getValue())) {
                amount += point.getValue().value;
            }
        }
        return amount;
    }
}
