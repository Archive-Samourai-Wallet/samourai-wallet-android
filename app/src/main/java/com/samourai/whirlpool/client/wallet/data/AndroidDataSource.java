package com.samourai.whirlpool.client.wallet.data;


import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.ISweepBackend;
import com.samourai.wallet.api.backend.seenBackend.ISeenBackend;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.send.PushTx;
import com.samourai.whirlpool.client.wallet.data.coordinator.CoordinatorSupplier;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

import org.bitcoinj.core.NetworkParameters;

public class AndroidDataSource implements DataSource {
    private PushTx pushTx;
    private WalletSupplier walletSupplier;
    private UtxoSupplier utxoSupplier;
    private DataSourceConfig dataSourceConfig;
    private BackendApi backendApi;

    public AndroidDataSource(WalletSupplier walletSupplier, UtxoConfigSupplier utxoConfigSupplier, DataSourceConfig dataSourceConfig, PushTx pushTx, APIFactory apiFactory, BIP47Util bip47Util, BIP47Meta bip47Meta, BackendApi backendApi, NetworkParameters params) {
        this.pushTx = pushTx;
        this.walletSupplier = walletSupplier;
        this.utxoSupplier = new AndroidUtxoSupplier(walletSupplier, utxoConfigSupplier, dataSourceConfig, apiFactory, bip47Util, bip47Meta, params);
        this.dataSourceConfig = dataSourceConfig;
        this.backendApi = backendApi;
    }

    @Override
    public void open(CoordinatorSupplier coordinatorSupplier) throws Exception {
        // initialize for coordinatorSupplier
        coordinatorSupplier.load();
        getUtxoSupplier()._setCoordinatorSupplier(coordinatorSupplier);
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public PushTx getPushTx() {
        return pushTx;
    }

    @Override
    public WalletSupplier getWalletSupplier() {
        return walletSupplier;
    }

    @Override
    public UtxoSupplier getUtxoSupplier() {
        return utxoSupplier;
    }

    @Override
    public PaynymSupplier getPaynymSupplier() {
        return null; // not implemented
    }

    @Override
    public DataSourceConfig getDataSourceConfig() {
        return dataSourceConfig;
    }

    @Override
    public ISeenBackend getSeenBackend() {
        return backendApi;
    }

    @Override
    public ISweepBackend getSweepBackend() {
        return backendApi; // not used yet
    }
}
