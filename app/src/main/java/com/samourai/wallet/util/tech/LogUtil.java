package com.samourai.wallet.util.tech;

import android.util.Log;

import com.samourai.wallet.BuildConfig;
import com.samourai.whirlpool.client.utils.ClientUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class LogUtil {
    private static final String TAG = "LogUtil";

    public static void debug(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, message);
        }
    }

    public static void info(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, message);
        }
    }

    public static void error(final String tag, String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, message);
        }
    }

    public static void error(final String tag, Exception exception) {
        if (BuildConfig.DEBUG) {
            if (StringUtils.isNotBlank(exception.getMessage())) {
                android.util.Log.e(tag, exception.getMessage());
            } else {
                android.util.Log.e(tag, "no error message", exception);
            }
        }
    }
    public static void error(final String tag, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            if (StringUtils.isNotBlank(throwable.getMessage())) {
                android.util.Log.e(tag, throwable.getMessage());
            } else {
                android.util.Log.e(tag, "no error message", throwable);
            }
        }
    }

    public static void setLoggersDebug() {
        // skip noisy logs
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.ERROR);

        // enable debug logs for external Samourai libraries...
        ((Logger) LoggerFactory.getLogger("com.samourai")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.wallet")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.soroban")).setLevel(Level.TRACE);
        ((Logger) LoggerFactory.getLogger("com.samourai.whirlpool")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.xmanager")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("com.samourai.wallet.staging")).setLevel(Level.DEBUG);

        // set whirlpool log level
        Level level = BuildConfig.DEBUG ? Level.DEBUG : Level.WARN;
        ClientUtils.setLogLevel(level.toString());

        Log.d("LogUtil", "Debug logs enabled");
    }

    /**
     * Debug large strings to avoid log truncation.
     * @param tag
     * @param content
     */
    public static void debugLarge(String tag, String content) {
        if (content.length() > 4000) {
            Log.d(tag, content.substring(0, 4000));
            debugLarge(tag, content.substring(4000));
        } else {
            Log.d(tag, content);
        }
    }

}
