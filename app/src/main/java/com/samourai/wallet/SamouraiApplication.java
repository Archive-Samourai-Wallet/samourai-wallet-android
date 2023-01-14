package com.samourai.wallet;


import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.stealth.StealthModeController;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ConnectionChangeReceiver;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.squareup.picasso.Picasso;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.plugins.RxJavaPlugins;

public class SamouraiApplication extends Application {

    public static String TOR_CHANNEL_ID = "TOR_CHANNEL";
    public static String FOREGROUND_SERVICE_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL_ID";
    public static String WHIRLPOOL_CHANNEL = "WHIRLPOOL_CHANNEL";
    public static String WHIRLPOOL_NOTIFICATIONS = "WHIRLPOOL_NOTIFICATIONS";

    @Override
    public void onCreate() {
        super.onCreate();
        ExceptionReportHandler.Companion.attach(this);
        setUpTorService();
        setUpChannels();
        registerNetworkCallBack();
        RxJavaPlugins.setErrorHandler(throwable -> {
        });
        ExternalBackupManager.attach(this);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        if (BuildConfig.DEBUG || BuildConfig.FLAVOR.equals("staging")) {
            Picasso.get().setIndicatorsEnabled(true);
            LogUtil.setLoggersDebug();
        }
        StealthModeController.INSTANCE.fixStealthModeIfNotActive(getApplicationContext());
    }

    private void registerNetworkCallBack() {
        ConnectionChangeReceiver changeReceiver = new ConnectionChangeReceiver(getApplicationContext());
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(ConnectivityManager.class);
            connectivityManager.requestNetwork(networkRequest, changeReceiver.getNetworkCallback());
        }
        AppUtil.getInstance(getApplicationContext()).checkOfflineState();
    }

    public void startService() {
        TorServiceController.startTor();
    }

    private void setUpChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel whirlpoolChannel = new NotificationChannel(
                    WHIRLPOOL_CHANNEL,
                    "Whirlpool service ",
                    NotificationManager.IMPORTANCE_LOW
            );
            whirlpoolChannel.enableLights(false);
            whirlpoolChannel.enableVibration(false);
            whirlpoolChannel.setSound(null, null);

            NotificationChannel serviceChannel = new NotificationChannel(
                    TOR_CHANNEL_ID,
                    "Tor service ",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);

            NotificationChannel refreshService = new NotificationChannel(
                    FOREGROUND_SERVICE_CHANNEL_ID,
                    "Samourai Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            refreshService.setSound(null, null);
            refreshService.setImportance(NotificationManager.IMPORTANCE_LOW);
            refreshService.setLockscreenVisibility(Notification.VISIBILITY_SECRET);


            NotificationChannel whirlpoolNotifications = new NotificationChannel(
                    WHIRLPOOL_NOTIFICATIONS,
                    "Mix status notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            whirlpoolChannel.enableLights(true);

            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(refreshService);
                manager.createNotificationChannel(whirlpoolChannel);
                manager.createNotificationChannel(whirlpoolNotifications);
            }
        }
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        ExternalBackupManager.dispose();
        TorServiceController.stopTor();
        super.onTerminate();
    }

    private void setUpTorService() {
        TorManager.INSTANCE.setUp(this);
        if(!StealthModeController.INSTANCE.isStealthEnabled(getApplicationContext())){
            if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false) && !PrefsUtil.getInstance(this).getValue(PrefsUtil.OFFLINE, false)) {
                startService();
            }
        }
    }

}
