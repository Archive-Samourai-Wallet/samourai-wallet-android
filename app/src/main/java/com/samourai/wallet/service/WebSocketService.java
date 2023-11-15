package com.samourai.wallet.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.tech.AppUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.samourai.wallet.util.tech.LogUtil.debug;
//import android.util.Log;

// this services keeps connexion to WebSocket for realtime notifications
// it must be always running for blockHeight sync (required for whirlpool) and Dojo tokens in sync
public class WebSocketService extends Service {

    private Context context = null;
    private WebSocketHandler webSocketHandler = null;

    public static List<String> addrSubs = null;

    @Override
    public void onCreate() {
        debug("WebSocketService", "onCreate()");

        super.onCreate();

        //
        context = this.getApplicationContext();

        new Thread(() -> {
            Looper.prepare();

            // dojo token keepalive
            APIFactory.getInstance(context).stayingAlive();

            Looper.loop();

        }).start();

        if(HD_WalletFactory.getInstance(context).get() == null)    {
            debug("WebSocketService", "onCreate() EXIT hdWallet null");
            return;
        }

        //
        // prune BIP47 lookbehind
        //
        BIP47Meta.getInstance().pruneIncoming();

        addrSubs = new ArrayList<String>();
        addrSubs.add(AddressFactory.getInstance(context).account2xpub().get(0));
        addrSubs.add(BIP49Util.getInstance(context).getWallet().getAccount(0).xpubstr());
        addrSubs.add(BIP84Util.getInstance(context).getWallet().getAccount(0).xpubstr());
        addrSubs.addAll(Arrays.asList(BIP47Meta.getInstance().getIncomingLookAhead(context)));
        String[] addrs = addrSubs.toArray(new String[addrSubs.size()]);

        webSocketHandler = new WebSocketHandler(WebSocketService.this, addrs);
        connectToWebsocketIfNotConnected();

    }

    public static void startService(Context ctx) {
        debug("WebSocketService", "startService()");
        ctx.startService(new Intent(ctx.getApplicationContext(), WebSocketService.class));
    }

    public static void stopService(Context ctx) {
        debug("WebSocketService", "stopService()");
        if (AppUtil.getInstance(ctx.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            ctx.stopService(new Intent(ctx.getApplicationContext(), WebSocketService.class));
        }
    }

    public static void restartService(Context ctx) {
        debug("WebSocketService", "restartService()");
        stopService(ctx);
        startService(ctx);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connectToWebsocketIfNotConnected()
    {
        debug("WebSocketService", "connectToWebsocketIfNotConnected()");
        try {
            if(!webSocketHandler.isConnected()) {
                webSocketHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if(webSocketHandler != null)    {
                webSocketHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy()
    {
        debug("WebSocketService", "onDestroy()");
        stop();
        super.onDestroy();
    }

}