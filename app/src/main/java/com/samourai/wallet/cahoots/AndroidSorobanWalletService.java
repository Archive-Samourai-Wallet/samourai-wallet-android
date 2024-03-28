package com.samourai.wallet.cahoots;

import android.content.Context;
import android.util.Log;

import com.samourai.http.client.AndroidHttpClientService;
import com.samourai.soroban.client.SorobanConfig;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.bip47.rpc.secretPoint.ISecretPointFactory;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.cahoots.manual.ManualCahootsService;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.constants.SamouraiNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.send.provider.CahootsUtxoProvider;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.ExtLibJConfig;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.whirlpool.client.wallet.AndroidWalletSupplier;
import com.samourai.whirlpool.client.wallet.data.AndroidChainSupplier;

import java.security.Provider;
import java.security.Security;

public class AndroidSorobanWalletService {
    private static final String TAG = "AndroidSorobanWalletS";
    private static final Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider(); // use spongycastle
    private static AndroidSorobanWalletService instance;
    private Context ctx;
    private CahootsWallet cahootsWallet;
    private SorobanConfig sorobanConfig;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static AndroidSorobanWalletService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidSorobanWalletService(ctx);
        }
        return instance;
    }

    private AndroidSorobanWalletService(Context ctx) {
        this.ctx = ctx;
        this.sorobanConfig = computeSorobanConfig(ctx);

        ChainSupplier chainSupplier = AndroidChainSupplier.getInstance(ctx);
        CahootsUtxoProvider utxoProvider = AndroidCahootsUtxoProvider.getInstance(ctx);
        WalletSupplier walletSupplier = AndroidWalletSupplier.getInstance(ctx);
        this.cahootsWallet = new CahootsWalletImpl(chainSupplier, walletSupplier, utxoProvider);
    }

    private static ExtLibJConfig computeExtLibJConfig(Context ctx) {
        boolean testnet = SamouraiWallet.getInstance().isTestNet();
        SamouraiNetwork samouraiNetwork = testnet ? SamouraiNetwork.TESTNET : SamouraiNetwork.MAINNET;
        DojoUtil dojoUtil = DojoUtil.getInstance(ctx);
        String dojoParams = dojoUtil.getDojoParams();
        boolean useDojo = (dojoParams != null);
        boolean isTorRequired = SamouraiTorManager.INSTANCE.isRequired();
        boolean onion = useDojo || isTorRequired;
        IHttpClientService httpClientService = AndroidHttpClientService.getInstance(ctx);
        ISecretPointFactory secretPointFactory = AndroidSecretPointFactory.getInstance();
        CryptoUtil cryptoUtil = CryptoUtil.getInstance(PROVIDER);
        BIP47Util bip47Util = BIP47Util.getInstance(ctx);

        Log.v(TAG, "whirlpoolWalletConfig[Tor] = onion=" + onion + ", useDojo=" + useDojo + ", torManager.isRequired=" + isTorRequired);
        return new ExtLibJConfig(samouraiNetwork, onion, httpClientService, cryptoUtil, bip47Util,
                BIP_FORMAT.PROVIDER, secretPointFactory);
    }

    public static SorobanConfig computeSorobanConfig(Context ctx) {
        ExtLibJConfig extLibJConfig = computeExtLibJConfig(ctx);
        return new SorobanConfig(extLibJConfig);
    }

    public CahootsWallet getCahootsWallet() {
        return cahootsWallet;
    }

    public SorobanWalletInitiator getSorobanWalletInitiator() throws Exception {
        checkOnline();
        SorobanWalletService sorobanWalletService = sorobanConfig.getSorobanWalletService();
        SorobanWalletInitiator sw = sorobanWalletService.getSorobanWalletInitiator(cahootsWallet);
        sw.setTimeoutMeetingMs(120000);
        return sw;
    }

    public SorobanWalletCounterparty getSorobanWalletCounterparty() throws Exception {
        checkOnline();
        SorobanWalletService sorobanWalletService = sorobanConfig.getSorobanWalletService();
        SorobanWalletCounterparty sw = sorobanWalletService.getSorobanWalletCounterparty(cahootsWallet);
        sw.setTimeoutMeetingMs(60000);
        return sw;
    }

    private void checkOnline() throws Exception {
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }

    public ManualCahootsService getManualCahootsService() {
        return sorobanConfig.getSorobanWalletService().getManualCahootsService();
    }
}
