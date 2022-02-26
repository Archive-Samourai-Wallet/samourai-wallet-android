package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;

public class MockAndroidMinerFeeSupplier implements MinerFeeSupplier {
    @Override
    public int getFee(MinerFeeTarget feeTarget) {
        switch (feeTarget) {
            case BLOCKS_24:
                return 2;
            case BLOCKS_12:
                return 4;
            case BLOCKS_6:
                return 6;
            case BLOCKS_4:
                return 8;
            case BLOCKS_2:
                return 10;
        }
        return Integer.parseInt(feeTarget.getValue());
    }
}
