package com.samourai.whirlpool.client.wallet.data.dataPersister;

import android.content.Context;

import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.AndroidWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import java.io.File;

public class AndroidFileDataPersisterFactory extends FileDataPersisterFactory {
    private WhirlpoolUtils whirlpoolUtils;
    private Context ctx;

    public AndroidFileDataPersisterFactory(WhirlpoolUtils whirlpoolUtils, Context ctx) {
        this.whirlpoolUtils = whirlpoolUtils;
        this.ctx = ctx;
    }

    @Override
    protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
        return whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
    }

    @Override
    protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
        return whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
    }

    @Override
    protected WalletStateSupplier computeWalletStateSupplier(WhirlpoolWallet whirlpoolWallet) {
        AddressFactory addressFactory = AddressFactory.getInstance(ctx);
        return new AndroidWalletStateSupplier(addressFactory);
    }
}
