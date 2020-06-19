package com.samourai.wallet;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import androidx.multidex.MultiDex;

import com.samourai.wallet.tor.TorService;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.util.PrefsUtil;

import java.io.File;
import java.io.PrintWriter;

public class SamouraiApplication extends Application {

    public static String TOR_CHANNEL_ID = "TOR_CHANNEL";
    public static String FOREGROUND_SERVICE_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL_ID";
    public static String WHIRLPOOL_CHANNEL = "WHIRLPOOL_CHANNEL";
    public static String WHIRLPOOL_NOTIFICATIONS = "WHIRLPOOL_NOTIFICATIONS";

    @Override
    public void onCreate() {
        super.onCreate();
        setUpChannels();
        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false)) {
            startService();
        }
        // Write logcat output to a file
        if (BuildConfig.DEBUG) {
            try {
                String logFile = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/Samourai_debug_log.txt");
                File file = new File(logFile);
                if (file.exists()) {
                    // clear log file after accumulating more than 6mb
                    if (file.length() > 6000000) {
                        PrintWriter writer = new PrintWriter(file);
                        writer.flush();
                        writer.close();
                    }
                }
                Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void startService() {
        if (ConnectivityStatus.hasConnectivity(getApplicationContext()) && PrefsUtil.getInstance(getApplicationContext()).getValue(PrefsUtil.ENABLE_TOR, false)) {
            Intent startIntent = new Intent(this, TorService.class);
            startIntent.setAction(TorService.START_SERVICE);
            startService(startIntent);
        }
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
}
