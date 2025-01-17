package com.samourai.wallet.send.review.ref;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.Maps;
import com.samourai.wallet.api.fee.EnumFeeBlockCount;
import com.samourai.wallet.api.fee.EnumFeeRate;
import com.samourai.wallet.api.fee.EnumFeeRepresentation;

import java.util.Map;

public enum EnumTransactionPriority {
    VERY_LOW(EnumFeeRate.RATE_100, null,
            "Low", "Low",
            "10% probability of next block", "24 blocks"),

    LOW(EnumFeeRate.RATE_200, EnumFeeBlockCount.BLOCK_24,
            "Low", "Low",
            "20% probability of next block", "24 blocks"),

    LOW_NORMAL(null, EnumFeeBlockCount.BLOCK_12,
            "Low", "Low",
            "20% probability of next block", "12 blocks"),

    NORMAL(EnumFeeRate.RATE_500, EnumFeeBlockCount.BLOCK_06,
            "Normal", "Normal",
            "50% probability of next block", "6 blocks"),

    NORMAL_HIGH(EnumFeeRate.RATE_900, EnumFeeBlockCount.BLOCK_04,
            "Normal", "Normal",
            "90% probability of next block", "4 blocks"),

    NEXT_BLOCK(EnumFeeRate.RATE_990, EnumFeeBlockCount.BLOCK_02,
            "Next Block", "High",
            "99% probability of next block", "2 blocks"),

    NEXT_BLOCK_FORCE(EnumFeeRate.RATE_999, null,
            "Next Block", "High",
            "99.9% probability of next block", "2 blocks"),
    ;

    private static final Map<String, EnumTransactionPriority> CACHE_NEXT_BLOCK = createCacheNextBlock();
    private static final Map<String, EnumTransactionPriority> CACHE_NB_BLOCK = createCacheNbBlock();

    private final EnumFeeRate nextBlockIdentifier;
    private final EnumFeeBlockCount nbBlockIdentifier;

    private final String nextBlockCaption;
    private final String nbBlockCaption;
    private final String nextBlockDescription;
    private final String nbBlockDescription;

    EnumTransactionPriority(
            final EnumFeeRate nextBlockIdentifier,
            final EnumFeeBlockCount nbBlockIdentifier,
            final String nextBlockCaption,
            final String nbBlockCaption,
            final String nextBlockDescription,
            final String nbBlockDescription) {

        this.nextBlockIdentifier = nextBlockIdentifier;
        this.nbBlockIdentifier = nbBlockIdentifier;
        this.nextBlockCaption = nextBlockCaption;
        this.nbBlockCaption = nbBlockCaption;
        this.nextBlockDescription = nextBlockDescription;
        this.nbBlockDescription = nbBlockDescription;
    }

    public String getIdentifier(final EnumFeeRepresentation feeRepresentation) {

        if (isNull(feeRepresentation)) return nextBlockIdentifier.getRateAsString();

        switch (feeRepresentation) {
            case NEXT_BLOCK_RATE:
                return nextBlockIdentifier.getRateAsString();
            case BLOCK_COUNT:
                return nbBlockIdentifier.getBlockCount();
        }
        return nextBlockIdentifier.getRateAsString();
    }

    public String getCaption(final EnumFeeRepresentation feeRepresentation) {

        if (isNull(feeRepresentation)) return nextBlockCaption;

        switch (feeRepresentation) {
            case NEXT_BLOCK_RATE:
                return nextBlockCaption;
            case BLOCK_COUNT:
                return nbBlockCaption;
        }
        return nextBlockCaption;
    }

    public String getDescription(final EnumFeeRepresentation feeRepresentation) {

        if (isNull(feeRepresentation)) return nextBlockDescription;

        switch (feeRepresentation) {
            case NEXT_BLOCK_RATE:
                return nextBlockDescription;
            case BLOCK_COUNT:
                return nbBlockDescription;
        }
        return nextBlockDescription;
    }

    public String getDescription(
            final EnumFeeRepresentation feeRepresentation,
            final long minerFeeRate,
            final long lowFeeRate,
            final long normalFeeRate,
            final long highFeeRate
    ) {

        if (isNull(feeRepresentation)) return nextBlockDescription;

        switch (feeRepresentation) {
            case NEXT_BLOCK_RATE:
                switch (this) {
                    case VERY_LOW:
                        if (minerFeeRate < lowFeeRate) return "<" + nextBlockDescription;
                        return nextBlockDescription;
                    case NORMAL:
                        if (minerFeeRate > lowFeeRate && minerFeeRate < normalFeeRate) return "<" + nextBlockDescription;
                        if (minerFeeRate < highFeeRate && minerFeeRate > normalFeeRate) return ">" + nextBlockDescription;
                        return nextBlockDescription;
                    case NEXT_BLOCK:
                        if (minerFeeRate > highFeeRate) return ">" + nextBlockDescription;
                        return nextBlockDescription;
                    default:
                        return nextBlockDescription;
                }
            case BLOCK_COUNT:
                return nbBlockDescription;
        }
        return nextBlockDescription;
    }

    public static EnumTransactionPriority fromIdentifier(
            final String identifier,
            final EnumFeeRepresentation feeRepresentation) {

        if (isNull(feeRepresentation)) return null;

        switch (feeRepresentation) {
            case NEXT_BLOCK_RATE:
                return CACHE_NEXT_BLOCK.get(identifier);
            case BLOCK_COUNT:
                return CACHE_NB_BLOCK.get(identifier);
        }
        return null;
    }

    private static Map<String, EnumTransactionPriority> createCacheNextBlock() {
        final Map<String, EnumTransactionPriority> cache = Maps.newHashMap();
        for (final EnumTransactionPriority priority : EnumTransactionPriority.values()) {
            if (nonNull(priority.nextBlockIdentifier)) {
                cache.put(priority.nextBlockIdentifier.getRateAsString(), priority);
            }
        }
        return cache;
    }

    private static Map<String, EnumTransactionPriority> createCacheNbBlock() {
        final Map<String, EnumTransactionPriority> cache = Maps.newHashMap();
        for (final EnumTransactionPriority priority : EnumTransactionPriority.values()) {
            if (nonNull(priority.nbBlockIdentifier)) {
                cache.put(priority.nbBlockIdentifier.getBlockCount(), priority);
            }
        }
        return cache;
    }
}
