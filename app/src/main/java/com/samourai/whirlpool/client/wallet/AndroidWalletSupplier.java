package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.whirlpool.client.wallet.data.AndroidWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class AndroidWalletSupplier {
    private static WalletSupplier instance = null;

    public static synchronized WalletSupplier getInstance(Context ctx) {
        if (instance == null) {
            WalletStateSupplier walletStateSupplier = AndroidWalletStateSupplier.getInstance(ctx);
            HD_Wallet bip44w = HD_WalletFactory.getInstance(ctx).get();
            instance = new WalletSupplierImpl(walletStateSupplier, bip44w);
        }
        return instance;
    }
}
