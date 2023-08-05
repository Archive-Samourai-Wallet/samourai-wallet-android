package com.samourai.wallet.service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.NotificationsFactory;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import static com.samourai.wallet.util.LogUtil.debug;
import static com.samourai.wallet.util.LogUtil.error;
import static com.samourai.wallet.util.LogUtil.info;

public class WebSocketHandler {
    private static final String URL_INV = "/inv";
    private static final int RECONNECT_DELAY_MS = 30000;
    private WebSocket webSocket = null;
    private boolean webSocketConnected = false;

    private String[] addrs = null;

    private static List<String> seenHashes = new ArrayList<String>();

    private static Context context = null;

    public WebSocketHandler(Context ctx, String[] addrs) {
        this.context = ctx;
        this.addrs = addrs;
    }

    public void send(String message) {

        try {
            if (isConnected()) {
                debug("WebSocketHandler", "send => " + message);
                webSocket.send(message);
            } else {
                error("WebSocketHandler", "could not send message (not connected) => " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void subscribe() {

        send("{\"op\":\"blocks_sub\"}");

        for(int i = 0; i < addrs.length; i++) {
            if(addrs[i] != null && addrs[i].length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\""+ addrs[i] + "\"}");
            }
        }

    }

    public boolean isConnected() {
        return  webSocket != null && webSocketConnected;
    }

    public void stop() {
        debug("WebSocketHandler", "stop()");

        if(isConnected()) {
            webSocket.close(1000, "Goodbye");
        }
    }

    public void start() {
        debug("WebSocketHandler", "start()");

        try {
            stop();
            connect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void connect() {
        info("WebSocketHandler", "connect()");
        new ConnectionTask().execute();
    }

    private void updateBalance(final String rbfHash, final String blkHash)    {
        /*new Thread() {
            public void run() {

                Looper.prepare();

//                Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
//                intent.putExtra("rbf", rbfHash);
//                intent.putExtra("notifTx", true);
//                intent.putExtra("fetch", true);
//                intent.putExtra("hash", blkHash);
//                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Looper.loop();

            }
        }.start();*/
    }

    private void updateReceive(final String address)    {
        new Thread() {
            public void run() {

                Looper.prepare();

                Intent intent = new Intent("com.samourai.wallet.ReceiveFragment.REFRESH");
                intent.putExtra("received_on", address);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Looper.loop();

            }
        }.start();
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... args) {

            try {
                boolean testnet = SamouraiWallet.getInstance().isTestNet();
                boolean onion = TorManager.INSTANCE.isRequired();
                String wsUrl = BackendServer.get(testnet).getBackendUrl(onion)+URL_INV;

                debug("WebSocketHandler", "doInBackground(): connecting WS: "+wsUrl);
                OkHttpClient client = WebUtil.getInstance(context).httpClientBuilder(wsUrl).build();
                Request request = new Request.Builder().url(wsUrl).build();
                webSocket = client.newWebSocket(request, computeWebSocketListener());
            }
            catch(Exception e)	{
                error("WebSocketHandler", "WS connexion failed");
                e.printStackTrace();
                reconnectOnDisconnect();
            }

            return null;
        }
    }

    private WebSocketListener computeWebSocketListener() {
        return new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                debug("WebSocketHandler", "onOpen: "+response);
                webSocketConnected = true;
                subscribe();
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                error("WebSocketHandler", "onFailure: "+response);
                error("WebSocketHandler", t);
                webSocketConnected = false;
                reconnectOnDisconnect();
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                debug("WebSocketHandler", "onClosed: code="+code+" "+reason);
                webSocketConnected = false;
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String message) {
                debug("WebSocketHandler", "onTextMessage: "+message);
                try {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(message);
                    } catch (JSONException je) {
                        error("WebSocketHandler", "JSONException:" + je.getMessage());
                        jsonObject = null;
                    }

                    if (jsonObject == null) {
                        error("WebSocketHandler", "jsonObject is null");
                        return;
                    }

                    String op = (String) jsonObject.get("op");

                    if(op.equals("block") && jsonObject.has("x"))    {

                        JSONObject objX = (JSONObject) jsonObject.get("x");

                        String hash = null;

                        if (objX.has("hash")) {
                            hash = objX.getString("hash");
                            if(seenHashes.contains(hash)){
                                return;
                            }
                            else    {
                                seenHashes.add(hash);
                            }
                        }

                        if (objX.has("height")) {
                            long height = objX.getLong("height");
                            debug("WebSocketHandler", "new block => " + height);

                            if(height > 0L) {
                                APIFactory.getInstance(context).setLatestBlockHeight(height);
//                                                info("WebSocketHandler", "set latest block height to:" + height);
                            }
                        }

                        updateBalance(null, hash);

                        return;
                    }

                    if (op.equals("utx") && jsonObject.has("x")) {

                        JSONObject objX = (JSONObject) jsonObject.get("x");

                        long value = 0L;
                        long total_value = 0L;
                        long ts = 0L;
                        String in_addr = null;
                        String out_addr = null;
                        String hash = null;

                        boolean isRBF = false;

                        if (objX.has("time")) {
                            ts = objX.getLong("time");
                        }

                        if (objX.has("hash")) {
                            hash = objX.getString("hash");
                        }

                        if (objX.has("inputs")) {
                            JSONArray inputArray = (JSONArray) objX.get("inputs");
                            JSONObject inputObj = null;
                            for (int j = 0; j < inputArray.length(); j++) {
                                inputObj = (JSONObject) inputArray.get(j);

                                if (inputObj.has("prev_out")) {
                                    JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                                    if (prevOutObj.has("value")) {
                                        value = prevOutObj.getLong("value");
                                    }
                                    if (prevOutObj.has("xpub")) {
                                        total_value -= value;
                                    }
                                    else if (prevOutObj.has("addr")) {
                                        if (in_addr == null) {
                                            in_addr = (String) prevOutObj.get("addr");
                                        }
                                        else {
                                            ;
                                        }
                                    }
                                    else {
                                        ;
                                    }
                                }
                            }
                        }

                        if (objX.has("out")) {
                            JSONArray outArray = (JSONArray) objX.get("out");
                            JSONObject outObj = null;
                            for (int j = 0; j < outArray.length(); j++) {
                                outObj = (JSONObject) outArray.get(j);
                                if (outObj.has("value")) {
                                    value = outObj.getLong("value");
                                }
                                if((outObj.has("addr") && BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr")) != null) ||
                                        (outObj.has("pubkey") && BIP47Meta.getInstance().getPCode4Addr(outObj.getString("pubkey")) != null))   {
                                    total_value += value;
                                    out_addr = outObj.getString("addr");
                                    info("WebSocketHandler", "received from " + out_addr);

                                    String pcode = BIP47Meta.getInstance().getPCode4Addr(outObj.getString("addr"));
                                    int idx = BIP47Meta.getInstance().getIdx4Addr(outObj.getString("addr"));
                                    if(outObj.has("pubkey"))    {
                                        idx = BIP47Meta.getInstance().getIdx4Addr(outObj.getString("pubkey"));
                                        pcode = BIP47Meta.getInstance().getPCode4Addr(outObj.getString("pubkey"));
                                    }
                                    if(pcode != null && idx > -1)    {

                                        SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                        String strTS = sd.format(ts * 1000L);
                                        String event = strTS + " " + context.getString(R.string.received) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) total_value / 1e8) + " BTC";
                                        BIP47Meta.getInstance().setLatestEvent(pcode, event);
                                        List<String> _addrs = new ArrayList<String>();

                                        idx++;
                                        for(int i = idx; i < (idx + BIP47Meta.INCOMING_LOOKAHEAD); i++)   {
                                            info("WebSocketHandler", "receive from " + i + ":" + BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                                            BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), i);
                                            BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), pcode.toString());

                                            _addrs.add(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                                        }

                                        idx--;
                                        if(idx >= 2)    {
                                            for(int i = idx; i >= (idx - (BIP47Meta.INCOMING_LOOKAHEAD - 1)); i--)   {
                                                info("WebSocketHandler", "receive from " + i + ":" + BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                                                BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), i);
                                                BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i), pcode.toString());

                                                _addrs.add(BIP47Util.getInstance(context).getReceivePubKey(new PaymentCode(pcode), i));
                                            }
                                        }

                                        addrs = _addrs.toArray(new String[_addrs.size()]);

                                        start();

                                    }
                                }
                                else if(outObj.has("addr"))   {
                                    info("WebSocketHandler", "addr:" + outObj.getString("addr"));
                                    if(outObj.has("xpub") && outObj.getJSONObject("xpub").has("path") && outObj.getJSONObject("xpub").getString("path").startsWith("M/1/"))    {
                                        return;
                                    }
                                    else    {
                                        total_value += value;
                                        out_addr = outObj.getString("addr");
                                    }
                                }
                                else    {
                                    ;
                                }
                            }
                        }

                        String title = context.getString(R.string.app_name);
                        if (total_value > 0L) {

                            isRBF = checkForRBF(hash);

                            String marquee = context.getString(R.string.received_bitcoin) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) total_value / 1e8) + " BTC";
                            if (in_addr !=null && in_addr.length() > 0) {
                                marquee += " from " + in_addr;
                            }

                            NotificationsFactory.getInstance(context).setNotification(title, marquee, marquee, R.mipmap.ic_launcher, MainActivity2.class, 1000);
                        }

                        updateBalance(isRBF ? hash : null, null);

                        if(out_addr != null)    {
                            updateReceive(out_addr);
                        }

                    }
                    else {
                        ;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void reconnectOnDisconnect() {
        error("WebSocketHandler", "Retrying to connect to WS in "+RECONNECT_DELAY_MS+"ms");
        // auto-reconnect after delay
        try {
            synchronized (this) {
                Thread.sleep(RECONNECT_DELAY_MS);
            }
        } catch (InterruptedException e){}
        start();
    }

    private synchronized boolean checkForRBF(String hash)  {

        boolean ret = false;

        try {
            JSONObject obj = APIFactory.getInstance(context).getTxInfo(hash);
            if(obj != null && obj.has("inputs"))    {
                JSONArray inputs = obj.getJSONArray("inputs");
                for(int i = 0; i < inputs.length(); i++)   {
                    JSONObject inputObj = inputs.getJSONObject(i);
                    if(inputObj.has("seq"))    {
                        long sequence = inputObj.getLong("seq");
                        if(BigInteger.valueOf(sequence).compareTo(SamouraiWallet.RBF_SEQUENCE_VAL) <= 0)    {
                            ret = true;
                            debug("WebSocketHandler", "return value:" + ret);
                            break;
                        }
                    }
                }
            }
        }
        catch(JSONException je) {
            ;
        }

        return ret;
    }

}
