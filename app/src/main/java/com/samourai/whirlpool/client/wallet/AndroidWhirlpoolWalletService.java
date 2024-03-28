package com.samourai.whirlpool.client.wallet;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.cahoots.AndroidSorobanWalletService;
import com.samourai.wallet.constants.BIP_WALLETS;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.util.network.BackendApiAndroid;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.data.AndroidChainSupplier;
import com.samourai.whirlpool.client.wallet.data.AndroidDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.AndroidMinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.AndroidWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.WhirlpoolInfo;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;

public class AndroidWhirlpoolWalletService extends WhirlpoolWalletService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidWhirlpoolWalletService.class);

    public enum ConnectionStates {
        CONNECTED,
        STARTING,
        LOADING,
        DISCONNECTED
    }
    private BehaviorSubject<ConnectionStates> source = BehaviorSubject.create();

    private static final String TAG = "AndroidWhirlpoolWalletS";
    private static AndroidWhirlpoolWalletService instance;
    private static WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();
    private ObjectMapper objectMapper;

    public static AndroidWhirlpoolWalletService getInstance() {
        if (instance == null) {
            instance = new AndroidWhirlpoolWalletService();
        }
        return instance;
    }

    protected AndroidWhirlpoolWalletService() {
        super();
        this.objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        source.onNext(ConnectionStates.LOADING);
    }

    private WhirlpoolWallet getOrOpenWhirlpoolWallet(Context ctx) throws Exception {
        WhirlpoolWallet whirlpoolWallet = whirlpoolWallet();
        if (whirlpoolWallet == null) {
            WhirlpoolWalletConfig config = computeWhirlpoolWalletConfig(ctx);

            // wallet closed => open WhirlpoolWallet
            HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
            String walletIdentifier = whirlpoolUtils.computeWalletIdentifier(bip84w); // preserve android filenames
            whirlpoolWallet = new WhirlpoolWallet(config, bip84w.getSeed(), bip84w.getPassphrase(), walletIdentifier);
            return openWallet(whirlpoolWallet, bip84w.getPassphrase());
        }
        // wallet already opened
        return whirlpoolWallet;
    }

    private static DataSourceFactory computeDataSourceFactory(Context ctx) {
        PushTx pushTx = PushTx.getInstance(ctx);
        APIFactory apiFactory = APIFactory.getInstance(ctx);
        BIP47Util bip47Util = BIP47Util.getInstance(ctx);
        BIP47Meta bip47Meta = BIP47Meta.getInstance();
        WalletSupplier walletSupplier = AndroidWalletSupplier.getInstance(ctx);
        DataSourceConfig dataSourceConfig = new DataSourceConfig(
                AndroidMinerFeeSupplier.getInstance(),
                AndroidChainSupplier.getInstance(ctx),
                BIP_FORMAT.PROVIDER,
                BIP_WALLETS.WHIRLPOOL);
        BackendApi backendApi = BackendApiAndroid.getInstance(ctx);
        return new AndroidDataSourceFactory(pushTx, apiFactory, bip47Util, bip47Meta, walletSupplier, dataSourceConfig, backendApi);
    }

    private static DataPersisterFactory computeDataPersisterFactory(Context ctx) {
        return new FileDataPersisterFactory() {
            @Override
            protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
                return whirlpoolUtils.computeIndexFile(walletIdentifier, ctx);
            }

            @Override
            protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
                return whirlpoolUtils.computeUtxosFile(walletIdentifier, ctx);
            }

            @Override
            protected WalletStateSupplier computeWalletStateSupplier(WhirlpoolWallet whirlpoolWallet) {
                return AndroidWalletStateSupplier.getInstance(ctx);
            }
        };
    }

    public static WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx) {
        String scode = WhirlpoolMeta.getInstance(ctx).getSCODE();
        DataSourceFactory dataSourceFactory = computeDataSourceFactory(ctx);
        DataPersisterFactory dataPersisterFactory = computeDataPersisterFactory(ctx);
        SorobanConfig sorobanConfig = AndroidSorobanWalletService.computeSorobanConfig(ctx);

        WhirlpoolWalletConfig whirlpoolWalletConfig = new WhirlpoolWalletConfig(dataSourceFactory, sorobanConfig, true);
        whirlpoolWalletConfig.setDataPersisterFactory(dataPersisterFactory);
        whirlpoolWalletConfig.setScode(scode);

        for (Map.Entry<String,String> configEntry : whirlpoolWalletConfig.getConfigInfo().entrySet()) {
            Log.v(TAG, "whirlpoolWalletConfig["+configEntry.getKey()+"] = "+configEntry.getValue());
        }

        return whirlpoolWalletConfig;
    }

    public static WhirlpoolInfo computeWhirlpoolInfo(Context ctx) {
        MinerFeeSupplier minerFeeSupplier = AndroidMinerFeeSupplier.getInstance();
        WhirlpoolWalletConfig config = AndroidWhirlpoolWalletService.computeWhirlpoolWalletConfig(ctx);
        return new WhirlpoolInfo(minerFeeSupplier, config);
    }

    public Completable startService(Context ctx) {
        if (source.hasObservers())
            source.onNext(ConnectionStates.STARTING);
        return Completable.fromCallable(() -> {
            try {
                this.getOrOpenWhirlpoolWallet(ctx).startAsync().blockingAwait();
                if (source.hasObservers()) {
                    source.onNext(ConnectionStates.CONNECTED);
                }
                return true;
            } catch (Exception e) {
                // start failed
                stop();
                throw e;
            }
        });
    }

    public void stop() {
        if (source.hasObservers()) {
            source.onNext(ConnectionStates.DISCONNECTED);
        }
        if (whirlpoolWallet() != null) {
            closeWallet();
        }
    }

    public BehaviorSubject<ConnectionStates> listenConnectionStatus() {
        return source;
    }
}
