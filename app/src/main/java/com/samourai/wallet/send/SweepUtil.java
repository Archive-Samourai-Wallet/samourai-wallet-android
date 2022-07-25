package com.samourai.wallet.send;

import android.content.Context;

public class SweepUtil extends SweepUtilGeneric {
    private static Context context = null;
    private static SweepUtil instance = null;

    private SweepUtil() { super(); }

    public static SweepUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new SweepUtil();
        }

        return instance;
    }

}
