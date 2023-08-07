package com.samourai.wallet.util.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.samourai.wallet.explorer.ExplorerActivity;

public class ActivityHelper {

    private ActivityHelper() {
    }

    public static void launchSupportPageInBrowser(final Activity activity,
                                                  final boolean connectedOnTor) {

        final String url = getSupportHttpUrl(connectedOnTor);
        final Intent explorerIntent = new Intent(activity, ExplorerActivity.class);
        explorerIntent.putExtra(ExplorerActivity.SUPPORT, url);
        activity.startActivity(explorerIntent);
    }

    @NonNull
    private static String getSupportHttpUrl(boolean connectedOnTor) {
        if (connectedOnTor) {
            return "http://72typmu5edrjmcdkzuzmv2i4zqru7rjlrcxwtod4nu6qtfsqegngzead.onion/support";
        }
        return "https://samouraiwallet.com/support";
    }
}
