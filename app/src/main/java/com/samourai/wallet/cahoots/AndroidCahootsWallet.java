package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.whirlpool.client.wallet.AndroidWalletSupplier;
import com.samourai.whirlpool.client.wallet.data.AndroidCahootsUtxoProvider;

public class AndroidCahootsWallet extends CahootsWallet {
    private static AndroidCahootsWallet instance = null;

    public static AndroidCahootsWallet getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidCahootsWallet(ctx);
        }
        return instance;
    }

    private AndroidCahootsWallet(Context ctx) {
        super(AndroidWalletSupplier.getInstance(ctx), BIP_FORMAT.PROVIDER, SamouraiWallet.getInstance().getCurrentNetworkParams(), AndroidCahootsUtxoProvider.getInstance(ctx));
    }

    @Override
    public long fetchFeePerB() {
        long feePerB = FeeUtil.getInstance().getSuggestedFeeDefaultPerB();
        return feePerB;
    }
}
