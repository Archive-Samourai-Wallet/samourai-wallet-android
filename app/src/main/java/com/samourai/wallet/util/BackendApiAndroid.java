package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.AndroidHttpClientService;
import com.samourai.http.client.HttpUsage;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.tor.SamouraiTorManager;

public class BackendApiAndroid {
    private static BackendApi backendApi;

    public static BackendApi getInstance(Context ctx) {
        if (backendApi == null) {
            AndroidHttpClient httpClient = AndroidHttpClientService.getInstance(ctx).getHttpClient(HttpUsage.BACKEND);
            boolean testnet = SamouraiWallet.getInstance().isTestNet();
            boolean useDojo = (DojoUtil.getInstance(ctx).getDojoParams() != null);
            if (useDojo) {
                // use dojo backend
                String dojoApiKey = APIFactory.getInstance(ctx).getAppToken();
                String dojoUrl = testnet ? WebUtil.SAMOURAI_API2_TESTNET_TOR : WebUtil.SAMOURAI_API2_TOR;
                backendApi = BackendApi.newBackendApiDojo(httpClient, dojoUrl, dojoApiKey);
            } else {
                // use samourai backend
                boolean onion = SamouraiTorManager.INSTANCE.isRequired();
                String backendUrl = BackendServer.get(testnet).getBackendUrl(onion);
                backendApi = BackendApi.newBackendApiSamourai(httpClient, backendUrl);
            }
        }
        return backendApi;
    }

    public static void reset() {
        backendApi = null;
    }
}
