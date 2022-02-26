package com.samourai.wallet.tor;

import java.net.Proxy;

public interface ITorManager {
    Boolean isRequired();
    Proxy getProxy();
}
