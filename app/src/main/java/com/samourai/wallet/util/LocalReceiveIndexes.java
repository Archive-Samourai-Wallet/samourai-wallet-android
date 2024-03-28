package com.samourai.wallet.util;

import android.content.Context;

import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.util.func.AddressFactory;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalReceiveIndexes {

    private static LocalReceiveIndexes instance = null;

    private static Context context = null;

    private LocalReceiveIndexes() { ; }

    public static LocalReceiveIndexes getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new LocalReceiveIndexes();
        }

        return instance;
    }

    public JSONObject toJSON() {

        JSONObject indexes = new JSONObject();

        try {
            indexes.put("local44idx", AddressFactory.getInstance(context).getIndex(WALLET_INDEX.BIP44_RECEIVE));
            indexes.put("local49idx", AddressFactory.getInstance(context).getIndex(WALLET_INDEX.BIP49_RECEIVE));
            indexes.put("local84idx", AddressFactory.getInstance(context).getIndex(WALLET_INDEX.BIP84_RECEIVE));
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

        return indexes;
    }

    public void fromJSON(JSONObject obj) {

        try {
            if(obj.has("local44idx")) {
                AddressFactory.getInstance(context).setWalletIdx(WALLET_INDEX.BIP44_RECEIVE, obj.getInt("local44idx"), true);
            }
            if(obj.has("local49idx")) {
                AddressFactory.getInstance(context).setWalletIdx(WALLET_INDEX.BIP49_RECEIVE, obj.getInt("local49idx"), true);
            }
            if(obj.has("local84idx")) {
                AddressFactory.getInstance(context).setWalletIdx(WALLET_INDEX.BIP84_RECEIVE, obj.getInt("local84idx"), true);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
