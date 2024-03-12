package com.samourai.http.client;

import com.samourai.wallet.api.backend.beans.HttpException;

public class HttpSystemException extends HttpException {
    public HttpSystemException(Exception cause) {
        super(cause);
    }
}
