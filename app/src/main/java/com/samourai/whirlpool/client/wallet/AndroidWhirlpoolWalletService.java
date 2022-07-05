package com.samourai.whirlpool.client.wallet;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.http.client.AndroidHttpClientService;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.data.AndroidDataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.AndroidWalletStateSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataPersister.FileDataPersisterFactory;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceFactory;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.client.whirlpool.ServerApi;

import org.bitcoinj.core.NetworkParameters;
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
    private WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();
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

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(Context ctx) {
        TorManager torManager = TorManager.INSTANCE;
        boolean testnet = SamouraiWallet.getInstance().isTestNet();
        DojoUtil dojoUtil = DojoUtil.getInstance(ctx);

        String dojoParams = dojoUtil.getDojoParams();
        boolean useDojo = (dojoParams != null);
        boolean onion = useDojo || torManager.isRequired();

        Log.v(TAG, "whirlpoolWalletConfig[Tor] = onion="+onion+", useDojo="+useDojo+", torManager.isRequired="+torManager.isRequired());

        String scode = WhirlpoolMeta.getInstance(ctx).getSCODE();

        IHttpClientService httpClientService = AndroidHttpClientService.getInstance(ctx);
        return computeWhirlpoolWalletConfig(torManager, testnet, onion, scode, httpClientService, ctx);
    }

    private DataSourceFactory computeDataSourceFactory(Context ctx) {
        PushTx pushTx = PushTx.getInstance(ctx);
        FeeUtil feeUtil = FeeUtil.getInstance();
        APIFactory apiFactory = APIFactory.getInstance(ctx);
        UTXOFactory utxoFactory = UTXOFactory.getInstance(ctx);
        BIP47Util bip47Util = BIP47Util.getInstance(ctx);
        BIP47Meta bip47Meta = BIP47Meta.getInstance();
        WalletSupplier walletSupplier = AndroidWalletSupplier.getInstance(ctx);
        return new AndroidDataSourceFactory(pushTx, feeUtil, apiFactory, utxoFactory, bip47Util, bip47Meta, walletSupplier);
    }

    private DataPersisterFactory computeDataPersisterFactory(Context ctx) {
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

    protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(TorManager torManager, boolean testnet, boolean onion, String scode, IHttpClientService httpClientService, Context ctx) {
        IStompClientService stompClientService = new AndroidStompClientService(torManager);
        TorClientService torClientService = new AndroidWhirlpoolTorService(torManager);

        WhirlpoolServer whirlpoolServer = testnet ? WhirlpoolServer.TESTNET : WhirlpoolServer.MAINNET;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
        NetworkParameters params = whirlpoolServer.getParams();

        DataSourceFactory dataSourceFactory = computeDataSourceFactory(ctx);
        DataPersisterFactory dataPersisterFactory = computeDataPersisterFactory(ctx);

        WhirlpoolWalletConfig whirlpoolWalletConfig =
                new WhirlpoolWalletConfig(dataSourceFactory,
                        httpClientService, stompClientService, torClientService, serverApi, params, true);
        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());
        whirlpoolWalletConfig.setBip47Util(BIP47Util.getInstance(ctx));
        whirlpoolWalletConfig.setDataPersisterFactory(dataPersisterFactory);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(true); // enable auto-mix

        whirlpoolWalletConfig.setScode(scode);
        whirlpoolWalletConfig.setMaxClients(1);
        whirlpoolWalletConfig.setLiquidityClient(false); // disable concurrent liquidity thread

        whirlpoolWalletConfig.setSecretPointFactory(AndroidSecretPointFactory.getInstance());

        for (Map.Entry<String,String> configEntry : whirlpoolWalletConfig.getConfigInfo().entrySet()) {
            Log.v(TAG, "whirlpoolWalletConfig["+configEntry.getKey()+"] = "+configEntry.getValue());
        }
        return whirlpoolWalletConfig;
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
