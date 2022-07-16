package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;

import org.bitcoinj.core.NetworkParameters;

import java.security.Provider;
import java.security.Security;

public class AndroidSorobanCahootsService extends SorobanCahootsService {
    private static final Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider(); // use spongycastle

    private static AndroidSorobanCahootsService instance;
    private Context ctx;

    static {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    public static AndroidSorobanCahootsService getInstance(Context ctx) {
        if (instance == null) {
            BIP47Util bip47Util = BIP47Util.getInstance(ctx);
            NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
            IHttpClient httpClient = AndroidHttpClient.getInstance(ctx);
            RpcService rpcService = new RpcService(httpClient, PROVIDER, TorManager.INSTANCE.isRequired());
            instance = new AndroidSorobanCahootsService(
                    bip47Util, BIP_FORMAT.PROVIDER, params, rpcService, ctx
            );
        }
        return instance;
    }

    private AndroidSorobanCahootsService(BIP47UtilGeneric bip47Util,
                                         BipFormatSupplier bipFormatSupplier,
                                         NetworkParameters params,
                                         RpcService rpcService,
                                         Context ctx) {
        super(bip47Util, bipFormatSupplier, params, rpcService);
        this.ctx = ctx;
    }

    @Override
    protected void checkTor() throws Exception {
        // require online
        if (AppUtil.getInstance(ctx).isOfflineMode()) {
            throw new Exception("Online mode is required for online Cahoots");
        }
    }
}
