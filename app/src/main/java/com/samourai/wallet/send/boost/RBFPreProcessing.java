package com.samourai.wallet.send.boost;

import static java.util.Objects.nonNull;

import android.app.Activity;
import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;

public class RBFPreProcessing implements Callable<String> {

    private static final String TAG = RBFPreProcessing.class.getSimpleName();
    private final SamouraiActivity activity;
    private final String txHash;
    private List<UTXO> utxos;
    private RBFSpend rbf;
    private boolean feeWarning = false;
    private Transaction transaction;
    private final Map<String, Long> inputValues = new HashMap<>();
    private long remainingFee;

    public String getTxHash() {
        return txHash;
    }

    public RBFSpend getRbf() {
        return rbf;
    }

    public boolean isFeeWarning() {
        return feeWarning;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Map<String, Long> getInputValues() {
        return inputValues;
    }

    public long getRemainingFee() {
        return remainingFee;
    }

    private RBFPreProcessing(final SamouraiActivity activity, final String txHash) {
        
        this.activity = activity;
        this.txHash = txHash;
        if (activity.getAccount() == SamouraiAccountIndex.POSTMIX) {
            utxos = APIFactory.getInstance(activity).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(activity).getUtxos(true);
        }
    }
    
    public static RBFPreProcessing create(final SamouraiActivity activity, final String txHash) {
        return new RBFPreProcessing(activity, txHash);
    }
    
    @Override
    public String call() throws Exception {
        return preProcessRBF();
    }

    private String preProcessRBF() {

        Log.d("RBF", "hash:" + txHash);

        rbf = RBFUtil.getInstance().get(txHash);
        Log.d("RBF", "rbf:" + rbf.toJSON().toString());
        final Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(rbf.getSerializedTx()));
        Log.d("RBF", "tx serialized:" + rbf.getSerializedTx());
        Log.d("RBF", "tx inputs:" + tx.getInputs().size());
        Log.d("RBF", "tx outputs:" + tx.getOutputs().size());
        final JSONObject txObj = APIFactory.getInstance(activity).getTxInfo(txHash);
        if (nonNull(txObj) && txObj.has("inputs") && txObj.has("outputs")) {
            try {
                JSONArray inputs = txObj.getJSONArray("inputs");
                JSONArray outputs = txObj.getJSONArray("outputs");

                int p2pkh = 0;
                int p2sh_p2wpkh = 0;
                int p2wpkh = 0;

                for (int i = 0; i < inputs.length(); i++) {
                    if (inputs.getJSONObject(i).has("outpoint") && inputs.getJSONObject(i).getJSONObject("outpoint").has("scriptpubkey")) {
                        String scriptpubkey = inputs.getJSONObject(i).getJSONObject("outpoint").getString("scriptpubkey");
                        Script script = new Script(Hex.decode(scriptpubkey));
                        String address = null;
                        if (Bech32Util.getInstance().isBech32Script(scriptpubkey)) {
                            try {
                                address = Bech32Util.getInstance().getAddressFromScript(scriptpubkey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            address = script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        }
                        if (FormatsUtil.getInstance().isValidBech32(address)) {
                            p2wpkh++;
                        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                            p2sh_p2wpkh++;
                        } else {
                            p2pkh++;
                        }
                    }
                }

                SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                BigInteger estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                long total_inputs = 0L;
                long total_outputs = 0L;
                long fee;
                long total_change = 0L;
                List<String> selfAddresses = new ArrayList<>();

                for (int i = 0; i < inputs.length(); i++) {
                    JSONObject obj = inputs.getJSONObject(i);
                    if (obj.has("outpoint")) {
                        JSONObject objPrev = obj.getJSONObject("outpoint");
                        if (objPrev.has("value")) {
                            total_inputs += objPrev.getLong("value");
                            String key = objPrev.getString("txid") + ":" + objPrev.getLong("vout");
                            inputValues.put(key, objPrev.getLong("value"));
                        }
                    }
                }

                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject obj = outputs.getJSONObject(i);
                    if (obj.has("value")) {
                        total_outputs += obj.getLong("value");

                        String _addr = null;
                        if (obj.has("address")) {
                            _addr = obj.getString("address");
                        }

                        selfAddresses.add(_addr);
                        if (_addr != null && rbf.getChangeAddrs().contains(_addr.toString())) {
                            total_change += obj.getLong("value");
                        }
                    }
                }

                fee = total_inputs - total_outputs;
                if (fee > estimatedFee.longValue()) {
                    feeWarning = true;
                }

                remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;

                Log.d("RBF", "total inputs:" + total_inputs);
                Log.d("RBF", "total outputs:" + total_outputs);
                Log.d("RBF", "total change:" + total_change);
                Log.d("RBF", "fee:" + fee);
                Log.d("RBF", "estimated fee:" + estimatedFee.longValue());
                Log.d("RBF", "fee warning:" + feeWarning);
                Log.d("RBF", "remaining fee:" + remainingFee);

                List<TransactionOutput> txOutputs = new ArrayList<>();
                txOutputs.addAll(tx.getOutputs());

                long remainder = remainingFee;
                if (total_change > remainder) {
                    for (TransactionOutput output : txOutputs) {
                        Script script = output.getScriptPubKey();
                        String scriptPubKey = Hex.toHexString(script.getProgram());
                        Address _p2sh = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                        Address _p2pkh = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                        try {
                            if ((Bech32Util.getInstance().isBech32Script(scriptPubKey) && rbf.getChangeAddrs().contains(Bech32Util.getInstance().getAddressFromScript(scriptPubKey))) || (_p2sh != null && rbf.getChangeAddrs().contains(_p2sh.toString())) || (_p2pkh != null && rbf.getChangeAddrs().contains(_p2pkh.toString()))) {
                                if (output.getValue().longValue() >= (remainder + SamouraiWallet.bDust.longValue())) {
                                    output.setValue(Coin.valueOf(output.getValue().longValue() - remainder));
                                    remainder = 0L;
                                    break;
                                } else {
                                    remainder -= output.getValue().longValue();
                                    output.setValue(Coin.valueOf(0L));      // output will be discarded later
                                }
                            }
                        } catch (Exception e) {
                            ;
                        }

                    }

                }

                //
                // original inputs are not modified
                //
                List<MyTransactionInput> _inputs = new ArrayList<>();
                List<TransactionInput> txInputs = tx.getInputs();
                for (TransactionInput input : txInputs) {
                    MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], input.getOutpoint(), input.getOutpoint().getHash().toString(), (int) input.getOutpoint().getIndex());
                    _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                    _inputs.add(_input);
                    Log.d("RBF", "add outpoint:" + _input.getOutpoint().toString());
                }

                Triple<Integer, Integer, Integer> outpointTypes = null;
                if (remainder > 0L) {
                    List<UTXO> selectedUTXO = new ArrayList<>();
                    long selectedAmount = 0L;
                    int selected = 0;
                    long _remainingFee = remainder;
                    Collections.sort(utxos, new UTXO.UTXOComparator());
                    for (UTXO _utxo : utxos) {

                        Log.d("RBF", "utxo value:" + _utxo.getValue());

                        //
                        // do not select utxo that are change outputs in current rbf tx
                        //
                        boolean isChange = false;
                        boolean isSelf = false;
                        for (MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {
                            if (rbf.containsChangeAddr(outpoint.getAddress())) {
                                Log.d("RBF", "is change:" + outpoint.getAddress());
                                Log.d("RBF", "is change:" + outpoint.getValue().longValue());
                                isChange = true;
                                break;
                            }
                            if (selfAddresses.contains(outpoint.getAddress())) {
                                Log.d("RBF", "is self:" + outpoint.getAddress());
                                Log.d("RBF", "is self:" + outpoint.getValue().longValue());
                                isSelf = true;
                                break;
                            }
                        }
                        if (isChange || isSelf) {
                            continue;
                        }

                        selectedUTXO.add(_utxo);
                        selected += _utxo.getOutpoints().size();
                        Log.d("RBF", "selected utxo:" + selected);
                        selectedAmount += _utxo.getValue();
                        Log.d("RBF", "selected utxo value:" + _utxo.getValue());
                        outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(_utxo.getOutpoints()));
                        p2pkh += outpointTypes.getLeft();
                        p2sh_p2wpkh += outpointTypes.getMiddle();
                        p2wpkh += outpointTypes.getRight();
                        _remainingFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length() == 1 ? 2 : outputs.length()).longValue();
                        Log.d("RBF", "_remaining fee:" + _remainingFee);
                        if (selectedAmount >= (_remainingFee + SamouraiWallet.bDust.longValue())) {
                            break;
                        }
                    }
                    long extraChange = 0L;
                    if (selectedAmount < (_remainingFee + SamouraiWallet.bDust.longValue())) {
                        return activity.getString(R.string.insufficient_funds);
                    } else {
                        extraChange = selectedAmount - _remainingFee;
                        Log.d("RBF", "extra change:" + extraChange);
                    }

                    boolean addedChangeOutput = false;
                    // parent tx didn't have change output
                    if (outputs.length() == 1 && extraChange > 0L) {
                        boolean isSegwitChange = (FormatsUtil.getInstance().isValidBech32(outputs.getJSONObject(0).getString("address")) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outputs.getJSONObject(0).getString("address")).isP2SHAddress()) || PrefsUtil.getInstance(activity).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false;

                        String change_address = null;
                        if (isSegwitChange) {
                            int changeIdx = BIP49Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChange().getAddrIdx();
                            change_address = BIP49Util.getInstance(activity).getAddressAt(AddressFactory.CHANGE_CHAIN, changeIdx).getAddressAsString();
                        } else {
                            int changeIdx = HD_WalletFactory.getInstance(activity).get().getAccount(activity.getAccount()).getChange().getAddrIdx();
                            change_address = HD_WalletFactory.getInstance(activity).get().getAccount(activity.getAccount()).getChange().getAddressAt(changeIdx).getAddressString();
                        }

                        Script toOutputScript = ScriptBuilder.createOutputScript(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), change_address));
                        TransactionOutput output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(extraChange), toOutputScript.getProgram());
                        txOutputs.add(output);
                        addedChangeOutput = true;

                    }
                    // parent tx had change output
                    else {
                        for (TransactionOutput output : txOutputs) {
                            Script script = output.getScriptPubKey();
                            String scriptPubKey = Hex.toHexString(script.getProgram());
                            String _addr = null;
                            if (Bech32Util.getInstance().isBech32Script(scriptPubKey)) {
                                try {
                                    _addr = Bech32Util.getInstance().getAddressFromScript(scriptPubKey);
                                } catch (Exception e) {
                                    ;
                                }
                            }
                            if (_addr == null) {
                                Address _address = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                if (_address == null) {
                                    _address = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                }
                                _addr = _address.toString();
                            }
                            Log.d("RBF", "checking for change:" + _addr);
                            if (rbf.containsChangeAddr(_addr)) {
                                Log.d("RBF", "before extra:" + output.getValue().longValue());
                                output.setValue(Coin.valueOf(extraChange + output.getValue().longValue()));
                                Log.d("RBF", "after extra:" + output.getValue().longValue());
                                addedChangeOutput = true;
                                break;
                            }
                        }
                    }

                    // sanity check
                    if (extraChange > 0L && !addedChangeOutput) {
                        return activity.getString(R.string.cannot_create_change_output);
                    }

                    //
                    // update keyBag w/ any new paths
                    //
                    final HashMap<String, String> keyBag = rbf.getKeyBag();
                    for (UTXO _utxo : selectedUTXO) {

                        for (MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {

                            MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], outpoint, outpoint.getTxHash().toString(), outpoint.getTxOutputN());
                            _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                            _inputs.add(_input);
                            Log.d("RBF", "add selected outpoint:" + _input.getOutpoint().toString());

                            String path = APIFactory.getInstance(activity).getUnspentPaths().get(outpoint.getAddress());
                            if (path != null) {
                                if (FormatsUtil.getInstance().isValidBech32(outpoint.getAddress())) {
                                    rbf.addKey(outpoint.toString(), path + "/84");
                                } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()) != null && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()).isP2SHAddress()) {
                                    rbf.addKey(outpoint.toString(), path + "/49");
                                } else {
                                    rbf.addKey(outpoint.toString(), path);
                                }
                                Log.d("RBF", "outpoint address:" + outpoint.getAddress());
                            } else {
                                String pcode = BIP47Meta.getInstance().getPCode4Addr(outpoint.getAddress());
                                int idx = BIP47Meta.getInstance().getIdx4Addr(outpoint.getAddress());
                                rbf.addKey(outpoint.toString(), pcode + "/" + idx);
                            }

                        }

                    }
                    rbf.setKeyBag(keyBag);

                }

                //
                // BIP69 sort of outputs/inputs
                //
                transaction = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams());
                List<TransactionOutput> _txOutputs = new ArrayList<>();
                _txOutputs.addAll(txOutputs);
                Collections.sort(_txOutputs, new BIP69OutputComparator());
                for (TransactionOutput to : _txOutputs) {
                    // zero value outputs discarded here
                    if (to.getValue().longValue() > 0L) {
                        transaction.addOutput(to);
                    }
                }

                List<MyTransactionInput> __inputs = new ArrayList<>();
                __inputs.addAll(_inputs);
                Collections.sort(__inputs, new SendFactory.BIP69InputComparator());
                for (TransactionInput input : __inputs) {
                    transaction.addInput(input);
                }

                FeeUtil.getInstance().setSuggestedFee(suggestedFee);

            } catch (final JSONException je) {
                return "rbf:" + je.getMessage();
            }

        } else {
            return activity.getString(R.string.cpfp_cannot_retrieve_tx);
        }
        
        return null;
    }

}
