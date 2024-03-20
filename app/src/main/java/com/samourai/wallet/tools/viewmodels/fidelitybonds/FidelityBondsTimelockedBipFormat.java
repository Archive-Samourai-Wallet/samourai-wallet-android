package com.samourai.wallet.tools.viewmodels.fidelitybonds;

import com.samourai.wallet.bipFormat.BipFormatImpl;
import com.samourai.wallet.hd.HD_Account;
import com.samourai.wallet.segwit.FidelityTimelockAddress;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.Calendar;
import java.util.TimeZone;

public class FidelityBondsTimelockedBipFormat extends BipFormatImpl {

    public static final String ID = "P2WSH";
    private final int timelockIndex;

    private FidelityBondsTimelockedBipFormat(final int timelockIndex) {
        super(ID, "FB_TIMELOCKED");
        this.timelockIndex = timelockIndex;
    }

    public static FidelityBondsTimelockedBipFormat create(final int timelockIndex) {
        return new FidelityBondsTimelockedBipFormat(timelockIndex);
    }

    @Override
    public String getBipPub(final HD_Account hdAccount) {
        // FB uses purpose 84 on public key derivation
        return hdAccount.zpubstr();
    }

    @Override
    public String getToAddress(final ECKey ecKey, final NetworkParameters params) {
        try {
            return new FidelityTimelockAddress(ecKey, params, timelockIndex).getTimelockAddressAsString();
        } catch (final Exception e) {
            throw new FidelityTimelockAddressException(e);
        }
    }

    @Override
    public void sign(final Transaction tx, final int inputIndex, final ECKey key) throws Exception {

        final TransactionInput txInput = tx.getInput(inputIndex);
        final Coin spendAmount = txInput.getValue();

        final FidelityTimelockAddress fAddress = new FidelityTimelockAddress(key, tx.getParams(), timelockIndex);
        final Script redeemScript = fAddress.segwitRedeemScript();

        final TransactionSignature sig = tx.calculateWitnessSignature(
                inputIndex,
                key,
                redeemScript.getProgram(),
                spendAmount,
                Transaction.SigHash.ALL,
                false);

        final TransactionWitness witness = new TransactionWitness(2);
        witness.setPush(0, sig.encodeToBitcoin());
        witness.setPush(1, redeemScript.getProgram());
        tx.setWitness(inputIndex, witness);
    }

    public long getTimelock() {

        int year = 2020 + (timelockIndex / 12);
        int calendarMonth = timelockIndex % 12;

        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(year, calendarMonth, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() / 1000L;
    }
}
