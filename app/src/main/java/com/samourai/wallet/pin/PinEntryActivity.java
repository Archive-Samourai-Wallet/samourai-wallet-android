package com.samourai.wallet.pin;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.TimeOutUtil;

public class PinEntryActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BuildConfig.FLAVOR.equals("staging")) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(PinEntryManager.create(this, R.layout.activity_pinentry, true)
                .install(() -> onSuccess())
                .getView());
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void onSuccess() {
        AccessFactory.getInstance(this).setIsLoggedIn(true);
        TimeOutUtil.getInstance().updatePin();
        AppUtil.getInstance(this).restartApp(getIntent().getExtras());
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data, getApplication());
    }
}
