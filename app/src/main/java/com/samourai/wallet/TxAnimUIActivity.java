package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.TaskStackBuilder;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.whirlpool.WhirlpoolHome;
import com.samourai.wallet.widgets.TransactionProgressView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
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

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.util.LogUtil.debug;

public class TxAnimUIActivity extends AppCompatActivity {

    private TransactionProgressView progressView = null;

    private static final String TAG = "TxAnimUIActivity";
    private int arcdelay = 800;
    private long signDelay = 2000L;
    private long broadcastDelay = 1599L;
    private long resultDelay = 1500L;
    private boolean txInProgress = false;

    private CompositeDisposable disposables = new CompositeDisposable();
    private Handler resultHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_anim_ui);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.green_ui_2));

        progressView = findViewById(R.id.transactionProgressView);
        progressView.reset();
        progressView.setTxStatusMessage(R.string.tx_creating_ok);
        progressView.getmArcProgress().startArc1(arcdelay);

        // make tx
        final Transaction tx = SendFactory.getInstance(TxAnimUIActivity.this).makeTransaction(SendParams.getInstance().getOutpoints(), SendParams.getInstance().getReceivers());
        if (tx == null) {
            failTx(R.string.tx_creating_ko);
        } else {
//            Toast.makeText(TxAnimUIActivity.this, "tx created OK", Toast.LENGTH_SHORT).show();

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

            final List<Integer> strictModeVouts = new ArrayList<Integer>();
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


                final Transaction _tx = SendFactory.getInstance(TxAnimUIActivity.this).signTransaction(tx, SendParams.getInstance().getAccount());
                if (_tx == null) {
                    failTx(R.string.tx_signing_ko);
                } else {
//                    Toast.makeText(TxAnimUIActivity.this, "tx signed OK", Toast.LENGTH_SHORT).show();
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
                        offlineTx(R.string.broadcast_off, hexTx, strTxHash);
                        return;

                    }

                    if (AppUtil.getInstance(TxAnimUIActivity.this).isOfflineMode()) {
                        offlineTx(R.string.offline_mode, hexTx, strTxHash);
                        return;
                    }
                    txInProgress = true;

                    Disposable disposable = pushTx(hexTx, strictModeVouts)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((jsonObject) -> {
                                txInProgress = false;
                                debug(TAG, jsonObject.toString());
                                if (jsonObject.getBoolean("isOk")) {
                                    progressView.showCheck();
                                    progressView.setTxStatusMessage(R.string.tx_sent_ok);
                                    handleResult(true, rbf, strTxHash, hexTx, _tx);

                                } else if (jsonObject.getBoolean("hasReuse")) {

                                    showAddressReuseWarning(rbf, strTxHash, hexTx, _tx);

                                } else {
                                    failTx(R.string.tx_sent_ko);
                                    handleResult(false, rbf, strTxHash, hexTx, _tx);
                                }

                            }, throwable -> {
                                txInProgress = false;
                                failTx(R.string.tx_broadcast_ko);
                            });

                    disposables.add(disposable);

                }, broadcastDelay);

            }, signDelay);

        }

    }

    private void showAddressReuseWarning(RBFSpend rbf, String strTxHash, String hexTx, Transaction _tx) {

        List<Integer> emptyList = new ArrayList<>();
        AlertDialog.Builder dlg = new AlertDialog.Builder(TxAnimUIActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.strict_mode_address)
                .setCancelable(false)
                .setPositiveButton(R.string.broadcast, (dialog, whichButton) -> {
                    dialog.dismiss();
                    Disposable disposable = this.pushTx(hexTx, emptyList)
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
                                throwable.printStackTrace();
                                failTx(R.string.tx_broadcast_ko);
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

    private Single<JSONObject> pushTx(String hexTx, List<Integer> strictModeVouts) {
        return Single.fromCallable(() -> {

            JSONObject results = new JSONObject();
            results.put("isOk", false);
            results.put("hasReuse", false);
            results.put("reuseIndexes", new JSONArray());

            for(Integer i :  strictModeVouts) {
                debug("TxAnimUIActivity", "strict mode output index:" + i);
            }

            String response = PushTx.getInstance(TxAnimUIActivity.this).samourai(hexTx, (strictModeVouts != null && strictModeVouts.size() > 0) ? strictModeVouts : null);
/*
            String response = null;
            if(strictModeVouts != null && strictModeVouts.size() > 0) {
                response = "{\"status\"=\"error\", \"error\"={\"code\"=\"VIOLATION_STRICT_MODE_VOUTS\"}}";
                debug("TxAnimUIActivity", "stub fail:" + response);
            }
            else {
                response = PushTx.getInstance(TxAnimUIActivity.this).samourai(hexTx, null);
                debug("TxAnimUIActivity", "stub rebroadcast:" + response);
            }
*/
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

    private void failTx(int id) {
        TxAnimUIActivity.this.runOnUiThread(() -> {
            progressView.reset();
            progressView.offlineMode(1200);
            progressView.setTxStatusMessage(R.string.tx_failed);
            progressView.setTxSubText(id);
        });
    }

    private void offlineTx(int id, final String hex, final String hash) {
        TxAnimUIActivity.this.runOnUiThread(() -> {

            progressView.reset();

            progressView.offlineMode(1200);
            progressView.setTxStatusMessage(R.string.tx_standby);
            progressView.setTxSubText(id);
            progressView.setTxSubText(R.string.in_offline_mode);
            progressView.toggleOfflineButton();

            progressView.getShowQRButton().setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    doShowTx(hex, hash);
                }
            });

            progressView.getTxTennaButton().setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
//                Toast.makeText(TxAnimUIActivity.this, R.string.tx_tenna_coming_soon, Toast.LENGTH_SHORT).show();

                    String pkgName = "com.samourai.txtenna";

                    String _hex = hex;
                    Intent txTennaIntent = new Intent("com.samourai.txtenna.HEX");
                    if (SamouraiWallet.getInstance().isTestNet()) {
                        _hex += "-t";
                    }
                    txTennaIntent.putExtra(Intent.EXTRA_TEXT, _hex);
                    txTennaIntent.setType("text/plain");

                    Uri marketUri = Uri.parse("market://search?q=pname:" + pkgName);
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);

                    PackageManager pm = getPackageManager();
                    try {
                        pm.getPackageInfo(pkgName, 0);
                        startActivity(txTennaIntent);
                    } catch (PackageManager.NameNotFoundException e) {
                        startActivity(marketIntent);
                    }

                }
            });
        });
    }

    private void handleResult(boolean isOK, RBFSpend rbf, String strTxHash, String hexTx, Transaction _tx) {

        try {
            WALLET_INDEX changeIndex = WALLET_INDEX.findChangeIndex(SendParams.getInstance().getAccount(), SendParams.getInstance().getChangeType());
            if (isOK) {
                Toast.makeText(TxAnimUIActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();

                if (SendParams.getInstance().getChangeAmount() > 0L && SendParams.getInstance().getSpendType() == SendActivity.SPEND_SIMPLE) {
                    // increment change index
                    AddressFactory.getInstance().increment(changeIndex);
                }

                if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

                    for (TransactionOutput out : _tx.getOutputs()) {
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
                            } else {
                                ;
                            }
                        } catch (NullPointerException npe) {
                            ;
                        } catch (Exception e) {
                            ;
                        }
                    }

                    rbf.setHash(strTxHash);
                    rbf.setSerializedTx(hexTx);

                    RBFUtil.getInstance().add(rbf);
                }

                // increment counter if BIP47 spend
                if (SendParams.getInstance().getPCode() != null && SendParams.getInstance().getPCode().length() > 0) {
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(SendParams.getInstance().getDestAddress(), SendParams.getInstance().getPCode());
                    BIP47Meta.getInstance().incOutgoingIdx(SendParams.getInstance().getPCode());

                    SentToFromBIP47Util.getInstance().add(SendParams.getInstance().getPCode(), strTxHash);

                    SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                    String strTS = sd.format(System.currentTimeMillis());
                    String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) SendParams.getInstance().getSpendAmount() / 1e8) + " BTC";
                    BIP47Meta.getInstance().setLatestEvent(SendParams.getInstance().getPCode(), event);
                } else if (SendParams.getInstance().getBatchSend() != null) {

                    for (BatchSendUtil.BatchSend d : SendParams.getInstance().getBatchSend()) {
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

                } else {
                    ;
                }

                if (SendParams.getInstance().hasPrivacyWarning() && SendParams.getInstance().hasPrivacyChecked()) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else if (SendAddressUtil.getInstance().get(SendParams.getInstance().getDestAddress()) == 0) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), true);
                }


                new Handler().postDelayed(() -> {
                    if (SendParams.getInstance().getAccount() != 0) {
                        Intent whirlPoolHome = new Intent(this, WhirlpoolHome.class);
                        whirlPoolHome.putExtra("_account", SendParams.getInstance().getAccount());
                        whirlPoolHome.putExtra("refresh", true);
                        Intent parentIntent = new Intent(this, BalanceActivity.class);
                        whirlPoolHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        TaskStackBuilder.create(getApplicationContext())
                                .addNextIntent(parentIntent)
                                .addNextIntent(whirlPoolHome)
                                .startActivities();
                    }else{
                        Intent _intent = new Intent(TxAnimUIActivity.this, BalanceActivity.class);
                        _intent.putExtra("refresh", true);
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(_intent);
                    }
                }, 1000L);

            } else {
                Toast.makeText(TxAnimUIActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                // reset change index upon tx fail
                AddressFactory.getInstance().setWalletIdx(changeIndex,SendParams.getInstance().getChangeIdx(),true);
            }
        } catch (DecoderException de) {
            Toast.makeText(TxAnimUIActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void doShowTx(final String hexTx, final String txHash) {

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(TxAnimUIActivity.this);
        showTx.setText(hexTx);
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        final CheckBox cbMarkInputsUnspent = new CheckBox(TxAnimUIActivity.this);
        cbMarkInputsUnspent.setText(R.string.mark_inputs_as_unspendable);
        cbMarkInputsUnspent.setChecked(false);

        LinearLayout hexLayout = new LinearLayout(TxAnimUIActivity.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(cbMarkInputsUnspent);
        hexLayout.addView(showTx);

        new AlertDialog.Builder(TxAnimUIActivity.this)
                .setTitle(txHash)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, (dialog, whichButton) -> {

                    if (cbMarkInputsUnspent.isChecked()) {
                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(hexTx,SendParams.getInstance().getAccount());
                        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                        intent.putExtra("notifTx", false);
                        intent.putExtra("fetch", true);
                        LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                    }

                    dialog.dismiss();
                    TxAnimUIActivity.this.finish();

                })
                .setNeutralButton(R.string.copy_to_clipboard, (dialog, whichButton) -> {

                    if (cbMarkInputsUnspent.isChecked()) {
                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(hexTx,SendParams.getInstance().getAccount());
                        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
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
                        UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsNonSpendable(hexTx,SendParams.getInstance().getAccount());
                        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                        intent.putExtra("notifTx", false);
                        intent.putExtra("fetch", true);
                        LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                    }

                    if (hexTx.length() <= QR_ALPHANUM_CHAR_LIMIT) {

                        final ImageView ivQR = new ImageView(TxAnimUIActivity.this);

                        Display display = (TxAnimUIActivity.this).getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int imgWidth = Math.max(size.x - 240, 150);

                        Bitmap bitmap = null;

                        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(hexTx, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                        try {
                            bitmap = qrCodeEncoder.encodeAsBitmap();
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }

                        ivQR.setImageBitmap(bitmap);

                        LinearLayout qrLayout = new LinearLayout(TxAnimUIActivity.this);
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
        if(!txInProgress){
            super.onBackPressed();
        }
    }
}
