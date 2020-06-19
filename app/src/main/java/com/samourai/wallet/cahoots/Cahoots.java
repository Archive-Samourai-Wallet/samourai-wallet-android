package com.samourai.wallet.cahoots;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.Z85;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import android.util.Log;

import static com.samourai.wallet.util.LogUtil.debug;

public class Cahoots {

    public static final int CAHOOTS_STONEWALLx2 = 0;
    public static final int CAHOOTS_STOWAWAY = 1;

    protected int version = 2;
    protected long ts = -1L;
    protected String strID = null;
    protected int type = -1;
    protected int step = -1;
    protected PSBT psbt = null;
    protected long spendAmount = 0L;
    protected long feeAmount = 0L;
    protected HashMap<String,Long> outpoints = null;
    protected String strDestination = null;
    protected String strPayNymCollab = null;
    protected String strPayNymInit = null;
    protected NetworkParameters params = null;
    protected int account = 0;
    protected int cptyAccount = 0;
    protected byte[] fingerprint = null;
    protected byte[] fingerprintCollab = null;
    protected String strCollabChange = null;

    public Cahoots()    { outpoints = new HashMap<String,Long>(); }

    protected Cahoots(Cahoots c)    {
        this.version = c.getVersion();
        this.ts = c.getTS();
        this.strID = c.getID();
        this.type = c.getType();
        this.step = c.getStep();
        this.psbt = c.getPSBT();
        this.spendAmount = c.getSpendAmount();
        this.feeAmount = c.getFeeAmount();
        this.outpoints = c.getOutpoints();
        this.strDestination = c.strDestination;
        this.strPayNymCollab = c.strPayNymCollab;
        this.strPayNymInit = c.strPayNymInit;
        this.params = c.getParams();
        this.account = c.getAccount();
        this.cptyAccount = c.getCounterpartyAccount();
        this.fingerprint = c.getFingerprint();
        this.fingerprintCollab = c.getFingerprintCollab();
        this.strCollabChange = c.getCollabChange();
    }

    public int getVersion() {
        return version;
    }

    public long getTS() { return ts; }

    public String getID() {
        return strID;
    }

    public int getType() {
        return type;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public PSBT getPSBT() {
        return psbt;
    }

    public void setPSBT(PSBT psbt) {
        this.psbt = psbt;
    }

    public Transaction getTransaction() {
        return psbt.getTransaction();
    }

    public long getSpendAmount() {
        return spendAmount;
    }

    public long getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(long fee)  {
        feeAmount = fee;
    }

    public HashMap<String, Long> getOutpoints() {
        return outpoints;
    }

    public void setOutpoints(HashMap<String, Long> outpoints) {
        this.outpoints = outpoints;
    }

    public String getDestination() {
        return strDestination;
    }

    public void setDestination(String strDestination) {
        this.strDestination = strDestination;
    }

    public String getPayNymCollab() {
        return strPayNymCollab;
    }

    public String getPayNymInit() {
        return strPayNymInit;
    }

    public NetworkParameters getParams() {
        return params;
    }

    public int getAccount() {
        return account;
    }

    public void setCounterpartyAccount(int account) {
        this.cptyAccount = account;
    }

    public int getCounterpartyAccount() {
        return cptyAccount;
    }

    public byte[] getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(byte[] fingerprint) {
        this.fingerprint = fingerprint;
    }

    public byte[] getFingerprintCollab() {
        return fingerprintCollab;
    }

    public void setFingerprintCollab(byte[] fingerprint) {
        this.fingerprintCollab = fingerprint;
    }

    public String getCollabChange() {
        return strCollabChange;
    }

    public void setCollabChange(String strCollabChange) {
        this.strCollabChange = strCollabChange;
    }

    public static boolean isCahoots(JSONObject obj)   {
        try {
            return obj.has("cahoots") && obj.getJSONObject("cahoots").has("type");
        }
        catch(JSONException je) {
            return false;
        }
    }

    public static boolean isCahoots(String s)   {
        try {
            JSONObject obj = new JSONObject(s);
            return isCahoots(obj);
        }
        catch(JSONException je) {
            return false;
        }
    }

    public JSONObject toJSON() {

        JSONObject cObj = new JSONObject();

        try {
            JSONObject obj = new JSONObject();

            obj.put("version", version);
            obj.put("ts", ts);
            obj.put("id", strID);
            obj.put("type", type);
            obj.put("step", step);
            obj.put("spend_amount", spendAmount);
            obj.put("fee_amount", feeAmount);
            JSONArray _outpoints = new JSONArray();
            for(String outpoint : outpoints.keySet())   {
                JSONObject entry = new JSONObject();
                entry.put("outpoint", outpoint);
                entry.put("value", outpoints.get(outpoint));
                _outpoints.put(entry);
            }
            obj.put("outpoints", _outpoints);
            obj.put("dest", strDestination == null ? "" : strDestination);
            if(params instanceof TestNet3Params)    {
                obj.put("params","testnet");
            }
            obj.put("account", account);
            obj.put("cpty_account", cptyAccount);
            if(fingerprint != null)    {
                obj.put("fingerprint", Hex.toHexString(fingerprint));
            }
            if(fingerprintCollab != null)    {
                obj.put("fingerprint_collab", Hex.toHexString(fingerprintCollab));
            }
            obj.put("psbt", psbt == null ? "" : Z85.getInstance().encode(psbt.toGZIP()));
            obj.put("collabChange", strCollabChange == null ? "" : strCollabChange);

            cObj.put("cahoots", obj);
        }
        catch(JSONException je) {
            je.printStackTrace();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }

        return cObj;
    }

    public void fromJSON(JSONObject cObj) {

        try {
            JSONObject obj = null;
            if(cObj.has("cahoots"))    {
                obj = cObj.getJSONObject("cahoots");
            }
            if(obj != null && obj.has("type") && obj.has("step") && obj.has("psbt") && obj.has("ts") && obj.has("id") && obj.has("version") && obj.has("spend_amount"))    {
                if(obj.has("params") && obj.getString("params").equals("testnet"))    {
                    this.params = TestNet3Params.get();
                }
                else    {
                    this.params = MainNetParams.get();
                }
                this.version = obj.getInt("version");
                this.ts = obj.getLong("ts");
                this.strID = obj.getString("id");
                this.type = obj.getInt("type");
                this.step = obj.getInt("step");
                this.spendAmount = obj.getLong("spend_amount");
                this.feeAmount = obj.getLong("fee_amount");
                JSONArray _outpoints = obj.getJSONArray("outpoints");
                for(int i = 0; i < _outpoints.length(); i++)   {
                    JSONObject entry = _outpoints.getJSONObject(i);
                    outpoints.put(entry.getString("outpoint"), entry.getLong("value"));
                }
                this.strDestination = obj.getString("dest");
                if(obj.has("collabChange")) {
                    this.strCollabChange = obj.getString("collabChange");
                }
                else    {
                    this.strCollabChange = "";
                }
                if(obj.has("account"))    {
                    this.account = obj.getInt("account");
                }
                else    {
                    this.account = 0;
                }
                if(obj.has("cpty_account"))    {
                    this.cptyAccount = obj.getInt("cpty_account");
                }
                else    {
                    this.cptyAccount = 0;
                }
                if(obj.has("fingerprint"))    {
                    fingerprint = Hex.decode(obj.getString("fingerprint"));
                }
                if(obj.has("fingerprint_collab"))    {
                    fingerprintCollab = Hex.decode(obj.getString("fingerprint_collab"));
                }
                this.psbt = obj.getString("psbt").equals("") ? null : PSBT.fromBytes(Z85.getInstance().decode(obj.getString("psbt")));
            }
        }
        catch(JSONException je) {
            je.printStackTrace();
        }
        catch(Exception e) {
//            throw new RuntimeException(e);
        }
    }

    protected void signTx(HashMap<String,ECKey> keyBag) {

        Transaction transaction = psbt.getTransaction();
        debug("Cahoots", "signTx:" + transaction.toString());

        for(int i = 0; i < transaction.getInputs().size(); i++)   {

            TransactionInput input = transaction.getInput(i);
            TransactionOutPoint outpoint = input.getOutpoint();
            if(keyBag.containsKey(outpoint.toString())) {

                debug("Cahoots", "signTx outpoint:" + outpoint.toString());

                ECKey key = keyBag.get(outpoint.toString());
                SegwitAddress segwitAddress = new SegwitAddress(key.getPubKey(), params);

                debug("Cahoots", "signTx bech32:" + segwitAddress.getBech32AsString());

                final Script redeemScript = segwitAddress.segWitRedeemScript();
                final Script scriptCode = redeemScript.scriptCode();

                long value = outpoints.get(outpoint.getHash().toString() + "-" + outpoint.getIndex());
                debug("Cahoots", "signTx value:" + value);

                TransactionSignature sig = transaction.calculateWitnessSignature(i, key, scriptCode, Coin.valueOf(value), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, key.getPubKey());
                transaction.setWitness(i, witness);

            }

        }

        psbt.setTransaction(transaction);

    }

}
