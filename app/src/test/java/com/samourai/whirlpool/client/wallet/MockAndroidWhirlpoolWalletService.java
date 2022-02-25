package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.http.client.IHttpClientService;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.tor.ITorManager;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.data.MockAndroidDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.MockServerApi;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.MockAndroidFileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.whirlpool.ServerApi;

import java.io.File;

public class MockAndroidWhirlpoolWalletService extends AndroidWhirlpoolWalletService {
    @Override
    protected DataPersisterFactory computeDataPersisterFactory(Context ctx) {
        try {
            File fileIndex = File.createTempFile("test-state", "test");
            File fileUtxo = File.createTempFile("test-utxos", "test");
            WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();
            return new MockAndroidFileDataPersisterFactory(whirlpoolUtils, ctx, fileIndex, fileUtxo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected DataSourceFactory computeDataSourceFactory(Context ctx) {
        PushTx pushTx = PushTx.getInstance(ctx);
        FeeUtil feeUtil = FeeUtil.getInstance();
        APIFactory apiFactory = APIFactory.getInstance(ctx);
        UTXOFactory utxoFactory = UTXOFactory.getInstance(ctx);
        BIP47Util bip47Util = BIP47Util.getInstance(ctx);
        BIP47Meta bip47Meta = BIP47Meta.getInstance();
        return new MockAndroidDataSourceFactory(pushTx, feeUtil, apiFactory, utxoFactory, bip47Util, bip47Meta);
    }

    @Override
    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(ITorManager torManager, boolean testnet, boolean onion, String scode, IHttpClientService httpClientService, Context ctx) {
        WhirlpoolWalletConfig config = super.computeWhirlpoolWalletConfig(torManager, testnet, onion, scode, httpClientService, ctx);

        // mock serverApi
        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        ServerApi serverApi = new MockServerApi(serverUrl, httpClientService);
        config.setServerApi(serverApi);
        return config;
    }
}
