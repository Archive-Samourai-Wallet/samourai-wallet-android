package com.samourai.wallet.swaps;

import android.content.Context;

import com.samourai.wallet.constants.SamouraiAccountIndex;

import org.json.JSONException;
import org.json.JSONObject;

public class SwapsMeta {

    private static com.samourai.wallet.swaps.SwapsMeta instance = null;

    private static Context context = null;

    private SwapsMeta() { ; }

    public static com.samourai.wallet.swaps.SwapsMeta getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new com.samourai.wallet.swaps.SwapsMeta();
        }

        return instance;
    }

    public int getSwapsMainAccount() {
        return SwapsConst.SWAPS_MAIN_ACCOUNT;
    }
    public int getSwapsRefundAccount() {
        return SwapsConst.SWAPS_REFUND_ACCOUNT;
    }

}
