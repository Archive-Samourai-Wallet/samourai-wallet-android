package com.samourai.wallet.util.tech;

public class NumberHelper {

    private NumberHelper() {}

    public static int numberOfTrailingZeros(final long number) {
        if (number == 0) return 0;

        int trailingZeros = 0;
        long mod = 10L;
        while (number%mod == 0) {
            trailingZeros++;
            mod *= 10;
        }
        return trailingZeros;
    }

}
