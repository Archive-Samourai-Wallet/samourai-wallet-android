package com.samourai.http.client;

import android.content.Context;

import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.tor.TorManager;

import io.matthewnelson.topl_service.TorServiceController;

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
        if (TorManager.INSTANCE.isRequired()) {
            TorServiceController.newIdentity();
        }
    }

    @Override
    public void stop() {
        // nothing to do
    }
}
