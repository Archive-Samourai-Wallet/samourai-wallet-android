package com.samourai.wallet.tor;

import java.net.Proxy;

public class MockTorManager implements ITorManager {
    public MockTorManager() {

    }

    @Override
    public Boolean isRequired() {
        return false;
    }

    @Override
    public Proxy getProxy() {
        return null;
    }
}
