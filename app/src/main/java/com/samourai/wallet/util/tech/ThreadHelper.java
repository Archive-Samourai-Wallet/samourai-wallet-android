package com.samourai.wallet.util.tech;

import android.util.Log;

public class ThreadHelper {

    private static final String TAG = "ThreadHelper";

    private ThreadHelper() {}

    public static void pauseMillis(final long pause) {
        try {
            Thread.sleep(pause);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "InterruptedException on #pauseMillis", e);
        }
    }
}
