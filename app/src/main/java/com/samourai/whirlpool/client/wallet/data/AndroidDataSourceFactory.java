package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;

public class AndroidDataSourceFactory implements DataSourceFactory {
    private PushTx pushTx;
    private FeeUtil feeUtil;
    private APIFactory apiFactory;
    private UTXOFactory utxoFactory;
    private BIP47Util bip47Util;
    private BIP47Meta bip47Meta;

    public AndroidDataSourceFactory(PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory, BIP47Util bip47Util, BIP47Meta bip47Meta) {
        this.pushTx = pushTx;
        this.feeUtil = feeUtil;
        this.apiFactory = apiFactory;
        this.utxoFactory = utxoFactory;
        this.bip47Util = bip47Util;
        this.bip47Meta = bip47Meta;
    }

    @Override
    public DataSource createDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister) throws Exception {
        return new AndroidDataSource(whirlpoolWallet, bip44w, dataPersister, pushTx, feeUtil, apiFactory, utxoFactory, bip47Util, bip47Meta);
    }
}
