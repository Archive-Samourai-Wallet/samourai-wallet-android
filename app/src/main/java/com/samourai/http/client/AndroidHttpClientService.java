package com.samourai.http.client;

import android.content.Context;

import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.tor.SamouraiTorManager;

/**
 * HTTP client manager for Whirlpool.
 */
public class AndroidHttpClientService implements IHttpClientService {
    private static AndroidHttpClientService instance;

    public static AndroidHttpClientService getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidHttpClientService(ctx);
        }
        return instance;
    }

    private Context ctx;

    private AndroidHttpClientService(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public AndroidHttpClient getHttpClient(HttpUsage httpUsage) {
        return AndroidHttpClient.getInstance(ctx);
    }

    @Override
    public void changeIdentity() {
        if (SamouraiTorManager.INSTANCE.isRequired()) {
            SamouraiTorManager.INSTANCE.newIdentity();
        }
    }

    @Override
    public void stop() {
        // nothing to do
    }
}
