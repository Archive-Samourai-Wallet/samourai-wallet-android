package com.samourai.wallet.send.cahoots;

import static com.google.common.collect.Lists.newArrayList;
import static com.samourai.wallet.send.cahoots.JoinbotHelper.isJoinbotPossibleWithCurrentUserUTXOs;

import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class JoinbotHelperTest {

    @Test
    public void should_return_false_when_balance_is_less_than_amount() {

        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(10l)),
                newArrayList(utxo(10l)),
                newArrayList()));
    }

    @Test
    public void should_return_false_when_all_groups_balance_is_less_than_amount() {

        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(10l)),
                newArrayList(utxo(999l)),
                newArrayList(utxo(900l))));
    }

    @Test
    public void should_return_false_when_there_are_only_one_utxo() {

        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(),
                newArrayList(),
                newArrayList(utxo(2099l))));
    }

    @Test
    public void should_return_false_when_there_are_not_enough_balance_after_to_consume_2_utxo_for_fees() {

        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(30l)),
                newArrayList(),
                newArrayList(utxo(999l), utxo(20l))));
    }

    @Test
    public void should_return_true_when_both_amount_and_fees_can_be_paid() {

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(30l)),
                newArrayList(),
                newArrayList(utxo(1000l), utxo(20l))));

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(30l)),
                newArrayList(utxo(5l)),
                newArrayList(utxo(1000l))));

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(30l)),
                newArrayList(utxo(1000l)),
                newArrayList(utxo(5l))));

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(30l), utxo(5l)),
                newArrayList(),
                newArrayList(utxo(400l), utxo(600l))));

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(35l)),
                newArrayList(),
                newArrayList(utxo(400l), utxo(600l))));
    }

    @Test
    public void test_tricky_case() {

        Assert.assertTrue(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(34l)),
                newArrayList(utxo(500l), utxo(450l), utxo(50l)),
                newArrayList(utxo(5l))));

        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(34l)),
                newArrayList(utxo(500l), utxo(450l), utxo(50l)),
                newArrayList(utxo(55l))));
    }

    @Test
    public void should_return_false_when_only_1_satoshi_is_missing_in_the_smallest_utxo() {
        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(34l)),
                newArrayList(),
                newArrayList(utxo(400l), utxo(600l))));
    }

    @Test
    public void should_return_false_when_any_group_can_paid_amount_where_the_total_value_is_enough() {
        Assert.assertFalse(isJoinbotPossibleWithCurrentUserUTXOs(
                1000l,
                newArrayList(utxo(50l)),
                newArrayList(utxo(800l)),
                newArrayList(utxo(800l))));
    }

    private static UTXO utxo(final long amount) {
        final UTXO utxo = new UTXO();
        final MyTransactionOutPoint outPoint = new MyTransactionOutPoint(
                TestNet3Params.get(),
                Sha256Hash.ZERO_HASH,
                0,
                BigInteger.valueOf(amount),
                new byte[0],
                null,
                0);
        utxo.setOutpoints(newArrayList(outPoint));
        return utxo;
    }
}
