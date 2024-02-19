package com.samourai.wallet.api.fee;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Test;

public class RawFeesTest {

    @Test
    public void test_retrieves_near_representation_for_fee_rates() {
        final RawFees fees = RawFees.createFromMap(ImmutableMap.of(
                "0.999", 26,
                "0.99", 18,
                "0.9", 13,
                "0.5", 13,
                "0.2", 11,
                "0.1", 9));

        Assert.assertEquals("0.999", fees.retrievesNearRepresentation(27));
        Assert.assertEquals("0.999", fees.retrievesNearRepresentation(26));
        Assert.assertEquals("0.99", fees.retrievesNearRepresentation(25));
        Assert.assertEquals("0.5", fees.retrievesNearRepresentation(14));
        Assert.assertEquals("0.5", fees.retrievesNearRepresentation(13));
        Assert.assertEquals("0.2", fees.retrievesNearRepresentation(11));
        Assert.assertEquals("0.1", fees.retrievesNearRepresentation(10));
        Assert.assertEquals("0.1", fees.retrievesNearRepresentation(9));
        Assert.assertEquals("0.1", fees.retrievesNearRepresentation(8));
    }

    @Test
    public void test_retrieves_near_representation_for_fee_rates_v1() {
        final RawFees fees = RawFees.createFromList(ImmutableList.of(123, 112, 154, 62));

        Assert.assertEquals("0.999", fees.retrievesNearRepresentation(155));
        Assert.assertEquals("0.999", fees.retrievesNearRepresentation(154));
        Assert.assertEquals("0.99", fees.retrievesNearRepresentation(153));
        Assert.assertEquals("0.5", fees.retrievesNearRepresentation(63));
        Assert.assertEquals("0.5", fees.retrievesNearRepresentation(62));
        Assert.assertEquals("0.5", fees.retrievesNearRepresentation(61));
    }

    @Test
    public void test_retrieves_near_representation_for_nb_block() {
        final RawFees fees = RawFees.createFromMap(ImmutableMap.of(
                "2", 181,
                "4", 150,
                "6", 150,
                "12", 111,
                "24", 62));

        Assert.assertEquals("2", fees.retrievesNearRepresentation(182));
        Assert.assertEquals("2", fees.retrievesNearRepresentation(181));
        Assert.assertEquals("6", fees.retrievesNearRepresentation(180));
        Assert.assertEquals("6", fees.retrievesNearRepresentation(150));
        Assert.assertEquals("12", fees.retrievesNearRepresentation(149));
        Assert.assertEquals("24", fees.retrievesNearRepresentation(63));
        Assert.assertEquals("24", fees.retrievesNearRepresentation(62));
        Assert.assertEquals("24", fees.retrievesNearRepresentation(61));
    }
}
