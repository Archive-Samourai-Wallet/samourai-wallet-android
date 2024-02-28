package com.samourai.wallet.util.tech;

public interface SimpleCallback<T> {

    default void onComplete(T result) {}

    default void onException(Throwable t) {}

}
