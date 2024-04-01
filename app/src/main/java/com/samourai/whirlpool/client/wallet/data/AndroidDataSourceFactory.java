package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.PushTx;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import org.bitcoinj.core.NetworkParameters;

public class AndroidDataSourceFactory implements DataSourceFactory {
    private PushTx pushTx;
    private APIFactory apiFactory;
    private BIP47Util bip47Util;
    private BIP47Meta bip47Meta;
    private WalletSupplier walletSupplier;
    private DataSourceConfig dataSourceConfig;
    private BackendApi backendApi;

    public AndroidDataSourceFactory(PushTx pushTx, APIFactory apiFactory, BIP47Util bip47Util, BIP47Meta bip47Meta, WalletSupplier walletSupplier, DataSourceConfig dataSourceConfig, BackendApi backendApi) {
        this.pushTx = pushTx;
        this.apiFactory = apiFactory;
        this.bip47Util = bip47Util;
        this.bip47Meta = bip47Meta;
        this.walletSupplier = walletSupplier;
        this.dataSourceConfig = dataSourceConfig;
        this.backendApi = backendApi;
    }

    @Override
    public DataSource createDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, String passphrase, WalletStateSupplier walletStateSupplier, UtxoConfigSupplier utxoConfigSupplier) throws Exception {
        NetworkParameters params = whirlpoolWallet.getConfig().getSamouraiNetwork().getParams();
        return new AndroidDataSource(walletSupplier, utxoConfigSupplier, dataSourceConfig, pushTx, apiFactory, bip47Util, bip47Meta, backendApi, params);
    }
}
