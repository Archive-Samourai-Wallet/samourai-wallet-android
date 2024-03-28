package com.samourai.wallet.util.func;

import android.content.Context;
import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.util.AddressFactoryGeneric;

import org.apache.commons.lang3.tuple.Pair;

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

    @Override
    public void reset() {
        Log.d(TAG, "reset");
        HD_Wallet bip44w = HD_WalletFactory.getInstance(context).get();
        HD_Wallet bip49w = BIP49Util.getInstance(context).getWallet();
        HD_Wallet bip84w = BIP84Util.getInstance(context).getWallet();
        reset(bip44w, bip49w, bip84w, SamouraiWallet.getInstance().getCurrentNetworkParams());
    }

    public String debugConsistency() {
        StringBuilder sb = new StringBuilder();

        // check bip44
        Pair<Integer, String> bip44Mine = getAddress(WALLET_INDEX.BIP44_RECEIVE);
        String bip44External = HD_WalletFactory.getInstance(context).get().getAddressAt(0, 0, bip44Mine.getLeft()).getAddressString();
        doDebugConsistency(bip44Mine.getRight(), bip44External, "HD_WalletFactory", sb);

        // check bip49
        Pair<Integer, String> bip49Mine = getAddress(WALLET_INDEX.BIP49_RECEIVE);
        String bip49External = BIP49Util.getInstance(context).getAddressAt(0, bip49Mine.getLeft()).getAddressAsString();
        doDebugConsistency(bip49Mine.getRight(), bip49External, "BIP49Util", sb);

        // check bip84
        Pair<Integer, String> bip84Mine = getAddress(WALLET_INDEX.BIP84_RECEIVE);
        String bip84External = BIP84Util.getInstance(context).getAddressAt(0, bip84Mine.getLeft()).getBech32AsString();
        doDebugConsistency(bip84Mine.getRight(), bip84External, "BIP84Util", sb);

        return sb.toString();
    }

    private void doDebugConsistency(String addressMine, String addressExternal, String nameExternal, StringBuilder sb) {
        sb.append(nameExternal+": ");
        if (addressMine.equals(addressExternal)) {
            sb.append("OK\n");
        } else {
            sb.append("KO!\n");
            sb.append("mine="+addressMine+"\n");
            sb.append(nameExternal+"="+addressExternal+"\n");
        }
    }

    public HashMap<String,Integer> xpub2account()   {
        return xpub2account;
    }

    public HashMap<Integer,String> account2xpub()   {
        return account2xpub;
    }
}
