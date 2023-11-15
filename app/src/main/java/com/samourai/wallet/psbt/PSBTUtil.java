package com.samourai.wallet.psbt;

import static com.samourai.wallet.util.tech.LogUtil.debug;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PSBTUtil {

    private static Context context = null;

    private static PSBTUtil instance = null;

    private PSBTUtil()    { ; }

    public static PSBTUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new PSBTUtil();
        }

        return instance;
    }

    public void doPSBT(final String strPSBT) throws Exception    {

        PSBT _psbt = null;
        PSBT.setDebug(true);
        try {
            _psbt = PSBT.fromBytes(Hex.decode(strPSBT), SamouraiWallet.getInstance().getCurrentNetworkParams());
        }
        catch(Exception e) {
            Toast.makeText(context, R.string.psbt_error, Toast.LENGTH_SHORT).show();
            return;
        }
        final PSBT psbt = PSBT.fromBytes(_psbt.toBytes(), SamouraiWallet.getInstance().getCurrentNetworkParams());

        final EditText edPSBT = new EditText(context);
        edPSBT.setSingleLine(false);
        edPSBT.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edPSBT.setLines(10);
        edPSBT.setHint(R.string.PSBT);
        edPSBT.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edPSBT.setSelection(0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edPSBT.addTextChangedListener(textWatcher);
        edPSBT.setText(psbt.dump());

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setView(edPSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                })
                .setNegativeButton(R.string.psbt_sign_tx, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        String unsignedHash = psbt.getTransaction().getHashAsString();
                        String unsignedHex = new String(Hex.encode(psbt.getTransaction().bitcoinSerialize()));
                        Transaction tx = doPSBTSignTx(psbt);
                        debug("PSBTUtil", "unsigned tx hash:" + unsignedHash);
                        debug("PSBTUtil", "unsigned tx:" + unsignedHex);
                        debug("PSBTUtil", "  signed tx hash:" + tx.getHashAsString());
                        String signedHex = new String(Hex.encode(tx.bitcoinSerialize()));
                        debug("PSBTUtil", "  signed tx:" + signedHex);

                        final TextView tvHexTx = new TextView(context);
                        float scale = context.getResources().getDisplayMetrics().density;
                        tvHexTx.setSingleLine(false);
                        tvHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        tvHexTx.setLines(10);
                        tvHexTx.setGravity(Gravity.START);
                        tvHexTx.setText(signedHex);
                        tvHexTx.setPadding((int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f));

                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(tvHexTx)
                                .setCancelable(false)
                                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = null;
                                        clip = android.content.ClipData.newPlainText("tx", signedHex);
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                });
                        if(!((Activity)context).isFinishing())    {
                            dlg.show();
                        }

                    }

                });
        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    public Transaction doPSBTSignTx(PSBT psbt)    {

        Transaction tx = psbt.getTransaction();

        HashMap<String,ECKey> keyBag = new HashMap<>();
        HashMap<String,Long> amountBag = new HashMap<>();
        String scriptType = "P2WPKH";
        HashMap<String,String> scriptTypeBag = new HashMap<>();

        SegwitAddress address = null;
        long value = 0L;
        int idx = 0;
        int txInputNum = 0;
        ECKey eckeyPriv = null;
        List<PSBTEntry> psbtInputs = psbt.getPsbtInputs();
        List<TransactionInput> txInputs = tx.getInputs();
        String[] s = null;

        for(PSBTEntry entry : psbtInputs) {

            if(entry.getKeyType() == null) {
                continue;
            }

            else if(org.spongycastle.util.encoders.Hex.toHexString(entry.getKeyType()).equals("00")) {
                Transaction txData = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), entry.getData());
                if (txData.getOutput(psbt.getTransaction().getInput(txInputNum).getOutpoint().getIndex()).getValue() != null) {
                    value = txData.getOutput(psbt.getTransaction().getInput(txInputNum).getOutpoint().getIndex()).getValue().value;
                    scriptType = "P2PKH";
                }
                if (txData.getOutput(psbt.getTransaction().getInput(txInputNum).getOutpoint().getIndex()).getScriptPubKey().isSentToP2WPKH()) {
                    scriptType = "P2WPKH";
                }
            }
            else if(org.spongycastle.util.encoders.Hex.toHexString(entry.getKeyType()).equals("01")) {

                address = null;

                byte[] data = entry.getData();
                Pair<Long,Byte[]> pair = PSBT.readSegwitInputUTXO(data);
                value = pair.getLeft();
                debug("PSBTUtil", "value:" + value);
            }
            else if(org.spongycastle.util.encoders.Hex.toHexString(entry.getKeyType()).equals("04"))
                scriptType = "P2SH";
            else if(entry.getKeyType() != null && Hex.toHexString(entry.getKeyType()).equals("06")) {

                byte[] data = entry.getData();
                String path = PSBT.readBIP32Derivation(data);
                s = path.replaceAll("'", "").split("/");
                debug("PSBTUtil", "path:" + path);
                // BIP84Util returns pubkey only, use bip84Wallet to get privkey
                if (s[1].equals("84") && scriptType.equals("P2SH")) {
                    HD_Address addr = BIP84Util.getInstance(context).getWallet().getAccount(Integer.parseInt(s[3])).getChain(Integer.parseInt(s[4])).getAddressAt(Integer.parseInt(s[5]));
                    address = new SegwitAddress(addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

                    debug("PSBTUtil", "address:" + address.getAddress());
                    eckeyPriv = address.getECKey();
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.hasPrivKey());
                }
                if (s[1].equals("84") && scriptType.equals("P2PKH")) {
                    HD_Address addr = BIP84Util.getInstance(context).getWallet().getAccount(Integer.parseInt(s[3])).getChain(Integer.parseInt(s[4])).getAddressAt(Integer.parseInt(s[5]));

                    debug("PSBTUtil", "address:" + addr.getAddress());
                    eckeyPriv = addr.getECKey();
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.hasPrivKey());
                }
                else if (s[1].equals("84") && scriptType.equals("P2WPKH")) {
                    HD_Wallet bip84Wallet = BIP84Util.getInstance(context).getWallet();
                    HD_Address addr = bip84Wallet.getAccount(Integer.parseInt(s[3])).getChain(Integer.parseInt(s[4])).getAddressAt(Integer.parseInt(s[5]));
                    address = new SegwitAddress(addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    debug("PSBTUtil", "address:" + address.getBech32AsString());
                    eckeyPriv = address.getECKey();
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.hasPrivKey());
                }
                else if (s[1].equals("49")) {
                    HD_Wallet bip49Wallet = BIP49Util.getInstance(context).getWallet();
                    HD_Address addr = bip49Wallet.getAccount(Integer.parseInt(s[3])).getChain(Integer.parseInt(s[4])).getAddressAt(Integer.parseInt(s[5]));
                    address = new SegwitAddress(addr.getECKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    debug("PSBTUtil", "address:" + address.getAddress());
                    eckeyPriv = address.getECKey();
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.hasPrivKey());
                }
                else if ((s[1].equals("44"))) {
                    HD_Wallet bip44w = HD_WalletFactory.getInstance(context).get();
                    HD_Address addr = bip44w.getAccount(Integer.parseInt(s[3])).getChain(Integer.parseInt(s[4])).getAddressAt(Integer.parseInt(s[5]));
                    debug("PSBTUtil", "address:" + addr.getAddress());
                    eckeyPriv = addr.getECKey();
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.hasPrivKey());
                    debug("PSBTUtil", "hasPrivKey:" + eckeyPriv.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams()));
                }
            }

            if(eckeyPriv != null) {
                TransactionInput input = txInputs.get(idx);
                keyBag.put(input.getOutpoint().toString(), eckeyPriv);
                amountBag.put(input.getOutpoint().toString(), value);
                scriptTypeBag.put(input.getOutpoint().toString(), scriptType);
                eckeyPriv = null;
                txInputNum++;
                idx++;
                scriptType = "P2WPKH";
            }
        }

        tx = signTx(tx, keyBag, amountBag, s, scriptTypeBag);

        return tx;
    }

    public Transaction signTx(Transaction transaction, HashMap<String,ECKey> keyBag, HashMap<String,Long> amountBag, String[] path, HashMap<String, String> scriptTypeBag) {

        for(int i = 0; i < transaction.getInputs().size(); i++)   {

            TransactionInput input = transaction.getInput(i);
            TransactionOutPoint outpoint = input.getOutpoint();
            if(keyBag.containsKey(outpoint.toString())) {
                ECKey key = keyBag.get(outpoint.toString());
                if (path[1].equals("44") || Objects.equals(scriptTypeBag.get(outpoint.toString()),"P2PKH")) {
                    TransactionSignature sig = transaction.calculateSignature(i, key, ScriptBuilder.createOutputScript(key.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram(), Transaction.SigHash.ALL, false);
                    transaction.getInput(i).setScriptSig(ScriptBuilder.createInputScript(sig, key));
                }
                else if (path[1].equals("84") || path[1].equals("49")) {
                    SegwitAddress segwitAddress = new SegwitAddress(key.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    final Script redeemScript = segwitAddress.segwitRedeemScript();
                    final Script scriptCode = redeemScript.scriptCode();

                    long value = amountBag.get(outpoint.toString());

                    TransactionSignature sig =  transaction.calculateWitnessSignature(i, key, scriptCode, Coin.valueOf(value), Transaction.SigHash.ALL, false);
                    final TransactionWitness witness = new TransactionWitness(2);
                    witness.setPush(0, sig.encodeToBitcoin());
                    witness.setPush(1, key.getPubKey());
                    transaction.setWitness(i, witness);

                    if(path[1].equals("49") || Objects.equals(scriptTypeBag.get(outpoint.toString()), "P2SH"))    {
                        try {
                            Script scriptPubKey = segwitAddress.segwitOutputScript();
                            final ScriptBuilder sigScript = new ScriptBuilder();
                            sigScript.data(redeemScript.getProgram());
                            transaction.getInput(i).setScriptSig(sigScript.build());
                            transaction.getInput(i).getScriptSig().correctlySpends(transaction, i, scriptPubKey, Coin.valueOf(value), Script.ALL_VERIFY_FLAGS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return transaction;

    }

}
