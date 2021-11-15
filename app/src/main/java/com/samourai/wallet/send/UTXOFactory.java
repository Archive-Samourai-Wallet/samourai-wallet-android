package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bouncycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;

public class UTXOFactory {

    private static Context context = null;

    private static UTXOFactory instance = null;

    private static HashMap<String, UTXO> p2pkh = null;
    private static HashMap<String, UTXO> p2sh_p2wpkh = null;
    private static HashMap<String, UTXO> p2wpkh = null;
    private static HashMap<String, UTXO> postMix_clean = null;
    private static HashMap<String, UTXO> postMix_toxic = null;
    private static HashMap<String, UTXO> preMix = null;
    private static HashMap<String, UTXO> badBank = null;
    private static long lastUpdate = 0;

    private UTXOFactory() {
        ;
    }

    public static UTXOFactory getInstance() {

        if (instance == null) {
            instance = new UTXOFactory();

            p2pkh = new HashMap<String, UTXO>();
            p2sh_p2wpkh = new HashMap<String, UTXO>();
            p2wpkh = new HashMap<String, UTXO>();
            postMix_clean = new HashMap<String, UTXO>();
            postMix_toxic = new HashMap<String, UTXO>();
            preMix = new HashMap<String, UTXO>();
            badBank = new HashMap<String, UTXO>();
        }

        return instance;
    }

    public static UTXOFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new UTXOFactory();

            p2pkh = new HashMap<String, UTXO>();
            p2sh_p2wpkh = new HashMap<String, UTXO>();
            p2wpkh = new HashMap<String, UTXO>();
            postMix_clean = new HashMap<String, UTXO>();
            postMix_toxic = new HashMap<String, UTXO>();
            preMix = new HashMap<String, UTXO>();
            badBank = new HashMap<String, UTXO>();
        }

        return instance;
    }

    public void clear() {
        p2pkh.clear();
        p2sh_p2wpkh.clear();
        p2wpkh.clear();
        postMix_clean.clear();
        postMix_toxic.clear();
        preMix.clear();
        badBank.clear();
    }

    public HashMap<String, UTXO> getPostMixClean() {
        return postMix_clean;
    }

    public HashMap<String, UTXO> getPostMixToxic() {
        return postMix_toxic;
    }

    public HashMap<String, UTXO> getPreMix() {
        return preMix;
    }

    public HashMap<String, UTXO> getBadBankMix() {
        return badBank;
    }

    public HashMap<String, UTXO> getP2PKH() {
        return p2pkh;
    }

    public HashMap<String, UTXO> getP2SH_P2WPKH() {
        return p2sh_p2wpkh;
    }

    public HashMap<String, UTXO> getP2WPKH() {
        return p2wpkh;
    }

    public HashMap<String, UTXO> getAllPostMix() {
        HashMap<String, UTXO> ret = new HashMap<String, UTXO>();
        ret.putAll(postMix_clean);
        ret.putAll(postMix_toxic);
        return ret;
    }

    public void addP2PKH(String hash, int id, String script, UTXO utxo) {
        if (!BlockedUTXO.getInstance().contains(hash, id)) {
            p2pkh.put(script, utxo);
            onChange();
        }
    }

    public void addP2SH_P2WPKH(String hash, int id, String script, UTXO utxo) {
        if (!BlockedUTXO.getInstance().contains(hash, id)) {
            p2sh_p2wpkh.put(script, utxo);
            onChange();
        }
    }

    public void addP2WPKH(String hash, int id, String script, UTXO utxo) {
        if (!BlockedUTXO.getInstance().contains(hash, id)) {
            p2wpkh.put(script, utxo);
            onChange();
        }
    }

    public void addPostMix(String hash, int id, String script, UTXO utxo) {
        if (!BlockedUTXO.getInstance().containsPostMix(hash, id)) {
            if (isPostmixToxic(utxo)) {
                postMix_toxic.put(script, utxo);
            } else {
                postMix_clean.put(script, utxo);
            }
            onChange();
        }
    }

    public void addPreMix(String hash, int id, String script, UTXO utxo) {
        preMix.put(script, utxo);
        onChange();
    }

    public void addBadBank(String hash, int id, String script, UTXO utxo) {
        badBank.put(script, utxo);
        onChange();
    }

    public long getTotalP2PKH() {
        HashMap<String, UTXO> utxos = getP2PKH();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2SH_P2WPKH() {
        HashMap<String, UTXO> utxos = getP2SH_P2WPKH();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2WPKH() {
        HashMap<String, UTXO> utxos = getP2WPKH();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPostMixClean() {
        HashMap<String, UTXO> utxos = getPostMixClean();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountP2PKH() {
        HashMap<String, UTXO> utxos = getP2PKH();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2SH_P2WPKH() {
        HashMap<String, UTXO> utxos = getP2SH_P2WPKH();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2WPKH() {
        HashMap<String, UTXO> utxos = getP2WPKH();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPostMixClean() {
        HashMap<String, UTXO> utxos = getPostMixClean();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public long getTotalPostMixToxic() {
        HashMap<String, UTXO> utxos = getPostMixToxic();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPreMix() {
        HashMap<String, UTXO> utxos = getPreMix();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountPostMixToxic() {
        HashMap<String, UTXO> utxos = getPostMixToxic();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPreMix() {
        HashMap<String, UTXO> utxos = getPreMix();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public long getTotalPostMix() {
        HashMap<String, UTXO> utxos = getAllPostMix();
        long ret = 0L;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountPostMix() {
        HashMap<String, UTXO> utxos = getAllPostMix();
        int ret = 0;

        for (UTXO utxo : utxos.values()) {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    private boolean isPostmixToxic(UTXO utxo) {

        String path = utxo.getPath();

        // bip47 receive
        if (path == null || path.length() == 0) {
            return false;
        }
        // any account receive
        else if (path.startsWith("M/0/")) {
            return false;
        }
        // assume starts with "M/1/"
        // any account change
        else {
            return true;
        }

    }

    public void markUTXOAsNonSpendable(String hexTx, int account) {

        HashMap<String, Long> utxos = new HashMap<String, Long>();
        int POST_MIX = WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix();
        int BAD_BANK = WhirlpoolMeta.getInstance(context).getWhirlpoolBadBank();

        List<UTXO> utxoList;
        if (account == POST_MIX) {
            utxoList = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else if (account == BAD_BANK) {
            utxoList = APIFactory.getInstance(context).getUtxosBadBank(true);
        } else {
            utxoList = APIFactory.getInstance(context).getUtxos(true);
        }

        for (UTXO utxo : utxoList) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxos.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
            }
        }

        Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(hexTx));
        for (TransactionInput input : tx.getInputs()) {
            String hash = input.getOutpoint().getHash().toString();
            int idx = (int) input.getOutpoint().getIndex();
            String blockedId = hash.concat("-").concat(String.valueOf(idx));
            Long value = utxos.get(blockedId);
            if (value != null) {
                if (account == POST_MIX) {
                    BlockedUTXO.getInstance().addPostMix(hash, idx, value);
                } else if (account == BAD_BANK) {
                    BlockedUTXO.getInstance().addBadBank(hash, idx, value);
                } else {
                    BlockedUTXO.getInstance().add(hash, idx, value);
                }
            }
        }

    }

    private void onChange() {
        // required for Whirlpool
        lastUpdate = System.currentTimeMillis();
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
