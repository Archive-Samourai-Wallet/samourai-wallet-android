package com.samourai.wallet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.onboard.OnBoardSlidesActivity;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.pin.PinEntryActivity;
import com.samourai.wallet.service.BackgroundManager;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.tor.EnumTorState;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.network.ConnectivityStatus;
import com.samourai.wallet.util.tech.LogUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity2 extends AppCompatActivity {

    private ProgressDialog progress = null;
    public static final String ACTION_RESTART = "com.samourai.wallet.MainActivity2.RESTART_SERVICE";
    private AlertDialog.Builder dlg;
    private boolean pinEntryActivityLaunched = false;
    private static final String TAG = "MainActivity2";
    private TextView loaderTxView;
    private LinearProgressIndicator progressIndicator;
    private CompositeDisposable compositeDisposables = new CompositeDisposable();
    private SwitchCompat netSwitch;
    private TextView mainnetText;
    private TextView testnetText;

    protected BroadcastReceiver receiver_restart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_RESTART.equals(intent.getAction())) {

//                ReceiversUtil.getInstance(MainActivity2.this).initReceivers();

                WebSocketService.restartService(MainActivity2.this);

            }

        }
    };

    protected BackgroundManager.Listener bgListener = new BackgroundManager.Listener() {

        public void onBecameForeground() {
//
//            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
//            intent.putExtra("notifTx", false);
//            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(intent);
//
//            Intent _intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
//            LocalBroadcastManager.getInstance(MainActivity2.this.getApplicationContext()).sendBroadcast(_intent);

        }

        public void onBecameBackground() {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            PayloadUtil.getInstance(MainActivity2.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + AccessFactory.getInstance(MainActivity2.this).getPIN()));
                        } catch (Exception e) {
                            ;
                        }

                    }
                }).start();
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        loaderTxView = findViewById(R.id.loader_text);
        progressIndicator = findViewById(R.id.loader);


        if (PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.TESTNET, false) == true) {
            SamouraiWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
        }

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        BackgroundManager.get(MainActivity2.this).addListener(bgListener);
//        }

        startApp();
    }

    private void startApp() {

        if (SamouraiTorManager.INSTANCE.isRequired() && !AppUtil.getInstance(getApplicationContext()).isOfflineMode() && ConnectivityStatus.hasConnectivity(getApplicationContext()) && !SamouraiTorManager.INSTANCE.isConnected()) {
            loaderTxView.setText(getText(R.string.initializing_tor));
            progressIndicator.setIndeterminate(false);
            progressIndicator.setMax(100);
            SamouraiTorManager.INSTANCE.getTorStateLiveData().observe(this, torState -> {
                progressIndicator.setProgressCompat(torState.getProgressIndicator(),true);
                if (torState.getState() == EnumTorState.ON) {
                    initAppOnCreate();
                    progressIndicator.setVisibility(View.GONE);
                    progressIndicator.setIndeterminate(true);
                    progressIndicator.setVisibility(View.VISIBLE);
                }
            });

        } else {
            initAppOnCreate();
        }

    }

    private void initAppOnCreate() {
        if (AppUtil.getInstance(MainActivity2.this).isOfflineMode() &&
                !(AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists())) {
            Toast.makeText(MainActivity2.this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
            doAppInit0(false, null, null);
        } else {
//            SSLVerifierThreadUtil.getInstance(MainActivity2.this).validateSSLThread();
//            APIFactory.getInstance(MainActivity2.this).validateAPIThread();

            String action = getIntent().getAction();
            String scheme = getIntent().getScheme();
            String strUri = null;
            boolean isDial = false;
//                String strUri = null;
            String strPCode = null;
            if (action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
                strUri = getIntent().getData().toString();
            } else {
                Bundle extras = getIntent().getExtras();
                if (extras != null && extras.containsKey("dialed")) {
                    isDial = extras.getBoolean("dialed");
                }
                if (extras != null && extras.containsKey("uri")) {
                    strUri = extras.getString("uri");
                }
                if (extras != null && extras.containsKey("pcode")) {
                    strPCode = extras.getString("pcode");
                }
            }

            if ( scheme !=null && scheme.equals("auth47") && getIntent().getData()!=null) {
                strUri = getIntent().getData().toString();
            }
            doAppInit0(isDial, strUri, strPCode);

        }

    }

    @Override
    protected void onResume() {
        if (PrefsUtil.getInstance(this).getValue(PrefsUtil.ENABLE_TOR, false)
                && !PrefsUtil.getInstance(this).getValue(PrefsUtil.OFFLINE,false)
                && !SamouraiTorManager.INSTANCE.isConnected()) {

            SamouraiTorManager.INSTANCE.start();
            SamouraiTorManager.INSTANCE.getTorStateLiveData().observe(this, torState -> {
                if (torState.getState() == EnumTorState.ON) {
                    initAppOnResume();
                }
            });
        } else {
            initAppOnResume();
        }
        super.onResume();

    }

    private void initAppOnResume() {

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(true);

        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

        IntentFilter filter_restart = new IntentFilter(ACTION_RESTART);
        LocalBroadcastManager.getInstance(MainActivity2.this).registerReceiver(receiver_restart, filter_restart);
        String strUri = null;
        try {
            String action = getIntent().getAction();
            String scheme = getIntent().getScheme();
            if (action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
                strUri = getIntent().getData().toString();
            } else {
                Bundle extras = getIntent().getExtras();

                if (extras != null && extras.containsKey("uri")) {
                    strUri = extras.getString("uri");
                }
            }

            if ( scheme !=null && scheme.equals("auth47") && getIntent().getData()!=null) {
                strUri = getIntent().getData().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        doAppInit0(false, strUri, null);

    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(MainActivity2.this).unregisterReceiver(receiver_restart);

        AppUtil.getInstance(MainActivity2.this).setIsInForeground(false);
    }

    @Override
    protected void onDestroy() {
        compositeDisposables.dispose();
        AppUtil.getInstance(MainActivity2.this).deleteQR();
        AppUtil.getInstance(MainActivity2.this).deleteBackup();

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        BackgroundManager.get(this).removeListener(bgListener);
//        }

        super.onDestroy();
    }

    private void initDialog() {
        Intent intent = new Intent(MainActivity2.this, OnBoardSlidesActivity.class);
        startActivity(intent);
        finish();
    }

    private void validatePIN(String strUri) {
        if (!pinEntryActivityLaunched) {

            if (AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
                return;
            }

            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            Intent intent = new Intent(MainActivity2.this, PinEntryActivity.class);
            if (strUri != null) {
                intent.putExtra("uri", strUri);
                PrefsUtil.getInstance(MainActivity2.this).setValue("SCHEMED_URI", strUri);
            }
            if (getBundleExtras() != null) {
                intent.putExtras(getBundleExtras());
            }
            startActivity(intent);
            finish();
            pinEntryActivityLaunched = true;

        }
    }

    private void launchFromDialer(final String pin) {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        progress = new ProgressDialog(MainActivity2.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    PayloadUtil.getInstance(MainActivity2.this).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getGUID() + pin));

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(true);
                    TimeOutUtil.getInstance().updatePin();
                    AppUtil.getInstance(MainActivity2.this).restartApp();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } catch (DecoderException de) {
                    de.printStackTrace();
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

                Looper.loop();

            }
        }).start();

    }

    private void doAppInit0(final boolean isDial, final String strUri, final String strPCode) {

        Disposable disposable = Completable.fromCallable(() -> {

            if (!APIFactory.getInstance(MainActivity2.this).APITokenRequired()) {
                doAppInit1(isDial, strUri, strPCode);
                return false;
            }
            boolean needToken = false;
            if (APIFactory.getInstance(MainActivity2.this).getAccessToken() == null) {
                needToken = true;
            } else {
                JWT jwt = new JWT(APIFactory.getInstance(MainActivity2.this).getAccessToken());
                if (jwt.isExpired(APIFactory.getInstance(MainActivity2.this).getAccessTokenRefresh())) {
                    APIFactory.getInstance(MainActivity2.this).getToken(true, false);
                    needToken = true;
                }
            }
            if (needToken && !AppUtil.getInstance(MainActivity2.this).isOfflineMode()) {

                APIFactory.getInstance(MainActivity2.this).stayingAlive();

                doAppInit1(isDial, strUri, strPCode);

                return true;

            } else {
                doAppInit1(isDial, strUri, strPCode);
            }
            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                }, e -> {
                    LogUtil.error(TAG, e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
        compositeDisposables.add(disposable);


    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }

    private Bundle getBundleExtras() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return null;
        }
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getScheme() != null && getIntent().getScheme().equals("bitcoin")) {
            bundle.putString("uri", getIntent().getData().toString());
        } else {
            if (bundle.containsKey("uri")) {
                bundle.putString("uri", bundle.getString("uri"));
            }
        }
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getScheme() != null && getIntent().getScheme().equals("auth47")) {
            bundle.putString("auth47", getIntent().getData().toString());
        }
        return bundle;

    }

    private void doAppInit1(boolean isDial, final String strUri, final String strPCode) {

        if (AccessFactory.getInstance(MainActivity2.this).getGUID().length() < 1 || !PayloadUtil.getInstance(MainActivity2.this).walletFileExists()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            if (AppUtil.getInstance(MainActivity2.this).isSideLoaded()) {
                runOnUiThread(this::doSelectNet);
            } else {
                runOnUiThread(this::initDialog);
            }
        } else if (isDial && AccessFactory.getInstance(MainActivity2.this).validateHash(PrefsUtil.getInstance(MainActivity2.this).getValue(PrefsUtil.ACCESS_HASH, ""), AccessFactory.getInstance(MainActivity2.this).getGUID(), new CharSequenceX(AccessFactory.getInstance(MainActivity2.this).getPIN()), AESUtil.DefaultPBKDF2Iterations)) {
            TimeOutUtil.getInstance().updatePin();
            launchFromDialer(AccessFactory.getInstance(MainActivity2.this).getPIN());
        } else if (TimeOutUtil.getInstance().isTimedOut()) {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri);
        } else if (AccessFactory.getInstance(MainActivity2.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
            TimeOutUtil.getInstance().updatePin();

            Intent intent = new Intent(MainActivity2.this, BalanceActivity.class);
            intent.putExtra("notifTx", true);
            intent.putExtra("fetch", true);
            if(strUri != null){
                intent.putExtra("uri", strUri);
            }
            if (getBundleExtras() != null) {
                intent.putExtras(getBundleExtras());
            }
            startActivity(intent);
        } else {
            AccessFactory.getInstance(MainActivity2.this).setIsLoggedIn(false);
            validatePIN(strUri == null ? null : strUri);
        }

    }

    private void doSelectNet() {
        if (dlg != null) {
            return;
        }
        dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.select_network)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    if(netSwitch.isChecked()) { //MAINNET SELECTION
                        dialog.dismiss();
                        PrefsUtil.getInstance(MainActivity2.this).removeValue(PrefsUtil.TESTNET);
                        SamouraiWallet.getInstance().setCurrentNetworkParams(MainNetParams.get());
                        initDialog();
                    }
                    else { // TESTNET SELECTION
                        dialog.dismiss();
                        doCheckTestnet();
                    }
                });
        if (!isFinishing()) {
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.net_selection,null);
            netSwitch = view.findViewById(R.id.switch1);
            mainnetText = view.findViewById(R.id.text_mainnet);
            mainnetText.setTextColor(Color.parseColor("#0CA9F4"));
            testnetText = view.findViewById(R.id.text_testnet);
            netSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    mainnetText.setTextColor(Color.parseColor("#03A9F4"));
                    testnetText.setTextColor(getResources().getColor(R.color.white));
                }
                else {
                    testnetText.setTextColor(Color.parseColor("#00BFA5"));
                    mainnetText.setTextColor(getResources().getColor(R.color.white));
                }
            });
            dlg.setView(view);
            dlg.show();
        }

    }

    private void doCheckTestnet() {
        AlertDialog testnetDlg = new AlertDialog.Builder(this)
                .setTitle("Samourai")
                .setMessage(R.string.confirm_testnet_message)
                .setCancelable(false)
                .setNegativeButton("BACK", (dialog12, whichButton12) -> {
                    dialog12.dismiss();
                    dlg = null;
                    doSelectNet();
                })
                .setPositiveButton("YES", (dialog1, whichButton1) -> {
                    PrefsUtil.getInstance(MainActivity2.this).setValue(PrefsUtil.TESTNET, true);
                    SamouraiWallet.getInstance().setCurrentNetworkParams(TestNet3Params.get());
                    initDialog();
                }).show();
    }
}
