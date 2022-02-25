package com.samourai.stomp.client;

import com.samourai.wallet.tor.ITorManager;

public class AndroidStompClientService implements IStompClientService {
    private ITorManager torManager;

    public AndroidStompClientService(ITorManager torManager) {
        this.torManager = torManager;
    }

    @Override
    public IStompClient newStompClient() {
        return new AndroidStompClient(torManager);
    }
}
