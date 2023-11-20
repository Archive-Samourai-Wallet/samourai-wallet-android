package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.send.provider.CahootsUtxoProvider;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.whirlpool.client.wallet.AndroidWalletSupplier;
import com.samourai.whirlpool.client.wallet.data.AndroidChainSupplier;

import org.bitcoinj.core.NetworkParameters;

import java.security.Provider;
import java.security.Security;

public class AndroidSorobanWalletService extends SorobanWalletService {
    private static final Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider(); // use spongycastle

    private static AndroidSorobanWalletService instance;
    private Context ctx;
    private CahootsWallet cahootsWallet;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static AndroidSorobanWalletService getInstance(Context ctx) {
        if (instance == null) {
            BIP47Util bip47Util = BIP47Util.getInstance(ctx);
            NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
            IHttpClient httpClient = AndroidHttpClient.getInstance(ctx);
            CryptoUtil cryptoUtil = CryptoUtil.getInstance(PROVIDER);
            boolean onion = SamouraiTorManager.INSTANCE.isRequired();
            RpcService rpcService = new RpcService(httpClient, cryptoUtil, onion);
            WalletSupplier walletSupplier = AndroidWalletSupplier.getInstance(ctx);
            CahootsUtxoProvider cahootsUtxoProvider = AndroidCahootsUtxoProvider.getInstance(ctx);
            instance = new AndroidSorobanWalletService(
                    bip47Util,
                    BIP_FORMAT.PROVIDER,
                    params,
                    rpcService,
                    walletSupplier,
                    cahootsUtxoProvider,
                    ctx
            );
        }
        return instance;
    }

    private AndroidSorobanWalletService(BIP47UtilGeneric bip47Util,
                                        BipFormatSupplier bipFormatSupplier,
                                        NetworkParameters params,
                                        RpcService rpcService,
                                        WalletSupplier walletSupplier,
                                        CahootsUtxoProvider cahootsUtxoProvider,
                                        Context ctx) {
        super(bip47Util, bipFormatSupplier, params, rpcService);
        this.ctx = ctx;
        ChainSupplier chainSupplier = AndroidChainSupplier.getInstance(ctx);
        this.cahootsWallet = new CahootsWallet(
                walletSupplier,
                chainSupplier,
                BIP_FORMAT.PROVIDER,
                params,
                cahootsUtxoProvider);
    }

    public CahootsWallet getCahootsWallet() {
        return cahootsWallet;
    }

    public SorobanWalletInitiator getSorobanWalletInitiator() throws Exception {
        checkOnline();
        SorobanWalletInitiator sw = getSorobanWalletInitiator(cahootsWallet);
        sw.setTimeoutMeetingMs(120000);
        return sw;
    }

    public SorobanWalletCounterparty getSorobanWalletCounterparty() throws Exception {
        checkOnline();
        SorobanWalletCounterparty sw = getSorobanWalletCounterparty(cahootsWallet);
        sw.setTimeoutMeetingMs(60000);
        return sw;
    }

    private void checkOnline() throws Exception {
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }
}
