package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class AndroidDataSourceFactory implements DataSourceFactory {
    private PushTx pushTx;
    private FeeUtil feeUtil;
    private APIFactory apiFactory;
    private BIP47Util bip47Util;
    private BIP47Meta bip47Meta;
    private WalletSupplier walletSupplier;
    private ChainSupplier chainSupplier;

    public AndroidDataSourceFactory(PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, BIP47Util bip47Util, BIP47Meta bip47Meta, WalletSupplier walletSupplier, ChainSupplier chainSupplier) {
        this.pushTx = pushTx;
        this.feeUtil = feeUtil;
        this.apiFactory = apiFactory;
        this.bip47Util = bip47Util;
        this.bip47Meta = bip47Meta;
        this.walletSupplier = walletSupplier;
        this.chainSupplier = chainSupplier;
    }

    @Override
    public DataSource createDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, String passphrase, WalletStateSupplier walletStateSupplier, UtxoConfigSupplier utxoConfigSupplier) throws Exception {
        return new AndroidDataSource(whirlpoolWallet, pushTx, feeUtil, apiFactory, bip47Util, bip47Meta, walletSupplier, utxoConfigSupplier, chainSupplier);
    }
}
