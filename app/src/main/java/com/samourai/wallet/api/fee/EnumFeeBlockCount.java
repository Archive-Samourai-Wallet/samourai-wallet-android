package com.samourai.wallet.api.fee;

import com.google.common.collect.Maps;

import java.util.Map;

public enum EnumFeeBlockCount {

    BLOCK_02("2", true),
    BLOCK_04("4", false),
    BLOCK_06("6", true),
    BLOCK_12("12", false),
    BLOCK_24("24", true),
    ;

    private static final Map<String, EnumFeeBlockCount> CACHE = createCache();

    private final String blockCount;
    private final boolean main;

    EnumFeeBlockCount(final String blockCount, boolean main) {
        this.blockCount = blockCount;
        this.main = main;
    }

    public String getBlockCount() {
        return blockCount;
    }

    public boolean isMain() {
        return main;
    }

    public static EnumFeeBlockCount fromBlockCount(final String blockCount) {
        return CACHE.get(blockCount);
    }

    private static Map<String, EnumFeeBlockCount> createCache() {
        final Map<String, EnumFeeBlockCount> cache = Maps.newHashMap();
        for (final EnumFeeBlockCount enumFeeBlockCount : EnumFeeBlockCount.values()) {
            cache.put(enumFeeBlockCount.getBlockCount(), enumFeeBlockCount);
        }
        return cache;
    }
}