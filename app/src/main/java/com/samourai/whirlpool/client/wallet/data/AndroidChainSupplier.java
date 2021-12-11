package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidChainSupplier implements ChainSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidChainSupplier.class);

    private APIFactory apiFactory;

    public AndroidChainSupplier(APIFactory apiFactory) {
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
