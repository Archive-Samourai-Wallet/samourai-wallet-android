package com.samourai.wallet.util.tech;

public interface SimpleCallback<T> {

    void onComplete(T result);

    void onException(Throwable t);

}
