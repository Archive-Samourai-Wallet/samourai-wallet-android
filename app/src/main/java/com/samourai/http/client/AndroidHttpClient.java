package com.samourai.http.client;

import android.content.Context;

import com.google.common.util.concurrent.RateLimiter;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.httpClient.HttpSystemException;
import com.samourai.wallet.httpClient.JacksonHttpClient;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.network.WebUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidHttpClient extends JacksonHttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidHttpClient.class);

    // limit changing Tor identity on network error every 4 minutes
    private static final double RATE_CHANGE_IDENTITY_ON_NETWORK_ERROR = 1.0 / 240;
    private static AndroidHttpClient instance;

    public static AndroidHttpClient getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidHttpClient(ctx);
        }
        return instance;
    }

    private WebUtil webUtil;

    private AndroidHttpClient(Context ctx) {
        this(WebUtil.getInstance(ctx));
    }

    public AndroidHttpClient(WebUtil webUtil) {
        super(computeOnNetworkError());
        this.webUtil = webUtil;
    }

    protected static Consumer<Exception> computeOnNetworkError() {
        RateLimiter rateLimiter = RateLimiter.create(RATE_CHANGE_IDENTITY_ON_NETWORK_ERROR);
        return e -> {
            if (SamouraiTorManager.INSTANCE.isRequired()) {
                if (!rateLimiter.tryAcquire()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("onNetworkError: not changing Tor identity (too many recent attempts)");
                    }
                    return;
                }
                // change Tor identity on network error
                SamouraiTorManager.INSTANCE.newIdentity();
            }
        };
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
        if (SamouraiTorManager.INSTANCE.isRequired()) {
            return webUtil.tor_postURL(url, jsonBody, headers);
        } else {
            return webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonBody, headers);
        }
    }

    @Override
    protected String requestJsonPostUrlEncoded(String url, Map<String, String> headers, Map<String, String> body) throws HttpException {
        if (SamouraiTorManager.INSTANCE.isRequired()) {
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
