package com.samourai.wallet.send.boost;

import android.util.Log;

import com.google.common.collect.Sets;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.constants.SamouraiAccountIndex;
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

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;

import androidx.annotation.Nullable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class RBFPreProcessing implements Callable<String> {

    private static final String TAG = RBFPreProcessing.class.getSimpleName();
    private final SamouraiActivity activity;
    private final String txHash;
    private List<UTXO> utxos;
    private RBFSpend rbf;
    private boolean feeWarning = false;
    private Transaction transaction;
    private final Map<String, Long> inputValues = new LinkedHashMap<>();

    //useful in RBFProcessing for postmix account to determine how to compute private key for new utxo in tx
    private final Map<String, String> extraInputs = new LinkedHashMap();
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

    public Map<String, String> getExtraInputs() {
        return extraInputs;
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

        final NetworkParameters networkParameters = SamouraiWallet.getInstance().getCurrentNetworkParams();

        final Transaction tx = new Transaction(networkParameters, Hex.decode(rbf.getSerializedTx()));
        Log.d("RBF", "tx serialized:" + rbf.getSerializedTx());
        Log.d("RBF", "tx inputs:" + tx.getInputs().size());
        Log.d("RBF", "tx outputs:" + tx.getOutputs().size());
        final JSONObject txObj = APIFactory.getInstance(activity).getTxInfo(txHash);
        if (nonNull(txObj) && txObj.has("inputs") && txObj.has("outputs")) {
            final SuggestedFee keepCurrentSuggestedFee = FeeUtil.getInstance().getSuggestedFee();
            try {
                JSONArray inputs = txObj.getJSONArray("inputs");
                JSONArray outputs = txObj.getJSONArray("outputs");

                int p2pkh = 0; // first format addr // Pay To Public Key Hash // starts with 1 or 2 // BIP44
                // p2sh : starts with 3 BIP49
                int p2sh_p2wpkh = 0; // BIP49 segwit compatible
                int p2wpkh = 0; // ScriptPubKey segwit native // starts with bc1q // encoding Bech32 // BIP84
                // P2TR : Taproot // starts with bc1p

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
                            address = script.getToAddress(networkParameters).toString();
                        }
                        if (FormatsUtil.getInstance().isValidBech32(address)) {
                            p2wpkh++;
                        } else if (Address.fromBase58(networkParameters, address).isP2SHAddress()) {
                            p2sh_p2wpkh++;
                        } else {
                            p2pkh++;
                        }
                    }
                }

                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                final BigInteger estimatedInitialFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                long total_inputs = 0L;
                long total_outputs = 0L;
                long currentFee;
                long total_change = 0L;
                final Collection<String> outAddresses = Sets.newHashSet();

                for (int i = 0; i < inputs.length(); i++) {
                    JSONObject obj = inputs.getJSONObject(i);
                    if (obj.has("outpoint")) {
                        JSONObject objPrev = obj.getJSONObject("outpoint");
                        if (objPrev.has("value")) {
                            final long amount = objPrev.getLong("value");
                            total_inputs += amount;
                            final String key = objPrev.getString("txid") + ":" + objPrev.getLong("vout");
                            inputValues.put(key, objPrev.getLong("value"));
                        }
                    }
                }

                for (int i = 0; i < outputs.length(); i++) {
                    final JSONObject obj = outputs.getJSONObject(i);
                    if (obj.has("value")) {
                        final long amount = obj.getLong("value");
                        total_outputs += amount;

                        String _addr = null;
                        if (obj.has("address")) {
                            _addr = obj.getString("address");
                        }

                        outAddresses.add(_addr);
                        if (nonNull(_addr) && rbf.containsChangeAddr(_addr)) {
                            total_change += amount;
                        }
                    }
                }

                currentFee = total_inputs - total_outputs;
                if (currentFee > estimatedInitialFee.longValue()) {
                    feeWarning = true;
                }

                remainingFee = (estimatedInitialFee.longValue() > currentFee) ? estimatedInitialFee.longValue() - currentFee : 0L;

                Log.d("RBF", "total inputs:" + total_inputs);
                Log.d("RBF", "total outputs:" + total_outputs);
                Log.d("RBF", "total change:" + total_change);
                Log.d("RBF", "fee:" + currentFee);
                Log.d("RBF", "estimated fee:" + estimatedInitialFee.longValue());
                Log.d("RBF", "fee warning:" + feeWarning);
                Log.d("RBF", "remaining fee:" + remainingFee);

                final List<TransactionOutput> txOutputs = new ArrayList<>();
                txOutputs.addAll(tx.getOutputs());

                long remainder = remainingFee;
                if (total_change > remainder) {
                    for (final TransactionOutput output : txOutputs) {
                        final Script script = output.getScriptPubKey();
                        final String scriptPubKey = Hex.toHexString(script.getProgram());
                        final Address _p2sh = output.getAddressFromP2SH(networkParameters);
                        final Address _p2pkh = output.getAddressFromP2PKHScript(networkParameters);
                        try {
                            if ( (Bech32Util.getInstance().isBech32Script(scriptPubKey) && rbf.containsChangeAddr((Bech32Util.getInstance().getAddressFromScript(scriptPubKey))) ) ||
                                    (_p2sh != null && rbf.containsChangeAddr(_p2sh.toString())) ||
                                    (_p2pkh != null && rbf.containsChangeAddr(_p2pkh.toString()))) {

                                final long currentAmount = output.getValue().longValue();
                                if (currentAmount >= (remainder + SamouraiWallet.bDust.longValue())) {
                                    output.setValue(Coin.valueOf(currentAmount - remainder));
                                    remainder = 0L;
                                    break;
                                } else {
                                    remainder -= currentAmount;
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
                final List<MyTransactionInput> _inputs = new ArrayList<>();

                for (final TransactionInput input : tx.getInputs()) {
                    final MyTransactionInput _input = new MyTransactionInput(
                            networkParameters,
                            null,
                            new byte[0],
                            input.getOutpoint(),
                            input.getOutpoint().getHash().toString(),
                            (int) input.getOutpoint().getIndex());

                    _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                    _inputs.add(_input);
                    Log.d("RBF", "add outpoint:" + _input.getOutpoint().toString());
                }

                if (remainder > 0L) {

                    final List<UTXO> selectedUTXO = new ArrayList<>();

                    long selectedAmount = 0L;
                    int selectedCount = 0;
                    long adjRemainingFee = remainder;
                    Collections.sort(utxos, new UTXO.UTXOComparator());
                    for (final UTXO _utxo : utxos) {

                        Log.d("RBF", "utxo value:" + _utxo.getValue());

                        //
                        // do not select utxo that are change outputs in current rbf tx
                        //
                        boolean isChange = false;
                        boolean isSelf = false;
                        final List<MyTransactionOutPoint> utxoOutpoints = _utxo.getOutpoints();
                        for (final MyTransactionOutPoint utxoOutpoint : utxoOutpoints) {
                            if (rbf.containsChangeAddr(utxoOutpoint.getAddress())) {
                                Log.d("RBF", "is change:" + utxoOutpoint.getAddress());
                                Log.d("RBF", "is change:" + utxoOutpoint.getValue().longValue());
                                isChange = true;
                                break;
                            }
                            if (outAddresses.contains(utxoOutpoint.getAddress())) {
                                Log.d("RBF", "is self:" + utxoOutpoint.getAddress());
                                Log.d("RBF", "is self:" + utxoOutpoint.getValue().longValue());
                                isSelf = true;
                                break;
                            }
                        }
                        if (isChange || isSelf) {
                            continue;
                        }

                        selectedUTXO.add(_utxo);
                        selectedCount += utxoOutpoints.size();
                        Log.d("RBF", "selected utxo:" + selectedCount);
                        selectedAmount += _utxo.getValue();
                        Log.d("RBF", "selected utxo value:" + _utxo.getValue());

                        final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxoOutpoints));
                        p2pkh += outpointTypes.getLeft();
                        p2sh_p2wpkh += outpointTypes.getMiddle();
                        p2wpkh += outpointTypes.getRight();

                        final BigInteger actualizedEstFee = FeeUtil.getInstance().estimatedFeeSegwit(
                                p2pkh,
                                p2sh_p2wpkh,
                                p2wpkh,
                                outputs.length() == 1 ? 2 : outputs.length());
                        final BigInteger extraFee = actualizedEstFee.subtract(estimatedInitialFee);

                        if (selectedAmount <= extraFee.longValue() + SamouraiWallet.bDust.longValue()) {
                            break; // let's common, extraFee is bigger than amount of associated extra utxo...
                        }

                        adjRemainingFee = remainder + extraFee.longValue();
                        Log.d("RBF", "_remaining fee:" + adjRemainingFee);
                        if (selectedAmount >= (adjRemainingFee + SamouraiWallet.bDust.longValue())) {
                            break;
                        }
                    }
                    long extraChange = 0L;
                    if (selectedAmount < (adjRemainingFee + SamouraiWallet.bDust.longValue())) {
                        return activity.getString(R.string.insufficient_funds);
                    } else {
                        extraChange = selectedAmount - adjRemainingFee;
                        Log.d("RBF", "extra change:" + extraChange);
                    }

                    boolean addedChangeOutput = false;
                    if (extraChange > 0L) {
                        // parent tx didn't have change output
                        if (outputs.length() == 1) {

                            final String addressFromTx = outputs.getJSONObject(0).getString("address");
                            final boolean isSegwitChange = FormatsUtil.getInstance().isValidBech32(addressFromTx) ||
                                    Address.fromBase58(networkParameters, addressFromTx).isP2SHAddress() ||
                                    PrefsUtil.getInstance(activity).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false;

                            final String change_address;
                            if (isSegwitChange) {
                                final int changeIdx = BIP49Util.getInstance(activity).getWallet().getAccount(activity.getAccount()).getChange().getAddrIdx();
                                change_address = BIP49Util.getInstance(activity).getAddressAt(AddressFactory.CHANGE_CHAIN, changeIdx).getAddressAsString();
                            } else {
                                final int changeIdx = HD_WalletFactory.getInstance(activity).get().getAccount(activity.getAccount()).getChange().getAddrIdx();
                                change_address = HD_WalletFactory.getInstance(activity).get().getAccount(activity.getAccount()).getChange().getAddressAt(changeIdx).getAddressString();
                            }

                            final Script toOutputScript = ScriptBuilder.createOutputScript(Address.fromBase58(networkParameters, change_address));
                            TransactionOutput output = new TransactionOutput(networkParameters, null, Coin.valueOf(extraChange), toOutputScript.getProgram());
                            txOutputs.add(output);
                            addedChangeOutput = true;

                        } else { // parent tx had change output
                            for (final TransactionOutput output : txOutputs) {
                                String _addr = getAddress(output);
                                Log.d("RBF", "checking for change:" + _addr);
                                if (rbf.containsChangeAddr(_addr)) {
                                    final long amount = output.getValue().longValue();
                                    Log.d("RBF", "before extra:" + amount);
                                    output.setValue(Coin.valueOf(extraChange + amount));
                                    Log.d("RBF", "after extra:" + output.getValue().longValue());
                                    addedChangeOutput = true;
                                    break;
                                }
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
                    //final HashMap<String, String> keyBag = rbf.getKeyBag();
                    for (final UTXO _utxo : selectedUTXO) {

                        for (final MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {

                            final MyTransactionInput _input = new MyTransactionInput(
                                    networkParameters,
                                    null,
                                    new byte[0],
                                    outpoint,
                                    outpoint.getTxHash().toString(),
                                    outpoint.getTxOutputN());

                            _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                            _inputs.add(_input);
                            _input.setValue(BigInteger.valueOf(outpoint.getValue().getValue()));
                            inputValues.put(_input.getOutpoint().toString(), outpoint.getValue().getValue());
                            extraInputs.put(outpoint.toString(), getAddress(outpoint.getConnectedOutput()));
                            Log.d("RBF", "add selected outpoint:" + _input.getOutpoint().toString());

                            final String path = APIFactory.getInstance(activity).getUnspentPaths().get(outpoint.getAddress());
                            if (nonNull(path)) {
                                if (FormatsUtil.getInstance().isValidBech32(outpoint.getAddress())) {
                                    rbf.addKey(outpoint.toString(), path + "/84");
                                } else if (nonNull(Address.fromBase58(networkParameters, outpoint.getAddress())) &&
                                        Address.fromBase58(networkParameters, outpoint.getAddress()).isP2SHAddress()) {
                                    rbf.addKey(outpoint.toString(), path + "/49");
                                } else {
                                    rbf.addKey(outpoint.toString(), path);
                                }
                                Log.d("RBF", "outpoint address:" + outpoint.getAddress());
                            } else {
                                final String pcode = BIP47Meta.getInstance().getPCode4Addr(outpoint.getAddress());
                                final int idx = BIP47Meta.getInstance().getIdx4Addr(outpoint.getAddress());
                                rbf.addKey(outpoint.toString(), pcode + "/" + idx);
                            }

                        }

                    }
                    //rbf.setKeyBag(keyBag);

                }

                //
                // BIP69 sort of outputs/inputs
                //
                transaction = new Transaction(networkParameters);
                final List<TransactionOutput> _txOutputs = new ArrayList<>();
                _txOutputs.addAll(txOutputs);
                Collections.sort(_txOutputs, new BIP69OutputComparator());
                for (final TransactionOutput to : _txOutputs) {
                    // zero value outputs discarded here
                    if (to.getValue().longValue() > 0L) {
                        transaction.addOutput(to);
                    }
                }

                final List<MyTransactionInput> __inputs = new ArrayList<>();
                __inputs.addAll(_inputs);
                Collections.sort(__inputs, new SendFactory.BIP69InputComparator());
                for (final TransactionInput input : __inputs) {
                    transaction.addInput(input);
                }

            } catch (final Exception e) {
                return "rbf:" + e.getMessage();
            } finally {
                FeeUtil.getInstance().setSuggestedFee(keepCurrentSuggestedFee);
            }

        } else {
            return activity.getString(R.string.cpfp_cannot_retrieve_tx);
        }
        
        return null;
    }

    @Nullable
    private static String getAddress(final TransactionOutput output) {

        final NetworkParameters networkParameters = SamouraiWallet.getInstance().getCurrentNetworkParams();

        final Script script = output.getScriptPubKey();
        final String scriptPubKey = Hex.toHexString(script.getProgram());
        String _addr = null;
        if (Bech32Util.getInstance().isBech32Script(scriptPubKey)) {
            try {
                _addr = Bech32Util.getInstance().getAddressFromScript(scriptPubKey);
            } catch (Exception e) {
                ;
            }
        }
        if (isNull(_addr)) {
            Address _address = output.getAddressFromP2PKHScript(networkParameters);
            if (isNull(_address)) {
                _address = output.getAddressFromP2SH(networkParameters);
            }
            _addr = _address.toString();
        }
        return _addr;
    }

}
