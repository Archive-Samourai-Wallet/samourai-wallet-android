package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class AndroidMinerFeeSupplier implements MinerFeeSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidMinerFeeSupplier.class);
    private FeeUtil feeUtil;

    public AndroidMinerFeeSupplier(FeeUtil feeUtil) {
        this.feeUtil = feeUtil;
    }

    @Override
    public int getFee(MinerFeeTarget feeTarget) {
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
