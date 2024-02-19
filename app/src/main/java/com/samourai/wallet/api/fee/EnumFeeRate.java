package com.samourai.wallet.api.fee;

import com.google.common.collect.Maps;

import java.util.Map;

public enum EnumFeeRate {

    RATE_999("0.999", false),
    RATE_990("0.99", true),
    RATE_900("0.9", false),
    RATE_500("0.5", true),
    RATE_200("0.2", true),
    RATE_100("0.1", false),
    ;

    private static final Map<String, EnumFeeRate> CACHE = createCache();

    private final String rateAsString;
    private final boolean main;

    EnumFeeRate(final String rateAsString, boolean main) {
        this.rateAsString = rateAsString;
        this.main = main;
    }

    public String getRateAsString() {
        return rateAsString;
    }

    public boolean isMain() {
        return main;
    }

    public static EnumFeeRate fromFeeRate(final String feeRate) {
        return CACHE.get(feeRate);
    }

    private static Map<String, EnumFeeRate> createCache() {
        final Map<String, EnumFeeRate> cache = Maps.newHashMap();
        for (final EnumFeeRate enumFeeRate : EnumFeeRate.values()) {
            cache.put(enumFeeRate.getRateAsString(), enumFeeRate);
        }
        return cache;
    }
}
