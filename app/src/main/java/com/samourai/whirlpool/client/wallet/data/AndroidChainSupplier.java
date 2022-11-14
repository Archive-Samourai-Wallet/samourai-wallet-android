package com.samourai.whirlpool.client.wallet.data;

import android.content.Context;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.chain.ChainSupplier;

public class AndroidChainSupplier implements ChainSupplier {
    private static AndroidChainSupplier instance = null;

    public static AndroidChainSupplier getInstance(Context ctx) {
        if (instance == null) {
            APIFactory apiFactory = APIFactory.getInstance(ctx);
            instance = new AndroidChainSupplier(apiFactory);
        }
        return instance;
    }

    private APIFactory apiFactory;

    private AndroidChainSupplier(APIFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    @Override
    public WalletResponse.InfoBlock getLatestBlock() {
        WalletResponse.InfoBlock infoBlock = new WalletResponse.InfoBlock();
        infoBlock.height = (int)apiFactory.getLatestBlockHeight();
        infoBlock.hash = apiFactory.getLatestBlockHash();
        infoBlock.time = apiFactory.getLatestBlockTime();
        return infoBlock;
    }
}
