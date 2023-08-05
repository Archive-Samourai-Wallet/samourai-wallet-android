package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UTXOFactory {

    private static Context context = null;
    private static UTXOFactory instance = null;

    private UTXOFactory() {
        ;
    }

    public static UTXOFactory getInstance() {

        if (instance == null) {
            instance = new UTXOFactory();
        }
        return instance;
    }

    public static UTXOFactory getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new UTXOFactory();
        }
        return instance;
    }

    public long getTotalP2PKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2PKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalP2SH_P2WPKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2SH_P2WPKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalP2WPKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2WPKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalPostMix() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        return UTXO.sumValue(utxos);
    }

    public int getCountP2PKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2PKH(true);
        return UTXO.countOutpoints(utxos);
    }

    public int getCountP2SH_P2WPKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2SH_P2WPKH(true);
        return UTXO.countOutpoints(utxos);
    }

    public int getCountP2WPKH() {
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2WPKH(true);
        return UTXO.countOutpoints(utxos);
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
    public List<UTXO> getUTXOS(String address, long neededAmount, int account) {
        APIFactory apiFactory = APIFactory.getInstance(context);
        List<UTXO> utxos;
        if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            if (UTXOFactory.getInstance().getTotalPostMix() > neededAmount) {
                List<UTXO> postmix = apiFactory.getUtxosPostMix(true);
                utxos = new ArrayList<>();
                //Filtering out do not spends
                for (UTXO item : postmix) {
                    UTXO u = new UTXO();
                    u.setPath(item.getPath());
                    for (MyTransactionOutPoint out : item.getOutpoints()) {
                        if (!BlockedUTXO.getInstance().contains(out.getTxHash().toString(), out.getTxOutputN())) {
                            u.getOutpoints().add(out);
                        }
                    }
                    if (u.getOutpoints().size() > 0) {
                        utxos.add(u);
                    }
                }
            } else {
                return null;
            }
        } else if (FormatsUtil.getInstance().isValidBech32(address) && apiFactory.getUtxosP2WPKH(true).size() > 0 && UTXOFactory.getInstance().getTotalP2WPKH() > neededAmount) {
            utxos = new ArrayList<UTXO>(apiFactory.getUtxosP2WPKH(true));
//                    Log.d("SendActivity", "segwit utxos:" + utxos.size());
        } else if (!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress() && apiFactory.getUtxosP2SH_P2WPKH(true).size() > 0 && getTotalP2SH_P2WPKH() > neededAmount) {
            utxos = new ArrayList<UTXO>(apiFactory.getUtxosP2SH_P2WPKH(true));
//                    Log.d("SendActivity", "segwit utxos:" + utxos.size());
        } else if (!FormatsUtil.getInstance().isValidBech32(address) && !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress() && apiFactory.getUtxosP2PKH(true).size() > 0 && getTotalP2PKH() > neededAmount) {
            utxos = new ArrayList<UTXO>(apiFactory.getUtxosP2PKH(true));
//                    Log.d("SendActivity", "p2pkh utxos:" + utxos.size());
        } else {
            utxos = apiFactory.getUtxos(true);
//                    Log.d("SendActivity", "all filtered utxos:" + utxos.size());
        }
        return utxos;
    }
}
