package com.samourai.whirlpool.client.wallet.data;

import android.content.Context;
import android.widget.Toast;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.chain.ChainSupplier;

import androidx.core.content.ContextCompat;

public class AndroidChainSupplier implements ChainSupplier {
    private static AndroidChainSupplier instance = null;
    private static Context ctx = null;

    public static AndroidChainSupplier getInstance(Context ctx) {
        if (instance == null) {
            APIFactory apiFactory = APIFactory.getInstance(ctx);
            instance = new AndroidChainSupplier(apiFactory);
            AndroidChainSupplier.ctx = ctx;
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
