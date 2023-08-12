package com.samourai.wallet;

import static com.samourai.wallet.util.LogUtil.debug;
import static com.samourai.wallet.util.activity.ActivityHelper.launchSupportPageInBrowser;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.util.view.ViewHelper;
import com.samourai.wallet.widgets.TransactionProgressView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TxAnimUIActivity extends AppCompatActivity {

    private static final String TAG = "TxAnimUIActivity";
    public static final int BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS = 1200;

    private TransactionProgressView progressView = null;
    private int arcdelay = 800;
    private long signDelay = 2000L;
    private long broadcastDelay = 1599L;
    private AtomicBoolean txInProgress = new AtomicBoolean(false);
    private AtomicBoolean txSuccess = new AtomicBoolean(false);

    private CompositeDisposable disposables = new CompositeDisposable();
    private Handler resultHandler = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_anim_ui);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue_send_ui));

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
        progressView.getLeftTopImgBtn().setOnClickListener(view -> gotoBalanceHomeActivity());
        progressView.getOptionBtn2().setOnClickListener(view -> {});

        // make tx
        final Transaction tx = SendFactory.getInstance(TxAnimUIActivity.this)
                .makeTransaction(
                        SendParams.getInstance().getOutpoints(),
                        SendParams.getInstance().getReceivers());

        if (tx == null) {
            failTx(R.string.tx_creating_ko);
            return;
        }

        final RBFSpend rbf;
        if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

            rbf = new RBFSpend();

            for (TransactionInput input : tx.getInputs()) {

                boolean _isBIP49 = false;
                boolean _isBIP84 = false;
                String _addr = null;
                String script = Hex.toHexString(input.getConnectedOutput().getScriptBytes());
                if (Bech32Util.getInstance().isBech32Script(script)) {
                    try {
                        _addr = Bech32Util.getInstance().getAddressFromScript(script);
                        _isBIP84 = true;
                    } catch (Exception e) {
                        ;
                    }
                } else {
                    Address _address = input.getConnectedOutput().getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                    if (_address != null) {
                        _addr = _address.toString();
                        _isBIP49 = true;
                    }
                }
                if (_addr == null) {
                    _addr = input.getConnectedOutput().getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                }

                String path = APIFactory.getInstance(TxAnimUIActivity.this).getUnspentPaths().get(_addr);
                if (path != null) {
                    if (_isBIP84) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/84");
                    } else if (_isBIP49) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/49");
                    } else {
                        rbf.addKey(input.getOutpoint().toString(), path);
                    }
                } else {
                    String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                    rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);
                }

            }

        } else {
            rbf = null;
        }

        final List<Integer> strictModeVouts = new ArrayList<>();
        if (SendParams.getInstance().getDestAddress() != null && SendParams.getInstance().getDestAddress().compareTo("") != 0 &&
                PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.STRICT_OUTPUTS, true) == true) {
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

            resultHandler = new Handler();

            new Handler().postDelayed(() -> {
                TxAnimUIActivity.this.runOnUiThread(() -> {
                    progressView.getmArcProgress().startArc3(arcdelay);
                    progressView.setTxStatusMessage(R.string.tx_broadcast_ok);
                });

                if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.BROADCAST_TX, true) == false) {
                    offlineTx(hexTx, strTxHash, R.string.broadcast_off);
                    return;
                }

                if (AppUtil.getInstance(TxAnimUIActivity.this).isOfflineMode()) {
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

    private void lockChangeUtxo(final String hexTx) {

        progressView.getOptionProgressBar().setVisibility(View.VISIBLE);

        final Disposable disposable = Single.fromCallable(() -> {
                    APIFactory.getInstance(this).initWallet();
                    return true;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {

                    final SendParams sendParams = SendParams.getInstance();
                    final UTXOFactory utxoFactory = UTXOFactory.getInstance(this);
                    final List<TransactionOutPoint> changeTxOutPoints = utxoFactory
                            .getChangeTxOutPoints(sendParams.getAccount(), hexTx);

                    if (!changeTxOutPoints.isEmpty()) {
                        try {
                            for (final TransactionOutPoint out : changeTxOutPoints) {
                                if (sendParams.isPostmixAccount(TxAnimUIActivity.this)) {
                                    BlockedUTXO.getInstance().addPostMix(
                                            out.getHash().toString(),
                                            (int)out.getIndex(),
                                            out.getValue().longValue());
                                } else if (sendParams.isBadBankAccount(TxAnimUIActivity.this)) {
                                    BlockedUTXO.getInstance().addBadBank(
                                            out.getHash().toString(),
                                            (int)out.getIndex(),
                                            out.getValue().longValue());
                                } else {
                                    BlockedUTXO.getInstance().add(
                                            out.getHash().toString(),
                                            (int)out.getIndex(),
                                            out.getValue().longValue());
                                }
                            }

                            TxAnimUIActivity.this.runOnUiThread(() -> {
                                progressView.showSuccessSentTxOptions(false);
                                progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                                progressView.getLeftTopImgBtn().setOnClickListener(view -> gotoBalanceHomeActivity());
                                Toast.makeText(
                                        TxAnimUIActivity.this,
                                        getResources().getString(R.string.tx_toast_change_utxo_locked),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        } catch (final Throwable t) {

                            Log.e(TAG, t.getMessage(), t);

                            TxAnimUIActivity.this.runOnUiThread(() -> {
                                progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                                Toast.makeText(
                                        TxAnimUIActivity.this,
                                        getResources().getString(R.string.tx_toast_lock_change_utxo_issue),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }


                    } else {
                        Toast.makeText(
                                TxAnimUIActivity.this,
                                getResources().getString(R.string.tx_toast_no_change_utxo_found),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }, throwable -> {

                    Log.e(TAG, throwable.getMessage(), throwable);

                    TxAnimUIActivity.this.runOnUiThread(() -> {
                        progressView.getOptionProgressBar().setVisibility(View.INVISIBLE);
                        Toast.makeText(
                                TxAnimUIActivity.this,
                                getResources().getString(R.string.tx_toast_lock_change_utxo_issue),
                                Toast.LENGTH_SHORT
                        ).show();
                    });
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

            ViewHelper.animateChangeBackgroundColor(
                    animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
                    getResources().getColor(R.color.blue_send_ui),
                    getResources().getColor(R.color.red_send_ui),
                    BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

            progressView.reset();
            progressView.showFailedTxOptions(resIdDetails);
            progressView.getOptionBtn1().setOnClickListener(v -> reBroadcastTx());
            progressView.getOptionBtn2().setOnClickListener(v -> launchSupportPageInBrowser(
                    TxAnimUIActivity.this,
                    TorManager.INSTANCE.isConnected()));
            progressView.getLeftTopImgBtn().setOnClickListener(view -> finish());
        });
    }

    private void reBroadcastTx() {

        ViewHelper.animateChangeBackgroundColor(
            animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
            getResources().getColor(R.color.red_send_ui),
            getResources().getColor(R.color.blue_send_ui),
            BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

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

            ViewHelper.animateChangeBackgroundColor(
                    animation -> getWindow().setStatusBarColor((int) animation.getAnimatedValue()),
                    getResources().getColor(R.color.blue_send_ui),
                    getResources().getColor(R.color.orange_send_ui),
                    BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

            progressView.reset();
            progressView.showOfflineTxOptions(resIdDetails);
            progressView.getOptionBtn1().setOnClickListener(v -> txTenna(hex));
            progressView.getOptionBtn2().setOnClickListener(v -> doShowTx(hex, hash));
            progressView.getLeftTopImgBtn().setOnClickListener(view -> finish());
        });
    }

    private void txTenna(final String hex) {

        final String pkgName = "com.samourai.txtenna";

        String _hex = hex;
        Intent txTennaIntent = new Intent("com.samourai.txtenna.HEX");
        if (SamouraiWallet.getInstance().isTestNet()) {
            _hex += "-t";
        }
        txTennaIntent.putExtra(Intent.EXTRA_TEXT, _hex);
        txTennaIntent.setType("text/plain");

        final Uri marketUri = Uri.parse("market://search?q=pname:" + pkgName);
        final Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);

        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(pkgName, 0);
            startActivity(txTennaIntent);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e.getMessage(), e);
            startActivity(marketIntent);
        }
    }

    private void handleResult(final boolean isOK,
                              final RBFSpend rbf,
                              final String strTxHash,
                              final String hexTx,
                              final Transaction _tx) {

        try {

            if (isOK) {

                txSuccess.set(true);

                if (SendParams.getInstance().getChangeAmount() > 0l
                        && SendParams.getInstance().getSpendType() == SendActivity.SPEND_SIMPLE) {

                    // increment change index
                    final WALLET_INDEX changeIndex = WALLET_INDEX.findChangeIndex(
                            SendParams.getInstance().getAccount(),
                            SendParams.getInstance().getChangeType());
                    AddressFactory.getInstance().increment(changeIndex);
                }

                if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

                    for (final TransactionOutput out : _tx.getOutputs()) {
                        try {
                            if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(out.getScriptBytes())) && !SendParams.getInstance().getDestAddress().equals(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())))) {
                                rbf.addChangeAddr(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                                debug("SendActivity", "added change output:" + Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                            } else if (SendParams.getInstance().getChangeType() == 44 && !SendParams.getInstance().getDestAddress().equals(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                rbf.addChangeAddr(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                debug("SendActivity", "added change output:" + out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                            } else if (SendParams.getInstance().getChangeType() != 44 && !SendParams.getInstance().getDestAddress().equals(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                rbf.addChangeAddr(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                debug("SendActivity", "added change output:" + out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                            }
                        } catch (final NullPointerException npe) {
                            Log.e(TAG, npe.getMessage(), npe);
                        } catch (final Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }

                    rbf.setHash(strTxHash);
                    rbf.setSerializedTx(hexTx);

                    RBFUtil.getInstance().add(rbf);
                }

                // increment counter if BIP47 spend
                if (SendParams.getInstance().getPCode() != null
                        && SendParams.getInstance().getPCode().length() > 0) {

                    BIP47Meta.getInstance().getPCode4AddrLookup().put(SendParams.getInstance().getDestAddress(), SendParams.getInstance().getPCode());
                    BIP47Meta.getInstance().incOutgoingIdx(SendParams.getInstance().getPCode());

                    SentToFromBIP47Util.getInstance().add(SendParams.getInstance().getPCode(), strTxHash);

                    SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                    String strTS = sd.format(System.currentTimeMillis());
                    String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) +
                            " " +
                            MonetaryUtil.getInstance().getBTCFormat()
                                    .format((double) SendParams.getInstance().getSpendAmount() / 1e8) + " BTC";
                    BIP47Meta.getInstance().setLatestEvent(SendParams.getInstance().getPCode(), event);

                } else if (SendParams.getInstance().getBatchSend() != null) {

                    for (final BatchSendUtil.BatchSend d : SendParams.getInstance().getBatchSend()) {
                        String address = d.addr;
                        String pcode = d.pcode;
                        // increment counter if BIP47 spend
                        if (pcode != null && pcode.length() > 0) {
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(address, pcode);
                            BIP47Meta.getInstance().incOutgoingIdx(pcode);

                            SentToFromBIP47Util.getInstance().add(pcode, strTxHash);

                            SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                            String strTS = sd.format(System.currentTimeMillis());
                            String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) d.amount / 1e8) + " BTC";
                            BIP47Meta.getInstance().setLatestEvent(pcode, event);

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
                    Toast.makeText(TxAnimUIActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                    progressView.getOptionBtn1().setOnClickListener(view -> {
                        if (!doNotSpendChangeBtnVisible) return;
                        lockChangeUtxo(hexTx);
                    });
                    progressView.getOptionBtn2().setOnClickListener(view -> {});
                    progressView.showSuccessSentTxOptions(doNotSpendChangeBtnVisible);
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

    private void gotoBalanceHomeActivity() {
        if (SendParams.getInstance().getAccount() != 0) {

            final Intent balanceHome = new Intent(this, BalanceActivity.class);
            balanceHome.putExtra("_account", SendParams.getInstance().getAccount());
            balanceHome.putExtra("refresh", true);
            final Intent parentIntent = new Intent(this, BalanceActivity.class);
            parentIntent.putExtra("_account", 0);
            balanceHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder.create(getApplicationContext())
                    .addNextIntent(parentIntent)
                    .addNextIntent(balanceHome)
                    .startActivities();

        } else {
            final Intent _intent = new Intent(TxAnimUIActivity.this, BalanceActivity.class);
            _intent.putExtra("refresh", true);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(_intent);
        }
        finish();
    }

    private void doShowTx(final String hexTx, final String txHash) {

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        final TextView showTx = new TextView(TxAnimUIActivity.this);
        showTx.setText(hexTx);
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        final CheckBox cbMarkInputsUnspent = new CheckBox(TxAnimUIActivity.this);
        cbMarkInputsUnspent.setText(R.string.mark_inputs_as_unspendable);
        cbMarkInputsUnspent.setChecked(false);

        final LinearLayout hexLayout = new LinearLayout(TxAnimUIActivity.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(cbMarkInputsUnspent);
        hexLayout.addView(showTx);

        new AlertDialog.Builder(TxAnimUIActivity.this)
                .setTitle(txHash)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, (dialog, whichButton) -> {

                    if (cbMarkInputsUnspent.isChecked()) {

                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(
                                hexTx,
                                SendParams.getInstance().getAccount());

                        final Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                        intent.putExtra("notifTx", false);
                        intent.putExtra("fetch", true);
                        LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                    }

                    dialog.dismiss();
                    TxAnimUIActivity.this.finish();

                })
                .setNeutralButton(R.string.copy_to_clipboard, (dialog, whichButton) -> {

                    if (cbMarkInputsUnspent.isChecked()) {

                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(
                                hexTx,
                                SendParams.getInstance().getAccount());

                        final Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                        intent.putExtra("notifTx", false);
                        intent.putExtra("fetch", true);
                        LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                    }


                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) TxAnimUIActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("TX", hexTx);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(TxAnimUIActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                })
                .setNegativeButton(R.string.show_qr, (dialog, whichButton) -> {

                    if (cbMarkInputsUnspent.isChecked()) {

                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(
                                hexTx,
                                SendParams.getInstance().getAccount());

                        final Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                        intent.putExtra("notifTx", false);
                        intent.putExtra("fetch", true);
                        LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                    }

                    if (hexTx.length() <= QR_ALPHANUM_CHAR_LIMIT) {

                        final ImageView ivQR = new ImageView(TxAnimUIActivity.this);

                        final Display display = (TxAnimUIActivity.this).getWindowManager().getDefaultDisplay();
                        final Point size = new Point();
                        display.getSize(size);
                        final int imgWidth = Math.max(size.x - 240, 150);

                        Bitmap bitmap = null;

                        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(hexTx, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                        try {
                            bitmap = qrCodeEncoder.encodeAsBitmap();
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }

                        ivQR.setImageBitmap(bitmap);

                        final LinearLayout qrLayout = new LinearLayout(TxAnimUIActivity.this);
                        qrLayout.setOrientation(LinearLayout.VERTICAL);
                        qrLayout.addView(ivQR);

                        new AlertDialog.Builder(TxAnimUIActivity.this)
                                .setTitle(txHash)
                                .setView(qrLayout)
                                .setCancelable(false)
                                .setPositiveButton(R.string.close, (dialog1, whichButton1) -> {

                                    dialog1.dismiss();
                                    TxAnimUIActivity.this.finish();

                                })
                                .setNegativeButton(R.string.share_qr, (dialog12, whichButton12) -> {

                                    String strFileName = AppUtil.getInstance(TxAnimUIActivity.this).getReceiveQRFilename();
                                    File file = new File(strFileName);
                                    if (!file.exists()) {
                                        try {
                                            file.createNewFile();
                                        } catch (Exception e) {
                                            Toast.makeText(TxAnimUIActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    file.setReadable(true, false);

                                    FileOutputStream fos = null;
                                    try {
                                        fos = new FileOutputStream(file);
                                    } catch (FileNotFoundException fnfe) {
                                        ;
                                    }

                                    if (file != null && fos != null) {
                                        Bitmap bitmap1 = ((BitmapDrawable) ivQR.getDrawable()).getBitmap();
                                        bitmap1.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                        try {
                                            fos.close();
                                        } catch (IOException ioe) {
                                            ;
                                        }

                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_SEND);
                                        intent.setType("image/png");
                                        if (android.os.Build.VERSION.SDK_INT >= 24) {
                                            //From API 24 sending FIle on intent ,require custom file provider
                                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                    TxAnimUIActivity.this,
                                                    getApplicationContext()
                                                            .getPackageName() + ".provider", file));
                                        } else {
                                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                        }
                                        startActivity(Intent.createChooser(intent, TxAnimUIActivity.this.getText(R.string.send_tx)));
                                    }

                                }).show();
                    } else {
                        Toast.makeText(TxAnimUIActivity.this, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();
                    }

                }).show();

    }

    @Override
    public void onBackPressed() {
        if(!txInProgress.get()) {
            if (txSuccess.get()) {
                gotoBalanceHomeActivity();
            } else {
                finish();
            }
        }
    }
}
