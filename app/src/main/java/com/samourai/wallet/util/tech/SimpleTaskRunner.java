package com.samourai.wallet.util.tech;

import android.os.Handler;
import android.os.Looper;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleTaskRunner {

    public static final String TAG = SimpleTaskRunner.class.getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SimpleTaskRunner() {}

    public static SimpleTaskRunner create() {
        return new SimpleTaskRunner();
    }

    public void executeAsync(final Runnable runnable) {
        executor.execute(runnable);
    }

    public <T> void executeAsync(final Callable<T> callable, final SimpleCallback<T> simpleCallback) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(simpleCallback);
        executor.submit(() -> {
            final T result;
            try {
                result = callable.call();
            } catch (final Exception e) {
                simpleCallback.onException(e);
                return;
            }
            handler.post(() -> {
                simpleCallback.onComplete(result);
            });
        });
    }

}
