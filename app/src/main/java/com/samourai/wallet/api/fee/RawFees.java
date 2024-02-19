package com.samourai.wallet.api.fee;

import static com.samourai.wallet.api.fee.EnumFeeRepresentation.BLOCK_COUNT;
import static com.samourai.wallet.api.fee.EnumFeeRepresentation.NEXT_BLOCK_RATE;
import static java.lang.Math.min;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RawFees {

    private final EnumFeeRepresentation feeRepresentation;
    private final Map<String, Integer> feeByRepresentation = Maps.newLinkedHashMap();
    private final TreeMap<Integer, List<String>> representationByFee = Maps.newTreeMap();

    private RawFees(final List<Integer> fees) {
        final List<Integer> orderedFees = Ordering.natural().reverse().sortedCopy(fees);
        this.feeRepresentation = NEXT_BLOCK_RATE;
        final EnumFeeRate[] enumFeeRates = EnumFeeRate.values();
        if (! orderedFees.isEmpty()) {
            final int lastIndex = orderedFees.size() - 1;
            for (int i = 0; i < enumFeeRates.length; ++ i) {
                putFee(enumFeeRates[i].getRateAsString(), orderedFees.get(Math.min(i, lastIndex)));
            }
        }
    }

    private RawFees(final Map<String, Integer> feeByRepresentation,
                    final EnumFeeRepresentation feeRepresentation) {

        this.feeRepresentation = feeRepresentation;
        for (final Map.Entry<String, Integer> feeByRepEntry : feeByRepresentation.entrySet()) {
            putFee(feeByRepEntry.getKey(), feeByRepEntry.getValue());
        }
    }

    public static RawFees createFromMap(final Map<String, Integer> feeByRepresentation) {
        if (isNextBlockRateMap(feeByRepresentation)) {
            return new RawFees(feeByRepresentation, NEXT_BLOCK_RATE);
        } else {
            return new RawFees(feeByRepresentation, BLOCK_COUNT);
        }
    }

    private static boolean isNextBlockRateMap(final Map<String, Integer> feeMap) {
        if (MapUtils.isEmpty(feeMap)) return true;
        for (final String rep : feeMap.keySet()) {
            if (StringUtils.contains(rep, ".")) {
                return true;
            }
        }
        return false;
    }

    public static RawFees createFromList(final List<Integer> fees) {
        return new RawFees(fees);
    }

    public void putFee(final String rateAsString, final Integer fee) {
        feeByRepresentation.put(rateAsString, fee);
        List<String> reps = representationByFee.get(fee);
        if (java.util.Objects.isNull(reps)) {
            reps = Lists.newArrayList();
            representationByFee.put(fee, reps);
        }
        reps.add(rateAsString);
    }

    public Integer getFee(final EnumFeeRate rate) {
        return getFee(rate.getRateAsString());
    }

    public Integer getFee(final EnumFeeBlockCount blockCount) {
        return getFee(blockCount.getBlockCount());
    }

    public Integer getFee(final String representation) {
        return feeByRepresentation.get(representation);
    }

    public EnumFeeRepresentation getFeeRepresentation() {
        return feeRepresentation;
    }

    public boolean hasFee() {
        return ! feeByRepresentation.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawFees fees = (RawFees) o;
        return feeRepresentation == fees.feeRepresentation && Objects.equal(feeByRepresentation, fees.feeByRepresentation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(feeRepresentation, feeByRepresentation);
    }

    @Override
    public String toString() {
        return "Fees{" +
                "feeRepresentation=" + feeRepresentation +
                ", feeByRepresentation=" + feeByRepresentation +
                '}';
    }

    public String retrievesNearRepresentation(final int fee) {
        final Map.Entry<Integer, List<String>> ceilingEntry = representationByFee.floorEntry(fee);
        if (java.util.Objects.nonNull(ceilingEntry)) {
            return getBestRepresentation(ceilingEntry);
        } else {
            return getBestRepresentation(representationByFee.ceilingEntry(fee));
        }
    }

    private String getBestRepresentation(final Map.Entry<Integer, List<String>> feeToReps) {

        if (java.util.Objects.isNull(feeToReps)) return null;

        final List<String> reps = feeToReps.getValue();
        if (CollectionUtils.isEmpty(reps)) return null;
        if (reps.size() > 1) {
            for (final String rep : reps) {
                switch(feeRepresentation) {
                    case NEXT_BLOCK_RATE:
                        if (EnumFeeRate.fromFeeRate(rep).isMain()) return rep;
                        break;
                    case BLOCK_COUNT:
                        if (EnumFeeBlockCount.fromBlockCount(rep).isMain()) return rep;
                        break;
                }
            }
        }
        return reps.get(0);
    }
}
