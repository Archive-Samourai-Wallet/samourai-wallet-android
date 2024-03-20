package com.samourai.wallet.util.tech;

import android.os.Handler;
import android.os.Looper;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartTaskRunner {

    public static final String TAG = SmartTaskRunner.class.getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler taskHandler;
    private final Handler callbackHandler;

    private SmartTaskRunner(final Looper taskLooper) {
        taskHandler = new Handler(taskLooper);
        callbackHandler = new Handler(Looper.myLooper());
    }

    public static SmartTaskRunner create(final Looper taskLooper) {
        return new SmartTaskRunner(taskLooper);
    }

    public void executeAsync(final Runnable runnable) {
        taskHandler.post(() -> executor.execute(runnable));
    }

    public <T> void executeAsync(final Callable<T> callable, final SimpleCallback<T> simpleCallback) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(simpleCallback);
        taskHandler.post(() -> executor.submit(() -> {
            final T result;
            try {
                result = callable.call();
            } catch (final Exception e) {
                simpleCallback.onException(e);
                return;
            }
            callbackHandler.post(() -> simpleCallback.onComplete(result));
        }));
    }

}
