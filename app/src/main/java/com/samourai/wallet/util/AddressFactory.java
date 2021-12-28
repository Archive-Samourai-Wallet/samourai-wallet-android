package com.samourai.wallet.util;

import android.content.Context;
import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;

import java.util.HashMap;

public class AddressFactory extends AddressFactoryGeneric {
    private static final String TAG = HD_WalletFactory.class.getSimpleName();
    private static Context context = null;
    private static AddressFactory instance = null;

    private HashMap<String,Integer> xpub2account = null;
    private HashMap<Integer,String> account2xpub = null;

    public static AddressFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new AddressFactory();
        }

        return instance;
    }

    public static AddressFactory getInstance() {
        return getInstance(null);
    }

    private AddressFactory() {
        super();
        reset();
        xpub2account = new HashMap<String,Integer>();
        account2xpub = new HashMap<Integer,String>();
    }

    public void reset() {
        Log.d(TAG, "reset");
        HD_Wallet bip44w = HD_WalletFactory.getInstance(context).get();
        HD_Wallet bip49w = BIP49Util.getInstance(context).getWallet();
        HD_Wallet bip84w = BIP84Util.getInstance(context).getWallet();
        reset(bip44w, bip49w, bip84w, SamouraiWallet.getInstance().getCurrentNetworkParams());
    }

    public HashMap<String,Integer> xpub2account()   {
        return xpub2account;
    }

    public HashMap<Integer,String> account2xpub()   {
        return account2xpub;
    }
}
