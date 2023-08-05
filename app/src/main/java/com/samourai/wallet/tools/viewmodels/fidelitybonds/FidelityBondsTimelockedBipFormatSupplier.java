package com.samourai.wallet.tools.viewmodels.fidelitybonds;

import com.google.common.collect.Lists;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipFormat.BipFormatSupplier;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutput;

import java.util.Collection;
import java.util.Collections;

public class FidelityBondsTimelockedBipFormatSupplier implements BipFormatSupplier {

    private final FidelityBondsTimelockedBipFormat bipFormat;

    private FidelityBondsTimelockedBipFormatSupplier(
            final FidelityBondsTimelockedBipFormat bipFormat) {

        this.bipFormat = bipFormat;
    }

    public static FidelityBondsTimelockedBipFormatSupplier create(
            final FidelityBondsTimelockedBipFormat bipFormat) {

        return new FidelityBondsTimelockedBipFormatSupplier(bipFormat);
    }

    @Override
    public Collection<BipFormat> getList() {
        return Collections.unmodifiableCollection(Lists.newArrayList(bipFormat));
    }

    @Override
    public BipFormat findByAddress(final String address, final NetworkParameters params) {
        return bipFormat;
    }

    @Override
    public BipFormat findById(final String bipFormatId) {
        return bipFormat;
    }

    @Override
    public String getToAddress(final TransactionOutput output) throws Exception {
        return BIP_FORMAT.PROVIDER.getToAddress(output);
    }

    @Override
    public String getToAddress(final byte[] scriptBytes,
                               final NetworkParameters params) throws Exception {

        return BIP_FORMAT.PROVIDER.getToAddress(scriptBytes, params);
    }

    @Override
    public TransactionOutput getTransactionOutput(final String address,
                                                  final long amount,
                                                  final NetworkParameters params) throws Exception {

        return BIP_FORMAT.PROVIDER.getTransactionOutput(address, amount, params);
    }
}
