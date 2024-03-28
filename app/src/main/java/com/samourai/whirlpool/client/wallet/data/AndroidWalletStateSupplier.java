package com.samourai.whirlpool.client.wallet.data;

import android.content.Context;

import com.samourai.wallet.bipWallet.BipDerivation;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.constants.SamouraiAccount;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import java.util.LinkedHashMap;
import java.util.Map;

public class AndroidWalletStateSupplier implements WalletStateSupplier {
    private static AndroidWalletStateSupplier instance;

    public static synchronized AndroidWalletStateSupplier getInstance(Context ctx) {
        if (instance == null) {
            AddressFactory addressFactory = AddressFactory.getInstance(ctx);
            instance = new AndroidWalletStateSupplier(addressFactory);
        }
        return instance;
    }

    private AddressFactory addressFactory;
    private Map<String, IIndexHandler> indexHandlerWallets;

    private AndroidWalletStateSupplier(AddressFactory addressFactory) {
        this.addressFactory = addressFactory;
        this.indexHandlerWallets = new LinkedHashMap<>();
    }

    @Override
    public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
        BipDerivation bipDerivation = bipWallet.getDerivation();
        String persistKey = computePersistKeyWallet(bipWallet.getAccount(), bipDerivation.getPurpose(), chain);

        IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
        if (indexHandlerWallet == null) {
            WALLET_INDEX walletIndex = WALLET_INDEX.find(bipDerivation, chain);
            indexHandlerWallet = new AndroidWalletStateIndexHandler(addressFactory, walletIndex);
            indexHandlerWallets.put(persistKey, indexHandlerWallet);
        }
        return indexHandlerWallet;
    }

    protected String computePersistKeyWallet(SamouraiAccount account, int purpose, Chain chain) {
        return account.name() + "_" + purpose + "_" + chain.getIndex();
    }

    @Override
    public IIndexHandler getIndexHandlerExternal() {
        // ignored
        return null;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void setInitialized(boolean initialized) {
        // ignored
    }

    @Override
    public void load() throws Exception {
        // ignored
    }

    @Override
    public boolean persist(boolean force) throws Exception {
        // ignored
        return false;
    }

    @Override
    public boolean isNymClaimed() {
        return false; // not implemented
    }

    @Override
    public void setNymClaimed(boolean value) {
        // not implemented
    }
}
