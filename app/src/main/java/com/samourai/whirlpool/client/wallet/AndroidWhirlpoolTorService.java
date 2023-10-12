package com.samourai.whirlpool.client.wallet;

import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.tor.SamouraiTorManager;

public class AndroidWhirlpoolTorService extends TorClientService {

    public AndroidWhirlpoolTorService() {
        super();
    }

    @Override
    public void changeIdentity() {
        if (SamouraiTorManager.INSTANCE.isRequired()) {
            SamouraiTorManager.newIdentity();
        }
    }
}
