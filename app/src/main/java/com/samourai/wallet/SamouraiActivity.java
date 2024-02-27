package com.samourai.wallet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.tech.LogUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


@SuppressLint("Registered")
public class SamouraiActivity extends AppCompatActivity {

    protected int account = 0;
    private boolean switchThemes = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private static final String TAG = "SamouraiActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            if (getIntent().getExtras().getInt("_account") == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                account = WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix();
            }
        }
        setUpTheme();
    }

    // todo remove this
    private void setUpTheme() {
        if (switchThemes)
            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                setTheme(R.style.Theme_Samourai_Whirlpool_Material);
            }
    }


    protected void saveState() {
        if(TimeOutUtil.getInstance().isTimedOut()){
            return;
        }
        Disposable disposable = Observable.fromCallable(() -> {
            PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance(getApplicationContext()).getPIN()));
            return true;
        })      .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe((aBoolean) -> {
                },throwable -> {
                    LogUtil.error(TAG,throwable);
                });
        registerDisposable(disposable);
    }

    public boolean registerDisposable(Disposable disposable) {
        return compositeDisposable.add(disposable);
    }


    @Override
    protected void onDestroy() {
        if (! compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        super.onDestroy();
    }

    public void setSwitchThemes(boolean switchThemes) {
        this.switchThemes = switchThemes;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data, getApplication());
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void hidekeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void setupThemes() {
        if (account == SamouraiAccountIndex.POSTMIX) {
            setTheme(R.style.Theme_Samourai_Whirlpool_Material);
        }
    }

    public void setNavigationBarColor(final int resourceColorValue) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(resourceColorValue);
        }
    }
    public int getNavigationBarColor() {
        return getWindow().getNavigationBarColor();
    }

    public void setStatusBarColor(final int color) {
        getWindow().setStatusBarColor(color);
    }
    public int getStatusBarColor() {
        return getWindow().getStatusBarColor();
    }

    public int getAccount() {
        return account;
    }
}
