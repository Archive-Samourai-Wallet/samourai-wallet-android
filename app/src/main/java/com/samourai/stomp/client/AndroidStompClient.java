package com.samourai.stomp.client;

import com.google.gson.Gson;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.MessageErrorListener;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.dto.StompCommand;
import ua.naiksoftware.stomp.dto.StompHeader;
import ua.naiksoftware.stomp.dto.StompMessage;

public class AndroidStompClient implements IStompClient {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class.getSimpleName());
    private Gson gson;
    private TorManager torManager;
    private StompClient stompClient;

    public AndroidStompClient(TorManager torManager) {
        this.gson = new Gson();
        this.torManager = torManager;
    }

    @Override
    public void connect(String url, Map<String, String> stompHeaders, final MessageErrorListener<Void, Throwable> onConnectOnDisconnectListener) {
        try {
            url += "/websocket"; // SockJS
            log.debug("connecting to " + url);
            AndroidWebSocketsConnectionProvider connectionProvider = new AndroidWebSocketsConnectionProvider(url, null, torManager);
            stompClient = new StompClient(connectionProvider);
            stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<LifecycleEvent>() {
                    @Override
                    public void accept(LifecycleEvent lifecycleEvent) {
                        if (log.isDebugEnabled()) {
                            log.debug("STOMP connect: " + lifecycleEvent.getType()+" : "+lifecycleEvent.getMessage());
                        }
                        switch (lifecycleEvent.getType()) {
                            case OPENED:
                                onConnectOnDisconnectListener.onMessage(null);
                                break;
                            case ERROR: case CLOSED:
                                Exception e = lifecycleEvent.getException();
                                if  (e == null) {
                                    e = new Exception("STOMP: "+lifecycleEvent.getType());
                                }
                                disconnect();
                                onConnectOnDisconnectListener.onError(e);
                                break;
                        }
                    }
                });
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            stompClient.connect(myHeaders);
        }catch(Exception e) {
            log.error("connect error", e);
            onConnectOnDisconnectListener.onError(new Exception("connect error"));
            throw e;
        }
    }

    @Override
    public void subscribe(Map<String, String> stompHeaders, final MessageErrorListener<IStompMessage, String> onMessageOnErrorListener) {
        try {
            String destination = getDestination(stompHeaders);
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            log.debug("subscribing " + destination);
            stompClient.topic(destination, myHeaders)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<StompMessage>() {
                        @Override
                        public void accept(StompMessage stompMessage) {
                            try {
                                String messageType = stompMessage.findHeader(WhirlpoolProtocol.HEADER_MESSAGE_TYPE);
                                String jsonPayload = stompMessage.getPayload();
                                Object objectPayload = gson.fromJson(jsonPayload, Class.forName(messageType));
                                AndroidStompMessage androidStompMessage = new AndroidStompMessage(stompMessage, objectPayload);
                                onMessageOnErrorListener.onMessage(androidStompMessage);
                            } catch(Exception e) {
                                log.error("stompClient.accept error", e);
                                onMessageOnErrorListener.onError(e.getMessage());
                            }
                        }
                    });
        }
        catch (Exception e) {
            log.error("subscribe error", e);
            onMessageOnErrorListener.onError(e.getMessage());
        }
        log.debug("subscribed");
    }

    @Override
    public void send(Map<String, String> stompHeaders, Object payload) {
        try {
            String destination = getDestination(stompHeaders);
            List<StompHeader> myHeaders = computeHeaders(stompHeaders);
            String jsonPayload = gson.toJson(payload);
            StompMessage stompMessage = new StompMessage(StompCommand.SEND, myHeaders, jsonPayload);

            log.debug("sending " + destination + ": " + jsonPayload);
            stompClient.send(stompMessage)
                    .compose(applySchedulers())
                    .subscribe(new Action() {
                        @Override
                        public void run() throws Exception {
                            // sending success
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            // sending error
                            log.debug("send: error", throwable);
                        }
                    });
        } catch(Exception e) {
            log.error("send error", e);
        }
    }

    @Override
    public void disconnect() {
        if (stompClient != null) {
            try {
                stompClient.disconnect();
            } catch(Exception e) {}
            stompClient = null;
        }
    }

    private String getDestination(Map<String, String> stompHeaders) {
        return stompHeaders.get(StompHeader.DESTINATION);
    }

    private CompletableTransformer applySchedulers() {
        return new CompletableTransformer() {
            @Override
            public CompletableSource apply(Completable upstream) {
                return upstream
                        .unsubscribeOn(Schedulers.newThread())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    private List<StompHeader> computeHeaders(Map<String,String> mapHeaders) {
        List<StompHeader> stompHeaders = new ArrayList<>();
        for (Map.Entry<String,String> entry : mapHeaders.entrySet()) {
            StompHeader stompHeader = new StompHeader(entry.getKey(), entry.getValue());
            stompHeaders.add(stompHeader);
        }
        return stompHeaders;
    }
}
