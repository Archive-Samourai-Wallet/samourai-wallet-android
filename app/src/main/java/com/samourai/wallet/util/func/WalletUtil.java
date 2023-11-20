package com.samourai.wallet.util.func;

import android.content.Context;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;

public class WalletUtil {

    private WalletUtil() {}

    public static boolean saveWallet(final Context context) {
        try {
            PayloadUtil.getInstance(context)
                    .saveWalletToJSON(new CharSequenceX(
                            AccessFactory.getInstance(context).getGUID() +
                                    AccessFactory.getInstance(context).getPIN()));
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
