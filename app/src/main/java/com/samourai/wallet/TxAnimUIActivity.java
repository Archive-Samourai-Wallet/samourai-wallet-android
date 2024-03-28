package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.BatchSendUtil;
import com.samourai.wallet.util.func.MonetaryUtil;
import com.samourai.wallet.util.func.RBFFactory;
import com.samourai.wallet.util.func.SendAddressUtil;
import com.samourai.wallet.util.func.SentToFromBIP47Util;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.util.view.ViewUtil;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.widgets.TransactionProgressView;

import org.bitcoinj.core.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.util.activity.ActivityHelper.gotoBalanceHomeActivity;
import static com.samourai.wallet.util.activity.ActivityHelper.launchSupportPageInBrowser;
import static com.samourai.wallet.util.func.RBFFactory.updateRBFSpendForBroadcastTxAndRegister;
import static com.samourai.wallet.util.func.SatoshiBitcoinUnitHelper.getBtcValue;
import static com.samourai.wallet.util.tech.LogUtil.debug;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TxAnimUIActivity extends SamouraiActivity {

    private static final String TAG = "TxAnimUIActivity";
    public static final int BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS = 1200;

    private TransactionProgressView progressView = null;
    private int arcdelay = 800;
    private long signDelay = 2000L;
    private long broadcastDelay = 1599L;
    private AtomicBoolean txInProgress = new AtomicBoolean(false);
    private AtomicBoolean txSuccess = new AtomicBoolean(false);

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_anim_ui);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue_send_ui));

        setNavigationBarColor(getResources().getColor(R.color.blue_send_ui));

        progressView = findViewById(R.id.transactionProgressView);
        progressView.getMainView().setBackgroundColor(getResources().getColor(R.color.blue_send_ui));
        progressView.setTheme(getTheme());
        broadcastTx();
    }

    private void broadcastTx() {
        progressView.reset();
        progressView.setTxStatusMessage(R.string.tx_creating_ok);
        progressView.getmArcProgress().setVisibility(View.VISIBLE);
        progressView.getmArcProgress().startArc1(arcdelay);
        progressView.getmCheckMark().setImageDrawable(null);
        progressView.getLeftTopImgBtn().setOnClickListener(view -> gotoBalanceHomeActivity(
                this,
                SendParams.getInstance().getAccount()));

        progressView.getOptionBtn2().setOnClickListener(view -> {});

        // make tx
        final Transaction tx = SendFactory.getInstance(this)
                .makeTransaction(
                        SendParams.getInstance().getOutpoints(),
                        SendParams.getInstance().getReceivers());

        if (tx == null) {
            failTx(R.string.tx_creating_ko);
            return;
        }

        final RBFSpend rbf = RBFFactory.createRBFSpendFromTx(tx, this);

        final List<Integer> strictModeVouts = new ArrayList<>();
        if (SendParams.getInstance().getDestAddress() != null && SendParams.getInstance().getDestAddress().compareTo("") != 0 &&
                PrefsUtil.getInstance(this).getValue(PrefsUtil.STRICT_OUTPUTS, true) == true) {
            List<Integer> idxs = SendParams.getInstance().getSpendOutputIndex(tx);
            for(int i = 0; i < tx.getOutputs().size(); i++)   {
                if(!idxs.contains(i))   {
                    strictModeVouts.add(i);
                }
            }
        }

        new Handler().postDelayed(() -> {

            TxAnimUIActivity.this.runOnUiThread(() -> {
                progressView.getmArcProgress().startArc2(arcdelay);
                progressView.setTxStatusMessage(R.string.tx_signing_ok);
            });


            final Transaction _tx = SendFactory.getInstance(TxAnimUIActivity.this)
                    .signTransaction(tx, SendParams.getInstance().getAccount());

            if (_tx == null) {
                failTx(R.string.tx_signing_ko);
                return;
            }

            final String hexTx = new String(Hex.encode(_tx.bitcoinSerialize()));
            debug("TxAnimUIActivity", "hex tx:" + hexTx);
            final String strTxHash = _tx.getHashAsString();

            new Handler().postDelayed(() -> {
                TxAnimUIActivity.this.runOnUiThread(() -> {
                    progressView.getmArcProgress().startArc3(arcdelay);
                    progressView.setTxStatusMessage(R.string.tx_broadcast_ok);
                });

                if (AppUtil.getInstance(TxAnimUIActivity.this).isBroadcastDisabled()) {
                    offlineTx(hexTx, strTxHash, R.string.tx_status_details_not_sent_offline_mode);
                    return;
                }

                TxAnimUIActivity.this.runOnUiThread(() -> {
                    txInProgress.set(true);
                });
                final Disposable disposable = pushTx(hexTx, strictModeVouts)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((jsonObject) -> {
                            txInProgress.set(false);
                            debug(TAG, jsonObject.toString());
                            if (jsonObject.getBoolean("isOk")) {
                                progressView.showCheck();
                                progressView.setTxStatusMessage(R.string.tx_sent_ok);
                                handleResult(true, rbf, strTxHash, hexTx, _tx);

                            } else if (jsonObject.getBoolean("hasReuse")) {
                                showAddressReuseWarning(rbf, strTxHash, hexTx, _tx);

                            } else {
                                // reset change index upon tx fail
                                final WALLET_INDEX changeIndex = WALLET_INDEX.findChangeIndex(
                                        SendParams.getInstance().getAccount(),
                                        SendParams.getInstance().getChangeType());
                                AddressFactory.getInstance().setWalletIdx(
                                        changeIndex,
                                        SendParams.getInstance().getChangeIdx(),
                                        true);
                                failTx(R.string.tx_status_details_try_rebroadcating);
                            }

                        }, throwable -> {
                            txInProgress.set(false);
                            failTx(R.string.tx_status_details_try_rebroadcating);
                        });

                disposables.add(disposable);

            }, broadcastDelay);
        }, signDelay);
    }

    private void editChangeUtxo(final String hexTx, final boolean lock) {

        progressView.getOptionProgressBar().setVisibility(View.VISIBLE);

        final Disposable disposable = Single.fromCallable(() -> {
                    APIFactory.getInstance(this).initWallet();
                    return true;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {

                    final UTXOFactory utxoFact = UTXOFactory.getInstance(this);
                    final SendParams sendParams = SendParams.getInstance();
                    if (utxoFact.markUTXOChange(hexTx, sendParams.getAccount(), lock)) {

                        try {

                            progressView.getOptionBtn1().setOnClickListener(view ->
                                    editChangeUtxo(hexTx, !lock));

                            if (lock) {
                                progressView.showSuccessSentTxOptions(true, R.string.tx_option_change_spendable);
                            } else {
                                progressView.showSuccessSentTxOptions(true, R.string.tx_option_change_do_not_spend);
                            }

                            progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                            progressView.getLeftTopImgBtn()
                                    .setOnClickListener(view -> gotoBalanceHomeActivity(
                                            this,
                                            SendParams.getInstance().getAccount()));

                            if (lock) {
                                Toast.makeText(
                                        TxAnimUIActivity.this,
                                        getResources().getString(R.string.tx_toast_change_utxo_locked),
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                Toast.makeText(
                                        TxAnimUIActivity.this,
                                        getResources().getString(R.string.tx_toast_change_utxo_unlocked),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        } catch (final Throwable t) {
                            Log.e(TAG, t.getMessage(), t);
                            progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                            Toast.makeText(
                                    TxAnimUIActivity.this,
                                    getResources().getString(R.string.tx_toast_lock_change_utxo_issue),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {

                        Toast.makeText(
                                TxAnimUIActivity.this,
                                getResources().getString(R.string.tx_toast_no_change_utxo_found),
                                Toast.LENGTH_SHORT
                        ).show();

                        progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                        progressView.getLeftTopImgBtn()
                                .setOnClickListener(view -> gotoBalanceHomeActivity(
                                        this,
                                        SendParams.getInstance().getAccount()));
                        if (lock) {
                            progressView.showSuccessSentTxOptions(false, R.string.tx_option_change_spendable);
                        } else {
                            progressView.showSuccessSentTxOptions(false, R.string.tx_option_change_do_not_spend);
                        }
                    }

                }, throwable -> {

                    Log.e(TAG, throwable.getMessage(), throwable);
                    progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                    Toast.makeText(
                            TxAnimUIActivity.this,
                            getResources().getString(R.string.tx_toast_lock_change_utxo_issue),
                            Toast.LENGTH_SHORT
                    ).show();
                });

        disposables.add(disposable);
    }

    private String getChangeAddress() {
        return SendParams.getInstance().generateChangeAddress(this);
    }

    private void showAddressReuseWarning(
            final RBFSpend rbf,
            final String strTxHash,
            final String hexTx,
            final Transaction _tx) {

        final List<Integer> emptyList = new ArrayList<>();
        final AlertDialog.Builder dlg = new AlertDialog.Builder(TxAnimUIActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.strict_mode_address)
                .setCancelable(false)
                .setPositiveButton(R.string.broadcast, (dialog, whichButton) -> {

                    dialog.dismiss();

                    final Disposable disposable = pushTx(hexTx, emptyList)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((jsonObject) -> {
                                if (jsonObject.getBoolean("isOk")) {
                                    progressView.showCheck();
                                    progressView.setTxStatusMessage(R.string.tx_sent_ok);
                                    handleResult(true, rbf, strTxHash, hexTx, _tx);
                                } else {
                                    handleResult(false, rbf, strTxHash, hexTx, _tx);
                                }
                            }, throwable -> {
                                Log.e(TAG, throwable.getMessage(), throwable);
                                failTx(R.string.tx_status_details_try_rebroadcating);
                            });

                    disposables.add(disposable);
                })
                .setNegativeButton(R.string.strict_reformulate, (dialog, whichButton) -> {
                    dialog.dismiss();
                    // return to SendActivity
                    TxAnimUIActivity.this.finish();
                });

        dlg.show();

    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

    private Single<JSONObject> pushTx(
            final String hexTx,
            final List<Integer> strictModeVouts) {

        return Single.fromCallable(() -> {

            final JSONObject results = new JSONObject();
            results.put("isOk", false);
            results.put("hasReuse", false);
            results.put("reuseIndexes", new JSONArray());

            for (final Integer i :  strictModeVouts) {
                debug("TxAnimUIActivity", "strict mode output index:" + i);
            }

            final String response = PushTx.getInstance(TxAnimUIActivity.this)
                    .samourai(
                            hexTx,
                            (strictModeVouts != null && strictModeVouts.size() > 0)
                                    ? strictModeVouts
                                    : null);

            debug("TxAnimUIActivity", "response:" + response);

            if (response != null) {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.has("status") && jsonObject.getString("status").equals("ok")) {
                    results.put("isOk", true);
                } else if (jsonObject.has("status") && jsonObject.getString("status").equals("error") &&
                        jsonObject.has("error") && jsonObject.getJSONObject("error").has("code") &&
                        jsonObject.getJSONObject("error").getString("code").equals("VIOLATION_STRICT_MODE_VOUTS")) {
                    results.put("hasReuse", true);
                    if (jsonObject.getJSONObject("error").has("message")) {
                        JSONArray indexes = new JSONArray();
                        JSONArray array = jsonObject.getJSONObject("error").getJSONArray("message");
                        for (int i = 0; i < array.length(); i++) {
                            indexes.put(array.getInt(i));
                        }
                        results.put("reuseIndexes", indexes);
                    }
                }
            } else {
                throw new Exception("Invalid response");
            }
            return results;
        });
    }

    private void failTx(final int resIdDetails) {
        TxAnimUIActivity.this.runOnUiThread(() -> {

            ViewUtil.animateChangeColor(
                    animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
                    getResources().getColor(R.color.blue_send_ui),
                    getResources().getColor(R.color.red_send_ui),
                    BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ViewUtil.animateChangeColor(
                        animation ->  getWindow().setNavigationBarColor((int) animation.getAnimatedValue()),
                        getResources().getColor(R.color.blue_send_ui),
                        getResources().getColor(R.color.red_send_ui),
                        BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);
            }

            progressView.reset();
            progressView.showFailedTxOptions(resIdDetails);
            progressView.getOptionBtn1().setOnClickListener(v -> reBroadcastTx());
            progressView.getOptionBtn2().setOnClickListener(v -> launchSupportPageInBrowser(
                    TxAnimUIActivity.this,
                    SamouraiTorManager.INSTANCE.isConnected()));
            progressView.getLeftTopImgBtn().setOnClickListener(view -> finish());
        });
    }

    private void reBroadcastTx() {

        ViewUtil.animateChangeColor(
            animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
            getResources().getColor(R.color.red_send_ui),
            getResources().getColor(R.color.blue_send_ui),
            BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ViewUtil.animateChangeColor(
                    animation ->  getWindow().setNavigationBarColor((int) animation.getAnimatedValue()),
                    getResources().getColor(R.color.red_send_ui),
                    getResources().getColor(R.color.blue_send_ui),
                    BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);
        }

        progressView.setBackgroundColorForOnlineMode(
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS,
                R.color.red_send_ui,
                R.color.blue_send_ui);

        progressView.reset();
        broadcastTx();
    }

    private void offlineTx(
            final String hex,
            final String hash,
            final int resIdDetails) {

        TxAnimUIActivity.this.runOnUiThread(() -> {

            ViewUtil.animateChangeColor(
                    animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
                    getResources().getColor(R.color.blue_send_ui),
                    getResources().getColor(R.color.orange_send_ui),
                    BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ViewUtil.animateChangeColor(
                        animation ->  getWindow().setNavigationBarColor((int) animation.getAnimatedValue()),
                        getResources().getColor(R.color.blue_send_ui),
                        getResources().getColor(R.color.orange_send_ui),
                        BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);
            }

            progressView.reset();
            progressView.showOfflineTxOptions(resIdDetails);
            progressView.getOptionBtn1().setOnClickListener(v -> txTenna(hex));
            progressView.getOptionBtn2().setOnClickListener(v -> launchManualTxBroadcastActivity(hex));
            progressView.getLeftTopImgBtn().setOnClickListener(view -> finish());
        });
    }

    private void launchManualTxBroadcastActivity(final String hex) {
        final Intent intent = new Intent(TxAnimUIActivity.this, TxBroadcastManuallyActivity.class);
        intent.putExtra("txHex", hex);
        startActivity(intent);
    }

    private void txTenna(final String hex) {
        try {
            startActivity(createTxTennaIntent(hex));
        } catch (final ActivityNotFoundException e) {
            Log.w(TAG, e.getMessage(), e);
            Log.i(TAG, "no txTenna app found, will redirect user to the txTenna github page");
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://github.com/MuleTools/txTenna/releases")));
        }
    }

    private static Intent createTxTennaIntent(String hex) {
        final Intent txTennaIntent = new Intent("com.samourai.txtenna.HEX");
        txTennaIntent.setComponent(new ComponentName(
                "com.samourai.txtenna",
                "com.samourai.txtenna.MainActivity"));
        txTennaIntent.setType("text/plain");
        if (SamouraiWallet.getInstance().isTestNet()) {
            txTennaIntent.putExtra(Intent.EXTRA_TEXT, hex + "-t");
        } else {
            txTennaIntent.putExtra(Intent.EXTRA_TEXT, hex);
        }
        return txTennaIntent;
    }

    private void handleResult(final boolean isOK,
                              final RBFSpend rbf,
                              final String strTxHash,
                              final String hexTx,
                              final Transaction _tx) {

        try {

            if (isOK) {

                UTXOUtil.getInstance().addNote(_tx.getHashAsString(), SendParams.getInstance().getNote());

                txSuccess.set(true);

                if (SendParams.getInstance().getChangeAmount() > 0l
                        && SendParams.getInstance().getSpendType() == SendActivity.SPEND_SIMPLE) {

                    // increment change index
                    final WALLET_INDEX changeIndex = WALLET_INDEX.findChangeIndex(
                            SendParams.getInstance().getAccount(),
                            SendParams.getInstance().getChangeType());
                    AddressFactory.getInstance().increment(changeIndex);
                }

                updateRBFSpendForBroadcastTxAndRegister(
                        rbf,
                        _tx,
                        SendParams.getInstance().getDestAddress(),
                        SendParams.getInstance().getChangeType(),
                        this);

                // increment counter if BIP47 spend
                if (SendParams.getInstance().getPCode() != null
                        && SendParams.getInstance().getPCode().length() > 0) {

                    final String destAddress = SendParams.getInstance().getDestAddress();
                    final String pcode = SendParams.getInstance().getPCode();
                    if (nonNull(BIP47Meta.getInstance().getPCode4Addr(destAddress))) {
                        Log.w(TAG, String.format("address %s is reuse for pcode %s", destAddress, pcode));
                    }

                    BIP47Meta.getInstance().getPCode4AddrLookup().put(destAddress, pcode);
                    BIP47Meta.getInstance().incOutgoingIdx(pcode);

                    SentToFromBIP47Util.getInstance().add(pcode, strTxHash);

                    SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                    String strTS = sd.format(System.currentTimeMillis());
                    final long spendAmount = SendParams.getInstance().getSpendAmount();
                    String event = strTS + " "
                            + TxAnimUIActivity.this.getString(R.string.sent) +
                            " " +
                            MonetaryUtil.getInstance().getBTCFormat()
                                    .format(getBtcValue(spendAmount)) +
                            " BTC";
                    BIP47Meta.getInstance().setLatestEvent(pcode, event);

                } else if (SendParams.getInstance().getBatchSend() != null) {

                    for (final BatchSendUtil.BatchSend d : SendParams.getInstance().getBatchSend()) {
                        try {
                            String address = d.getAddr(this);
                            String pcode = d.pcode;
                            // increment counter if BIP47 spend
                            if (isNotBlank(pcode)) {

                                if (nonNull(BIP47Meta.getInstance().getPCode4Addr(address))) {
                                    Log.w(TAG, String.format("address %s is reuse for pcode %s", address, pcode));
                                }

                                BIP47Meta.getInstance().getPCode4AddrLookup().put(address, pcode);
                                BIP47Meta.getInstance().incOutgoingIdx(pcode);

                                SentToFromBIP47Util.getInstance().add(pcode, strTxHash);

                                SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                String strTS = sd.format(System.currentTimeMillis());
                                String event = strTS + " " +
                                        TxAnimUIActivity.this.getString(R.string.sent) + " " +
                                        MonetaryUtil.getInstance().getBTCFormat().format(getBtcValue(d.amount)) +
                                        " BTC";
                                BIP47Meta.getInstance().setLatestEvent(pcode, event);

                            }
                        } catch (final Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }

                }

                if (SendParams.getInstance().hasPrivacyWarning() && SendParams.getInstance().hasPrivacyChecked()) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else if (SendAddressUtil.getInstance().get(SendParams.getInstance().getDestAddress()) == 0) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), true);
                }

                final boolean doNotSpendChangeBtnVisible = getChangeAddress() != null;
                TxAnimUIActivity.this.runOnUiThread(() -> {
                    progressView.getOptionBtn1().setOnClickListener(view -> {
                        if (!doNotSpendChangeBtnVisible) return;
                        editChangeUtxo(hexTx, true);
                    });
                    progressView.getOptionBtn2().setOnClickListener(view -> {});
                    progressView.showSuccessSentTxOptions(doNotSpendChangeBtnVisible, R.string.tx_option_change_do_not_spend);
                    progressView.getLeftTopImgBtn()
                            .setOnClickListener(view -> gotoBalanceHomeActivity(
                                    this,
                                    SendParams.getInstance().getAccount()));
                });
            } else {

                TxAnimUIActivity.this.runOnUiThread(
                        () -> Toast.makeText(
                                TxAnimUIActivity.this,
                                R.string.tx_failed,
                                Toast.LENGTH_SHORT).show()
                );

                // reset change index upon tx fail
                final WALLET_INDEX changeIndex = WALLET_INDEX.findChangeIndex(
                        SendParams.getInstance().getAccount(),
                        SendParams.getInstance().getChangeType());
                AddressFactory.getInstance().setWalletIdx(changeIndex,SendParams.getInstance().getChangeIdx(),true);
            }

        } catch (final DecoderException de) {
            TxAnimUIActivity.this.runOnUiThread(() -> {
                Toast.makeText(TxAnimUIActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                progressView.getLeftTopImgBtn().setVisibility(View.VISIBLE);
            });
        }

    }

    @Override
    public void onBackPressed() {
        if(! activityInProgress()) {
            if (txSuccess.get()) {
                gotoBalanceHomeActivity(this, SendParams.getInstance().getAccount());
            } else {
                finish();
            }
        }
    }

    private boolean activityInProgress() {
        return txInProgress.get()
                || progressView.getOptionProgressBar().getVisibility() == View.VISIBLE;
    }
}
