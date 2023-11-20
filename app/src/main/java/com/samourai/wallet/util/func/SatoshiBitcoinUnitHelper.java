package com.samourai.wallet.util.func;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SatoshiBitcoinUnitHelper {

    private static final char NUMBER_DIGIT_GROUPING_SEP = ' ';
    public static final double BTC_IN_SATOSHI = 100_000_000d;

    public static final int MAX_POSSIBLE_BTC = 21_000_000;
    public static final long MAX_POSSIBLE_SAT = MAX_POSSIBLE_BTC * (long)BTC_IN_SATOSHI;

    private SatoshiBitcoinUnitHelper() {}

    public static Long getSatValue(final Number btc) {
        return Math.round(btc.doubleValue() * BTC_IN_SATOSHI);
    }

    public static Long getSatValue(final double btc) {
        return Math.round(btc * BTC_IN_SATOSHI);
    }

    public static double getBtcValue(final Number sats) {
        return sats.longValue() / BTC_IN_SATOSHI;
    }

    public static double getBtcValue(final long sats) {
        return sats / BTC_IN_SATOSHI;
    }

    public static DecimalFormat createDecimalFormat(
            final String pattern,
            final boolean grouping) {

        final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        if (grouping) {
            symbols.setGroupingSeparator(NUMBER_DIGIT_GROUPING_SEP);
        }

        final DecimalFormat decimalFormat = new DecimalFormat(pattern);
        decimalFormat.setDecimalFormatSymbols(symbols);
        decimalFormat.setMinimumIntegerDigits(1);
        decimalFormat.setMaximumFractionDigits(8);
        return decimalFormat;
    }
}
