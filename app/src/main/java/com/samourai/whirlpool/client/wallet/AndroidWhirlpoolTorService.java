package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.tor.ITorManager;
import com.samourai.tor.client.TorClientService;

import io.matthewnelson.topl_service.TorServiceController;

public class AndroidWhirlpoolTorService extends TorClientService {
    private ITorManager torManager;

    public AndroidWhirlpoolTorService(ITorManager torManager) {
        super();
        this.torManager = torManager;
    }

    @Override
    public void changeIdentity() {
        if (torManager.isRequired()) {
            TorServiceController.newIdentity();
        }
    }
}
