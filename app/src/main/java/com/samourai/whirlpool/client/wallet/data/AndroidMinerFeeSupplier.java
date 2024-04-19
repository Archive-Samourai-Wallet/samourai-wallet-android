package com.samourai.whirlpool.client.wallet.data;

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
            case BLOCKS_2: case BLOCKS_4: {
                final Integer fee900 = rawFees.getFee(EnumFeeRate.RATE_900);
                if (nonNull(fee900)) return fee900;
                final Integer fee990 = rawFees.getFee(EnumFeeRate.RATE_990);
                if (nonNull(fee990)) {
                    return Math.max(fee990 / 2, feeUtil.getNormalFee().getDefaultPerKB().intValue() / 1000);
                } else {
                    Log.e(
                            AndroidMinerFeeSupplier.class.getSimpleName(),
                            "inconsistent state : " + EnumFeeRate.RATE_990.getRateAsString() + " is null");
                }
                break;
            }

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
