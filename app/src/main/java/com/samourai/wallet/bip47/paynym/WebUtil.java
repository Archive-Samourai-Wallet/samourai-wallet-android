package com.samourai.wallet.bip47.paynym;

import android.content.Context;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebUtil {

    public static final String PAYNYM_API = "https://paynym.is/";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;


    private static WebUtil instance = null;
    private Context context = null;

    private WebUtil(Context ctx) {
        this.context = ctx;
    }

    public static WebUtil getInstance(Context ctx) {

        if (instance == null) {

            instance = new WebUtil(ctx);
        }

        return instance;
    }

    public String postURL(
            final String contentType,
            final String authToken,
            final String requestURL,
            final String jsonBody) throws Exception {

        MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(JSON, jsonBody);

        OkHttpClient.Builder builder = com.samourai.wallet.util.WebUtil.getInstance(context).httpClientBuilder(requestURL);

        Request.Builder rbuilder = new Request.Builder()
                .url(requestURL)
                .addHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);

        if (authToken != null) {
            rbuilder.addHeader("auth-token", authToken);
        }

        Request request = rbuilder
                .post(body)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();
        }

    }


}
