package com.samourai.wallet.util;

import com.samourai.wallet.util.func.SatoshiBitcoinUnitHelper;

import org.junit.Assert;
import org.junit.Test;

public class SatoshiBitcoinUnitHelperTest {

    @Test
    public void should_convert_satoshi_to_bitcoin() {
        Assert.assertEquals(0.00000125d, SatoshiBitcoinUnitHelper.getBtcValue(125l), 0.0000000001);
    }

    @Test
    public void should_convert_bitcoin_to_satoshi() {
        Assert.assertEquals(450_000l, SatoshiBitcoinUnitHelper.getSatValue(0.0045), 0.0000000001);
    }

    @Test
    public void should_return_max_possible_satoshi() {
        Assert.assertEquals(2.1e15, SatoshiBitcoinUnitHelper.MAX_POSSIBLE_SAT, 0.0000000001);
    }
}
