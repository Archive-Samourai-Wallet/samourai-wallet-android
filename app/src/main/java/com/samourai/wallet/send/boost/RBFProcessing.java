package com.samourai.wallet.send.boost;

import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.util.tech.SimpleCallback;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.core.app.TaskStackBuilder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static java.util.Objects.nonNull;

public class RBFProcessing {

    private static final String TAG = RBFProcessing.class.getSimpleName();

    private RBFSpend rbf;
    private String txHash;
    private Transaction transaction;
    private Map<String, Long> inputValues;
    private Map<String, String> extraInputs;
    private String message;
    private SamouraiActivity activity;

    private RBFProcessing(@NotNull RBFSpend rbf,
                          @NotNull String txHash,
                          @NotNull Transaction transaction,
                          @NotNull Map<String, Long> inputValues,
                          @NotNull Map<String, String> extraInputs,
                          @NotNull String message,
                          @NotNull SamouraiActivity activity) {

        this.rbf = rbf;
        this.txHash = txHash;
        this.transaction = transaction;
        this.inputValues = inputValues;
        this.extraInputs = extraInputs;
        this.message = message;
        this.activity = activity;
    }

    public static @Nullable RBFProcessing create(
            @NotNull RBFSpend rbf,
            @NotNull String txHash,
            @NotNull Transaction transaction,
            @NotNull Map<String, Long> inputValues,
            @NotNull Map<String, String> extraInputs,
            @NotNull String message,
            @NotNull SamouraiActivity activity
    ) {
        return new RBFProcessing(rbf, txHash, transaction, inputValues, extraInputs, message, activity);
    }

    public void process(final SimpleCallback<String> callback) {

        final AlertDialog.Builder dlg = new AlertDialog.Builder(activity)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {

                    final Transaction __tx = signTx(transaction);
                    if (nonNull(__tx)) {

                        final String hexTx = new String(Hex.encode(__tx.bitcoinSerialize()));
                        Log.d("RBF", "hex tx:" + hexTx);

                        final String strTxHash = __tx.getHashAsString();
                        Log.d("RBF", "tx hash:" + strTxHash);

                        final Disposable disposable = Observable
                                .fromCallable(() -> PushTx.getInstance(activity).pushTx(hexTx))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        t -> {
                                            Toast.makeText(activity, R.string.rbf_spent, Toast.LENGTH_SHORT).show();
                                            rbf.setSerializedTx(hexTx);
                                            rbf.setHash(strTxHash);
                                            rbf.setPrevHash(txHash);
                                            RBFUtil.getInstance().add(rbf);

                                            gotoBalanceHomeActivity();

                                            callback.onComplete("DONE");
                                        },
                                        e -> {
                                            Log.e(TAG, "pushTx:" + e.getMessage());
                                            Toast.makeText(activity, "pushTx:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            callback.onException(e);
                                        });
                        activity.registerDisposable(disposable);

                    } else {
                        Log.e(TAG, "tx null : issue on signing tx");
                        Toast.makeText(activity, "tx null : issue on signing tx", Toast.LENGTH_SHORT).show();
                        callback.onException(new Exception("tx null : issue on signing tx"));
                    }
                }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
                    dialog.dismiss();
                    callback.onComplete("GIVE UP");
                });

        if (!activity.isFinishing()) {
            dlg.show();
        } else {
            callback.onException(new Exception("activity is finishing"));
        }
    }

    private void gotoBalanceHomeActivity() {
        if (activity.getAccount() != 0) {

            final Intent balanceHome = new Intent(activity, BalanceActivity.class);
            balanceHome.putExtra("_account", activity.getAccount());
            balanceHome.putExtra("refresh", true);
            final Intent parentIntent = new Intent(activity, BalanceActivity.class);
            parentIntent.putExtra("_account", 0);
            balanceHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder.create(activity)
                    .addNextIntent(parentIntent)
                    .addNextIntent(balanceHome)
                    .startActivities();

        } else {
            final Intent _intent = new Intent(activity, BalanceActivity.class);
            _intent.putExtra("refresh", true);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(_intent);
        }
    }

    private Transaction signTx(final Transaction tx) {


        final Map<String, ECKey> keyBagLegacy = new HashMap<>();
        final Map<String, ECKey> keyBag49 = new HashMap<>();
        final Map<String, ECKey> keyBag84 = new HashMap<>();
        final Map<String, ECKey> keyBagExtra = new HashMap<>();

        loadKeyBags(keyBagLegacy, keyBag49, keyBag84, keyBagExtra);

        final NetworkParameters networkParams = SamouraiWallet.getInstance().getCurrentNetworkParams();

        try {

            final List<TransactionInput> inputs = tx.getInputs();

            for (int i = 0; i < inputs.size(); i++) {

                ECKey ecKey = null;
                String address = null;
                final String key = inputs.get(i).getOutpoint().toString();

                if (keyBag84.containsKey(key)) {
                    ecKey = keyBag84.get(key);
                    final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), networkParams);
                    address = segwitAddress.getBech32AsString();
                } else if (keyBag49.containsKey(key)) {
                    ecKey = keyBag49.get(key);
                    final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), networkParams);
                    address = segwitAddress.getAddressAsString();
                } else if (keyBagLegacy.containsKey(key)) {
                    ecKey = keyBagLegacy.get(key);
                    address = ecKey.toAddress(networkParams).toString();
                } else if (keyBagExtra.containsKey(key)) {
                    ecKey = keyBagExtra.get(key);
                    address = ecKey.toAddress(networkParams).toString();
                }

                Log.d("RBF", "pubKey:" + Hex.toHexString(ecKey.getPubKey()));
                Log.d("RBF", "address:" + address);

                if (keyBag49.containsKey(key) || keyBag84.containsKey(key)) {

                    final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), networkParams);
                    Script scriptPubKey = segwitAddress.segwitOutputScript();
                    final Script redeemScript = segwitAddress.segwitRedeemScript();
                    System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                    final Script scriptCode = redeemScript.scriptCode();
                    System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                    final TransactionSignature sig = tx.calculateWitnessSignature(
                            i,
                            ecKey,
                            scriptCode,
                            Coin.valueOf(inputValues.get(key)),
                            Transaction.SigHash.ALL,
                            false);

                    final TransactionWitness witness = new TransactionWitness(2);
                    witness.setPush(0, sig.encodeToBitcoin());
                    witness.setPush(1, ecKey.getPubKey());
                    tx.setWitness(i, witness);

                    if (!FormatsUtil.getInstance().isValidBech32(address) &&
                            Address.fromBase58(networkParams, address).isP2SHAddress()) {
                        final ScriptBuilder sigScript = new ScriptBuilder();
                        sigScript.data(redeemScript.getProgram());
                        tx.getInput(i).setScriptSig(sigScript.build());
                        tx.getInput(i).getScriptSig().correctlySpends(
                                tx,
                                i,
                                scriptPubKey,
                                Coin.valueOf(inputValues.get(key)),
                                Script.ALL_VERIFY_FLAGS);
                    }

                } else if (keyBagExtra.containsKey(key)) {
                    SendFactoryGeneric.getInstance().signInput(ecKey, tx, i, BIP_FORMAT.PROVIDER);
                }
                else {
                    Log.i("RBF", "sign outpoint:" + key);
                    Log.i("RBF", "ECKey address from keyBag:" + ecKey.toAddress(networkParams).toString());

                    Log.i("RBF", "script:" + ScriptBuilder.createOutputScript(ecKey.toAddress(networkParams)));
                    Log.i("RBF", "script:" + Hex.toHexString(ScriptBuilder.createOutputScript(ecKey.toAddress(networkParams)).getProgram()));
                    final TransactionSignature sig = tx.calculateSignature(
                            i,
                            ecKey,
                            ScriptBuilder.createOutputScript(ecKey.toAddress(networkParams)).getProgram(),
                            Transaction.SigHash.ALL,
                            false);

                    tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(sig, ecKey));
                }

            }
        } catch(Exception e) {
            return null;
        }

        return tx;
    }

    private void loadKeyBags(final Map<String, ECKey> keyBagLegacy,
                             final Map<String, ECKey> keyBag49,
                             final Map<String, ECKey> keyBag84,
                             final Map<String, ECKey> keyBagExtra) {

        final Map<String, String> keys = rbf.getKeyBag();

        for (final Map.Entry<String, String> keyByOutpoint : keys.entrySet()) {

            final String key = keyByOutpoint.getValue();
            final String outpoint = keyByOutpoint.getKey();

            final String[] path = key.split("/");
            Log.i("RBF", "path length:" + path.length);
            final ECKey ecKey = getECKey(path, activity.getAccount(), outpoint);

            Log.i("RBF", "outpoint:" + outpoint);
            Log.i("RBF", "path:" + key);

            if (nonNull(ecKey)) {
                if (extraInputs.containsKey(outpoint)) {
                    keyBagExtra.put(outpoint, ecKey);
                } else if (activity.getAccount() == SamouraiAccountIndex.POSTMIX) {
                    if (path.length == 4) {
                        if (path[3].equals("84")) {
                            keyBag84.put(outpoint, ecKey);
                        } else {
                            keyBag49.put(outpoint, ecKey);
                        }
                    } else {
                        keyBagLegacy.put(outpoint, ecKey);
                    }

                } else {
                    if (path.length == 4) {
                        if (path[3].equals("84")) {
                            keyBag84.put(outpoint, ecKey);
                        } else {
                            keyBag49.put(outpoint, ecKey);
                        }
                    } else {
                        keyBagLegacy.put(outpoint, ecKey);
                    }
                }

            } else {
                throw new RuntimeException("ECKey error: cannot process private key");
            }
        }
    }

    private ECKey getECKey(final String[] keyParts,
                           final int account,
                           final String outpoint) {

        if (extraInputs.containsKey(outpoint)) {
            return SendFactory.getPrivKey(extraInputs.get(outpoint), account);
        }

        if (account == SamouraiAccountIndex.POSTMIX) {
            return getEcKeyForPostMixAccount(keyParts);
        } else {
            return getEcKey(keyParts);
        }
    }

    private ECKey getEcKeyForPostMixAccount(final String[] keyParts) {
        ECKey ecKey  = null;
        if (keyParts.length == 4) {
            if (keyParts[3].equals("84")) {
                final HD_Address addr = BIP84Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(keyParts[1])).getAddressAt(Integer.parseInt(keyParts[2]));
                ecKey = addr.getECKey();
            } else {
                final HD_Address addr = BIP84Util.getInstance(activity).getWallet().getAccount(WhirlpoolMeta.getInstance(activity).getWhirlpoolPostmix()).getChain(Integer.parseInt(keyParts[1])).getAddressAt(Integer.parseInt(keyParts[2]));
                ecKey = addr.getECKey();
            }
        } else if (keyParts.length == 3) {
            final HD_Address hd_addr = BIP84Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(keyParts[1])).getAddressAt(Integer.parseInt(keyParts[2]));
            ecKey = hd_addr.getECKey();
        } else if (keyParts.length == 2) {
            try {
                final SegwitAddress address = BIP47Util.getInstance(activity).getReceiveAddress(new PaymentCode(keyParts[0]), Integer.parseInt(keyParts[1]));
                ecKey = address.getECKey();
            } catch (Exception e) {
                ;
            }
        } else {
            ;
        }
        return ecKey;

    }

    private ECKey getEcKey(final String[] keyParts) {
        ECKey ecKey  = null;
        if (keyParts.length == 4) {
            if (keyParts[3].equals("84")) {
                final HD_Address addr = BIP84Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(keyParts[1])).getAddressAt(Integer.parseInt(keyParts[2]));
                ecKey = addr.getECKey();
            } else {
                final HD_Address addr = BIP49Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(keyParts[1])).getAddressAt(Integer.parseInt(keyParts[2]));
                ecKey = addr.getECKey();
            }
        } else if (keyParts.length == 3) {
            final HD_Address hd_address = AddressFactory.getInstance(activity).get(activity.getAccount(), Integer.parseInt(keyParts[1]), Integer.parseInt(keyParts[2]));
            final String strPrivKey = hd_address.getPrivateKeyString();
            final NetworkParameters networkParams = SamouraiWallet.getInstance().getCurrentNetworkParams();
            final DumpedPrivateKey pk = DumpedPrivateKey.fromBase58(networkParams, strPrivKey);
            ecKey = pk.getKey();
        } else if (keyParts.length == 2) {
            try {
                final SegwitAddress address = BIP47Util.getInstance(activity).getReceiveAddress(new PaymentCode(keyParts[0]), Integer.parseInt(keyParts[1]));
                ecKey = address.getECKey();
            } catch (Exception e) {
                ;
            }
        } else {
            ;
        }
        return ecKey;
    }
}
