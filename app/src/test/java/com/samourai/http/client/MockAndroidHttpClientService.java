package com.samourai.http.client;

import android.content.Context;

import com.samourai.wallet.tor.ITorManager;
import com.samourai.wallet.tor.MockTorManager;
import com.samourai.wallet.util.WebUtil;

public class MockAndroidHttpClientService extends AndroidHttpClientService{
    private AndroidHttpClient httpClient;

    public MockAndroidHttpClientService(Context context) {
        super(context);
        WebUtil webUtil = WebUtil.getInstance(context);
        ITorManager torManager = new MockTorManager();
        httpClient = new AndroidHttpClient(webUtil, torManager);
    }

    @Override
    public AndroidHttpClient getHttpClient(HttpUsage httpUsage) {
        return httpClient;
    }
}
