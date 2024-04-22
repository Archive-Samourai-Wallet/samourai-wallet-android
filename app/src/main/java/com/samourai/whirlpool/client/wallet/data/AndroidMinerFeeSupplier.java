package com.samourai.whirlpool.client.wallet.data;

import static java.lang.Math.min;
import static java.util.Objects.nonNull;

import android.util.Log;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.fee.EnumFeeRate;
import com.samourai.wallet.api.fee.RawFees;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class AndroidMinerFeeSupplier implements MinerFeeSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidMinerFeeSupplier.class);
    private static AndroidMinerFeeSupplier instance;
    private FeeUtil feeUtil;

    public static AndroidMinerFeeSupplier getInstance() {
        if (instance == null) {
            instance = new AndroidMinerFeeSupplier(FeeUtil.getInstance());
        }
        return instance;
    }

    private AndroidMinerFeeSupplier(FeeUtil feeUtil) {
        this.feeUtil = feeUtil;
    }

    @Override
    public int getFee(MinerFeeTarget feeTarget) {
        feeUtil.normalize();
        switch (feeUtil.getFeeRepresentation()) {
            case NEXT_BLOCK_RATE:
                return getNextBlockFeeRate(feeTarget);
            case BLOCK_COUNT:
            default:
                return getBlockCountFeeRate(feeTarget);
        }
    }

    private int getNextBlockFeeRate(final MinerFeeTarget feeTarget) {

        final RawFees rawFees = feeUtil.getRawFees();
        BigInteger feePerKB = feeUtil.getNormalFee().getDefaultPerKB();

        switch (feeTarget) {
            case BLOCKS_2: case BLOCKS_4:
                feePerKB = feeUtil.getHighFee().getDefaultPerKB();
                break;

            case BLOCKS_6:
                feePerKB = feeUtil.getNormalFee().getDefaultPerKB();
                break;

            case BLOCKS_12: case BLOCKS_24: {
                final Integer fee100 = rawFees.getFee(EnumFeeRate.RATE_100);
                if (nonNull(fee100)) return fee100;
                final Integer fee990 = rawFees.getFee(EnumFeeRate.RATE_990);
                if (nonNull(fee990)) {
                    return min(fee990 / 2, feeUtil.getNormalFee().getDefaultPerKB().intValue() / 1000);
                } else {
                    feePerKB = feeUtil.getLowFee().getDefaultPerKB();
                    Log.e(
                            AndroidMinerFeeSupplier.class.getSimpleName(),
                            "inconsistent state : " + EnumFeeRate.RATE_990.getRateAsString() + " is null");
                }
                break;
            }

            default:
                log.error("unknown MinerFeeTarget: "+feeTarget);
                break;
        }
        long feePerB = feePerKB.longValue() / 1000L;
        return (int)feePerB;
    }

    private int getBlockCountFeeRate(final MinerFeeTarget feeTarget) {

        BigInteger feePerKB = feeUtil.getNormalFee().getDefaultPerKB();
        switch (feeTarget) {
            case BLOCKS_2: case BLOCKS_4:
                feePerKB = feeUtil.getHighFee().getDefaultPerKB();
                break;

            case BLOCKS_6:
                feePerKB = feeUtil.getNormalFee().getDefaultPerKB();
                break;

            case BLOCKS_12: case BLOCKS_24:
                feePerKB = feeUtil.getLowFee().getDefaultPerKB();
                break;

            default:
                log.error("unknown MinerFeeTarget: "+feeTarget);
                break;
        }
        long feePerB = feePerKB.longValue() / 1000L;
        return (int)feePerB;
    }
}
