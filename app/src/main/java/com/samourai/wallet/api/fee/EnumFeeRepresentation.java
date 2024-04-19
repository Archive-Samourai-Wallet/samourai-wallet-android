package com.samourai.wallet.api.fee;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.samourai.wallet.send.SuggestedFee;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public enum EnumFeeRepresentation {

    NEXT_BLOCK_RATE {
        @Override
        public List<SuggestedFee> createSuggestedFeeList(final RawFees rawFees) {

            final List<SuggestedFee> suggestedFees = new ArrayList<>();
            if (isNull(rawFees)) return suggestedFees;

            final Integer feeForNextBlock = rawFees.getFee(EnumFeeRate.RATE_990);
            if (nonNull(feeForNextBlock)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeForNextBlock * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            final Integer feeForNextBlockAt50 = rawFees.getFee(EnumFeeRate.RATE_500);
            if (nonNull(feeForNextBlockAt50)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeForNextBlockAt50 * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            final Integer feeForNextBlockAt10 = nonNull(rawFees.getFee(EnumFeeRate.RATE_100))
                    ? rawFees.getFee(EnumFeeRate.RATE_100)
                    : feeForNextBlockAt50;

            if (nonNull(feeForNextBlockAt10)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeForNextBlockAt10 * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if (isNull(rawFees.getFee(EnumFeeRate.RATE_100))) {
                rawFees.putFee(EnumFeeRate.RATE_100.getRateAsString(), feeForNextBlockAt10);
            }

            return suggestedFees;
        }
    },
    BLOCK_COUNT {
        @Override
        public List<SuggestedFee> createSuggestedFeeList(final RawFees rawFees) {

            final List<SuggestedFee> suggestedFees = new ArrayList<>();
            if (isNull(rawFees)) return suggestedFees;

            final Integer feeFor2Blocks = rawFees.getFee(EnumFeeBlockCount.BLOCK_02);
            if (nonNull(feeFor2Blocks)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeFor2Blocks * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            final Integer feeFor6Blocks = rawFees.getFee(EnumFeeBlockCount.BLOCK_06);
            if (nonNull(feeFor6Blocks)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeFor6Blocks * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            final Integer feeFor24Blocks = rawFees.getFee(EnumFeeBlockCount.BLOCK_24);
            if (nonNull(feeFor24Blocks)) {
                final SuggestedFee suggestedFee = new SuggestedFee();
                suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeFor24Blocks * 1000L));
                suggestedFee.setStressed(false);
                suggestedFee.setOK(true);
                suggestedFees.add(suggestedFee);
            }

            if (! suggestedFees.isEmpty()) {
                if (isNull(feeFor2Blocks)) {
                    rawFees.putFee(EnumFeeBlockCount.BLOCK_02.getBlockCount(), suggestedFees.get(0).getDefaultPerKB().intValue() / 1000);
                }
                if (isNull(feeFor6Blocks)) {
                    rawFees.putFee(EnumFeeBlockCount.BLOCK_06.getBlockCount(), suggestedFees.get(0).getDefaultPerKB().intValue() / 1000);
                }
                if (isNull(feeFor24Blocks)) {
                    rawFees.putFee(EnumFeeBlockCount.BLOCK_24.getBlockCount(), suggestedFees.get(suggestedFees.size() - 1).getDefaultPerKB().intValue() / 1000);
                }
            }

            return suggestedFees;
        }
    },
    ;

    abstract public List<SuggestedFee> createSuggestedFeeList(final RawFees rawFees);

    public boolean is1DolFeeEstimator() {
        return this == NEXT_BLOCK_RATE;
    }

    public boolean isBitcoindFeeEstimator() {
        return this == BLOCK_COUNT;
    }
}