package com.samourai.http.client;

import android.content.Context;

import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.httpClient.JacksonHttpClient;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidHttpClient extends JacksonHttpClient {
    private static AndroidHttpClient instance;

    public static AndroidHttpClient getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidHttpClient(ctx);
        }
        return instance;
    }

    private WebUtil webUtil;
    private TorManager torManager;

    private AndroidHttpClient(Context ctx) {
        this(WebUtil.getInstance(ctx), TorManager.INSTANCE);
    }

    public AndroidHttpClient(WebUtil webUtil, TorManager torManager) {
        super((e) -> {
            if (torManager.isRequired()) {
                TorServiceController.newIdentity();
            }
        });
        this.webUtil = webUtil;
        this.torManager = torManager;
    }

    @Override
    public void connect() {
        // ok
    }

    @Override
    protected String requestJsonGet(String urlStr, Map<String, String> headers, boolean async) throws HttpException {
        return webUtil.getURL(urlStr, headers);
    }

    @Override
    protected String requestJsonPost(String url, Map<String, String> headers, String jsonBody) throws HttpException {
        if (torManager.isRequired()) {
            return webUtil.tor_postURL(url, jsonBody, headers);
        } else {
            return webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonBody, headers);
        }
    }

    @Override
    protected String requestJsonPostUrlEncoded(String url, Map<String, String> headers, Map<String, String> body) throws HttpException {
        if (torManager.isRequired()) {
            // tor enabled
            return webUtil.tor_postURL(url, body, headers);
        } else {
            // tor disabled
            String jsonString = queryString(body);
            return webUtil.postURL(null, url, jsonString, headers);
        }
    }

    @Override
    protected String requestStringPost(String s, Map<String, String> map, String s1, String s2) throws HttpException {
        return null; // not used yet
    }

    public String queryString(final Map<String,String> parameters) throws HttpException {
        String url = "";
        try {
            for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                final String encodedKey = URLEncoder.encode(parameter.getKey(), "UTF-8");
                final String encodedValue = URLEncoder.encode(parameter.getValue(), "UTF-8");
                url += encodedKey + "=" + encodedValue + "&";
            }
        } catch (Exception e) {
            throw new HttpSystemException(e);
        }
        return url;
    }

    @Override
    protected <T> Single<Optional<T>> httpObservable(final Callable<T> supplier) {
        return Single.fromCallable(() -> Optional.ofNullable(supplier.call()))
                // important: subscribe & observe on IO for Cahoots stuff
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }
}
