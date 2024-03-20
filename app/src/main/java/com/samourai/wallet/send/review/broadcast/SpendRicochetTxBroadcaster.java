package com.samourai.wallet.send.review.broadcast;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.ricochet.RicochetActivity;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.ricochet.RicochetTransactionInfo;
import com.samourai.wallet.send.review.ReviewTxModel;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.activity.ActivityHelper;
import com.samourai.wallet.util.network.WebUtil;
import com.samourai.wallet.utxos.UTXOUtil;

import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SpendRicochetTxBroadcaster {
    private final static int RICOCHET = 2013;

    private final ReviewTxModel model;
    private final SamouraiActivity activity;

    private SpendRicochetTxBroadcaster(final ReviewTxModel model,
                                     final SamouraiActivity activity) {

        this.model = model;
        this.activity = activity;
    }

    public static SpendRicochetTxBroadcaster create(final ReviewTxModel model,
                                                  final SamouraiActivity activity) {

        return new SpendRicochetTxBroadcaster(model, activity);
    }

    public SpendRicochetTxBroadcaster broadcast() {
        if (model.isEnabledRicochetStaggered()) {
            ricochetSpendStaggered();
        } else {
            ricochetSpend();
        }
        return this;
    }

    private void ricochetSpend() {
        final RicochetTransactionInfo transactionInfo = model.getTxData().getValue().getRicochetTransactionInfo();
        RicochetMeta.getInstance(activity).add(transactionInfo.getRicochetScriptAsJson());
        final Intent intent = new Intent(activity, RicochetActivity.class);
        intent.putExtra("tx_note", defaultString(model.getTxNote().getValue()));
        intent.putExtra("_account", model.getAccount());
        activity.startActivityForResult(intent, RICOCHET);
    }

    private void ricochetSpendStaggered() {
        try {

            final StringBuilder txHash = new StringBuilder();

            final RicochetTransactionInfo transactionInfo = model.getTxData().getValue().getRicochetTransactionInfo();
            if (transactionInfo.getRicochetScriptAsJson().has("hops")) {

                final JSONArray hops = transactionInfo.getRicochetScriptAsJson().getJSONArray("hops");

                if (hops.getJSONObject(0).has("nTimeLock")) {

                    final JSONArray nLockTimeScript = new JSONArray();
                    for (int i = 0; i < hops.length(); i++) {
                        JSONObject hopObj = hops.getJSONObject(i);
                        long locktime = hopObj.getLong("nTimeLock");
                        String hex = hopObj.getString("tx");
                        JSONObject scriptObj = new JSONObject();
                        scriptObj.put("hop", i);
                        scriptObj.put("nlocktime", locktime);
                        scriptObj.put("tx", hex);
                        nLockTimeScript.put(scriptObj);

                        if (i == 0) {
                            final Transaction tx = new Transaction(
                                    SamouraiWallet.getInstance().getCurrentNetworkParams(),
                                    Hex.decode(hex.trim()));
                            txHash.append(tx.getHashAsString());
                        }
                    }

                    JSONObject nLockTimeObj = new JSONObject();
                    nLockTimeObj.put("script", nLockTimeScript);
                    if (APIFactory.getInstance(activity).APITokenRequired()) {
                        nLockTimeObj.put("at", APIFactory.getInstance(activity).getAccessToken());
                    }

//                                        Log.d("SendActivity", "Ricochet nLockTime:" + nLockTimeObj.toString());

                    new Thread(() -> {

                        Looper.prepare();

                        String url = WebUtil.getAPIUrl(activity);
                        url += "pushtx/schedule";
                        try {
                            String result = "";
                            if (SamouraiTorManager.INSTANCE.isRequired()) {
                                result = WebUtil.getInstance(activity).tor_postURL(url, nLockTimeObj, null);

                            } else {
                                result = WebUtil.getInstance(activity).postURL("application/json", url, nLockTimeObj.toString());

                            }
//                                                    Log.d("SendActivity", "Ricochet staggered result:" + result);
                            JSONObject resultObj = new JSONObject(result);
                            if (resultObj.has("status") && resultObj.getString("status").equalsIgnoreCase("ok")) {

                                final String txNote = model.getTxNote().getValue();
                                if (isNotBlank(txNote) && isNotBlank(txHash)) {
                                    UTXOUtil.getInstance().addNote(txHash.toString(), txNote);
                                }
                                Toast.makeText(activity, R.string.ricochet_nlocktime_ok, Toast.LENGTH_LONG).show();
                                ActivityHelper.gotoBalanceHomeActivity(activity, model.getAccount());
                            } else {
                                Toast.makeText(activity, R.string.ricochet_nlocktime_ko, Toast.LENGTH_LONG).show();
                                activity.finish();
                            }
                        } catch (Exception e) {
                            Log.d("SendActivity", e.getMessage());
                            Toast.makeText(activity, R.string.ricochet_nlocktime_ko, Toast.LENGTH_LONG).show();
                            activity.finish();
                        }

                        Looper.loop();

                    }).start();

                }
            }
        } catch (JSONException je) {
            Log.d("SendActivity", je.getMessage());
        }
    }
}
