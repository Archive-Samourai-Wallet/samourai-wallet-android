package com.samourai.wallet.util.func;

import static java.util.Objects.isNull;

import com.samourai.wallet.send.MyTransactionOutPoint;

import java.util.Comparator;

/**
 * DESC order based on Coin#value
 */
public class MyTransactionOutPointAmountComparator implements Comparator<MyTransactionOutPoint> {
    @Override
    public int compare(final MyTransactionOutPoint o1, final MyTransactionOutPoint o2) {
        if (o2 == o1) return 0;
        if (isNull(o2)) return -1;
        if (isNull(o1)) return 1;
        if (o2.getValue() == o1.getValue()) return 0;
        if (isNull(o2.getValue())) return -1;
        if (isNull(o1.getValue())) return 1;
        return Long.compare(o2.getValue().getValue(), o1.getValue().getValue());
    }
}

