package com.samourai.wallet.send.boost;

import static java.util.Objects.nonNull;

import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.util.tech.SimpleCallback;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RBFProcessing {

    private static final String TAG = RBFProcessing.class.getSimpleName();

    private RBFSpend rbf;
    private String txHash;
    private Transaction transaction;
    private Map<String, Long> inputValues;
    private String message;
    private SamouraiActivity activity;

    private RBFProcessing(@NotNull RBFSpend rbf,
                          @NotNull String txHash,
                          @NotNull Transaction transaction,
                          @NotNull Map<String, Long> inputValues,
                          @NotNull String message,
                          @NotNull SamouraiActivity activity) {

        this.rbf = rbf;
        this.txHash = txHash;
        this.transaction = transaction;
        this.inputValues = inputValues;
        this.message = message;
        this.activity = activity;
    }

    public static @Nullable RBFProcessing create(
            @NotNull RBFSpend rbf,
            @NotNull String txHash,
            @NotNull Transaction transaction,
            @NotNull Map<String, Long> inputValues,
            @NotNull String message,
            @NotNull SamouraiActivity activity
    ) {
        return new RBFProcessing(rbf, txHash, transaction, inputValues, message, activity);
    }

    public void process(final SimpleCallback<String> callback) {

        final AlertDialog.Builder dlg = new AlertDialog.Builder(activity)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {

                    final Transaction __tx = signTx(transaction, inputValues);
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

                                            final Intent _intent = new Intent(activity, MainActivity2.class);
                                            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            activity.startActivity(_intent);
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

    private Transaction signTx(final Transaction tx,
                               final Map<String, Long> inputValues) {

        final Map<String, ECKey> keyBag = new HashMap<>();
        final Map<String, ECKey> keyBag49 = new HashMap<>();
        final Map<String, ECKey> keyBag84 = new HashMap<>();

        final Map<String, String> keys = rbf.getKeyBag();

        for (final String outpoint : keys.keySet()) {

            ECKey ecKey = null;

            String[] s = keys.get(outpoint).split("/");
            Log.i("RBF", "path length:" + s.length);
            if (s.length == 4) {
                if (s[3].equals("84")) {
                    HD_Address addr = BIP84Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                    ecKey = addr.getECKey();
                } else {
                    HD_Address addr = BIP49Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                    ecKey = addr.getECKey();
                }
            } else if (s.length == 3) {
                HD_Address hd_address = AddressFactory.getInstance(activity).get(activity.getAccount(), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                String strPrivKey = hd_address.getPrivateKeyString();
                DumpedPrivateKey pk = DumpedPrivateKey.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPrivKey);
                ecKey = pk.getKey();
            } else if (s.length == 2) {
                try {
                    PaymentAddress address = BIP47Util.getInstance(activity).getReceiveAddress(new PaymentCode(s[0]), Integer.parseInt(s[1]));
                    ecKey = address.getReceiveECKey();
                } catch (Exception e) {
                    ;
                }
            } else {
                ;
            }

            Log.i("RBF", "outpoint:" + outpoint);
            Log.i("RBF", "path:" + keys.get(outpoint));
//                Log.i("RBF", "ECKey address from ECKey:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

            if (ecKey != null) {
                if (s.length == 4) {
                    if (s[3].equals("84")) {
                        keyBag84.put(outpoint, ecKey);
                    } else {
                        keyBag49.put(outpoint, ecKey);
                    }
                } else {
                    keyBag.put(outpoint, ecKey);
                }
            } else {
                throw new RuntimeException("ECKey error: cannot process private key");
//                    Log.i("ECKey error", "cannot process private key");
            }

        }

        try {
            List<TransactionInput> inputs = tx.getInputs();
            for (int i = 0; i < inputs.size(); i++) {

                ECKey ecKey ;
                String address;
                if (inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {
                    if (keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {
                        ecKey = keyBag84.get(inputs.get(i).getOutpoint().toString());
                        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        address = segwitAddress.getBech32AsString();
                    } else {
                        ecKey = keyBag49.get(inputs.get(i).getOutpoint().toString());
                        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                        address = segwitAddress.getAddressAsString();
                    }
                } else {
                    ecKey = keyBag.get(inputs.get(i).getOutpoint().toString());
                    address = ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                }
                Log.d("RBF", "pubKey:" + Hex.toHexString(ecKey.getPubKey()));
                Log.d("RBF", "address:" + address);

                if (inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {

                    final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    Script scriptPubKey = segwitAddress.segwitOutputScript();
                    final Script redeemScript = segwitAddress.segwitRedeemScript();
                    System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                    final Script scriptCode = redeemScript.scriptCode();
                    System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                    TransactionSignature sig = tx.calculateWitnessSignature(i, ecKey, scriptCode, Coin.valueOf(inputValues.get(inputs.get(i).getOutpoint().toString())), Transaction.SigHash.ALL, false);
                    final TransactionWitness witness = new TransactionWitness(2);
                    witness.setPush(0, sig.encodeToBitcoin());
                    witness.setPush(1, ecKey.getPubKey());
                    tx.setWitness(i, witness);

                    if (!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                        final ScriptBuilder sigScript = new ScriptBuilder();
                        sigScript.data(redeemScript.getProgram());
                        tx.getInput(i).setScriptSig(sigScript.build());
                        tx.getInput(i).getScriptSig().correctlySpends(tx, i, scriptPubKey, Coin.valueOf(inputValues.get(inputs.get(i).getOutpoint().toString())), Script.ALL_VERIFY_FLAGS);
                    }

                } else {
                    Log.i("RBF", "sign outpoint:" + inputs.get(i).getOutpoint().toString());
                    Log.i("RBF", "ECKey address from keyBag:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

                    Log.i("RBF", "script:" + ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())));
                    Log.i("RBF", "script:" + Hex.toHexString(ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram()));
                    TransactionSignature sig = tx.calculateSignature(i, ecKey, ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram(), Transaction.SigHash.ALL, false);
                    tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(sig, ecKey));
                }

            }
        }
        catch(NoSuchAlgorithmException nsae) {
            return null;
        }

        return tx;
    }
}
