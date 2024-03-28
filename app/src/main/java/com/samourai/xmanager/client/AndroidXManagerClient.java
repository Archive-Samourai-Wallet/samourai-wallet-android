package com.samourai.xmanager.client;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.xmanagerClient.XManagerClient;

public class AndroidXManagerClient extends XManagerClient {
    private static AndroidXManagerClient instance;

    public static AndroidXManagerClient getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidXManagerClient(ctx);
        }
        return instance;
    }

    private AndroidXManagerClient(Context ctx) {
        super(AndroidHttpClient.getInstance(ctx), SamouraiWallet.getInstance().isTestNet(), SamouraiTorManager.INSTANCE.isConnected());
    }
}
