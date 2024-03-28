package com.samourai.wallet.send;

import android.content.Context;

import com.google.common.collect.Lists;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.constants.SamouraiAccountIndex;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2PKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalP2SH_P2WPKH() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2SH_P2WPKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalP2WPKH() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2WPKH(true);
        return UTXO.sumValue(utxos);
    }

    public long getTotalPostMix() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        return UTXO.sumValue(utxos);
    }

    public int getCountP2PKH() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2PKH(true);
        return UTXO.countOutpoints(utxos);
    }

    public int getCountP2SH_P2WPKH() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2SH_P2WPKH(true);
        return UTXO.countOutpoints(utxos);
    }

    public int getCountP2WPKH() {
        final List<UTXO> utxos = APIFactory.getInstance(context).getUtxosP2WPKH(true);
        return UTXO.countOutpoints(utxos);
    }

    public List<UTXO> getUtxos(
            final int account,
            final int utxoType) {

        final APIFactory instance = APIFactory.getInstance(context);
        if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            return instance.getUtxosPostMix(true);
        } else if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolBadBank()) {
            return instance.getUtxosBadBank(true);
        } else if (utxoType == 84) {
            return instance.getUtxosP2WPKH(true);
        } else if (utxoType == 49) {
            return instance.getUtxosP2SH_P2WPKH(true);
        } else {
            return instance.getUtxosP2PKH(true);
        }
    }

    public Map<String, Long> getAmountsByCoinId(final int account,
                                                final boolean onlyUnlockedUtxo) {

        final List<UTXO> utxoList = Lists.newArrayList();
        if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxoList.addAll(APIFactory.getInstance(context).getUtxosPostMix(onlyUnlockedUtxo));
        } else if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolBadBank()) {
            utxoList.addAll(APIFactory.getInstance(context).getUtxosBadBank(onlyUnlockedUtxo));
        } else {
            utxoList.addAll(APIFactory.getInstance(context).getUtxos(onlyUnlockedUtxo));
        }

        final Map<String, Long> coinIdToAmount = new HashMap<>();
        for (final UTXO utxo : utxoList) {
            for (final MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                final String coinId =
                        outpoint.getTxHash().toString() +
                                "-" +
                                outpoint.getTxOutputN();
                coinIdToAmount.put(coinId, outpoint.getValue().longValue());
            }
        }
        return coinIdToAmount;
    }

    public void markUTXOAsNonSpendable(final String hexTx,
                                       final int account) {

        final Map<String, Long> amountByCoinId = getAmountsByCoinId(account, true);

        final Transaction tx = new Transaction(
                SamouraiWallet.getInstance().getCurrentNetworkParams(),
                Hex.decode(hexTx));

        final int POST_MIX = WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix();
        final int BAD_BANK = WhirlpoolMeta.getInstance(context).getWhirlpoolBadBank();

        for (final TransactionInput input : tx.getInputs()) {
            final String hash = input.getOutpoint().getHash().toString();
            final int idx = (int) input.getOutpoint().getIndex();
            final String toBlockCoinId = hash.concat("-").concat(String.valueOf(idx));
            final Long value = amountByCoinId.get(toBlockCoinId);
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

    public boolean markUTXOChange(final String hexTx,
                                  final int account,
                                  final boolean lock) {

        final List<TransactionOutPoint> changeTxOutPoints
                = getChangeTxOutPoints(account, hexTx, lock);

        for (final TransactionOutPoint out : changeTxOutPoints) {

            if (account == SamouraiAccountIndex.POSTMIX) {
                if (lock) {
                    BlockedUTXO.getInstance().addPostMix(
                            out.getHash().toString(),
                            (int)out.getIndex(),
                            out.getValue().longValue());
                } else {
                    BlockedUTXO.getInstance().removePostMix(
                            out.getHash().toString(),
                            (int)out.getIndex());
                }
            } else if (account == SamouraiAccountIndex.BADBANK) {
                if (lock) {
                    BlockedUTXO.getInstance().addBadBank(
                            out.getHash().toString(),
                            (int)out.getIndex(),
                            out.getValue().longValue());
                } else {
                    BlockedUTXO.getInstance().removeBadBank(
                            out.getHash().toString(),
                            (int)out.getIndex());
                }
            } else {
                if (lock) {
                    BlockedUTXO.getInstance().add(
                            out.getHash().toString(),
                            (int)out.getIndex(),
                            out.getValue().longValue());
                } else {
                    BlockedUTXO.getInstance().remove(
                            out.getHash().toString(),
                            (int)out.getIndex());
                }
            }
        }
        return ! changeTxOutPoints.isEmpty();
    }

    public List<UTXO> getUTXOS(
            final String address,
            final long neededAmount,
            final int account) {

        final APIFactory apiFactory = APIFactory.getInstance(context);

        List<UTXO> utxos;
        if (account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            if (UTXOFactory.getInstance().getTotalPostMix() > neededAmount) {
                List<UTXO> postmix = apiFactory.getUtxosPostMix(true);
                utxos = new ArrayList<>();
                //Filtering out do not spends
                for (UTXO item : postmix) {
                    UTXO u = new UTXO(item.getPath(), item.getXpub());
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
        } else if (FormatsUtil.getInstance().isValidBech32(address)
                && apiFactory.getUtxosP2WPKH(true).size() > 0 &&
                UTXOFactory.getInstance().getTotalP2WPKH() > neededAmount) {

            utxos = new ArrayList<>(apiFactory.getUtxosP2WPKH(true));

        } else if (!FormatsUtil.getInstance().isValidBech32(address) &&
                Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress() &&
                apiFactory.getUtxosP2SH_P2WPKH(true).size() > 0 &&
                getTotalP2SH_P2WPKH() > neededAmount) {

            utxos = new ArrayList<>(apiFactory.getUtxosP2SH_P2WPKH(true));

        } else if (!FormatsUtil.getInstance().isValidBech32(address) &&
                !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress() &&
                apiFactory.getUtxosP2PKH(true).size() > 0 &&
                getTotalP2PKH() > neededAmount) {

            utxos = new ArrayList<>(apiFactory.getUtxosP2PKH(true));

        } else {
            utxos = apiFactory.getUtxos(true);
        }

        return utxos;
    }

    public List<TransactionOutPoint> getChangeTxOutPoints(final int account,
                                                          final String hexTx,
                                                          final boolean onlyUnlockedUtxo) {

        final List<TransactionOutPoint> changeTxOutPoints = Lists.newArrayList();

        final Map<String, Long> amountByCoinId = getAmountsByCoinId(account, onlyUnlockedUtxo);

        final NetworkParameters netParams = SamouraiWallet.getInstance().getCurrentNetworkParams();
        final Transaction tx = new Transaction(
                netParams,
                Hex.decode(hexTx));
        if (tx == null) return changeTxOutPoints;

        for (final TransactionOutput output : tx.getOutputs()) {
            final TransactionOutPoint outPointFor = output.getOutPointFor();
            if (outPointFor == null) continue;
            final String hash = outPointFor.getHash().toString();
            final int idx = (int) outPointFor.getIndex();
            final String toBlockCoinId = hash.concat("-").concat(String.valueOf(idx));
            final Long value = amountByCoinId.get(toBlockCoinId);
            if (value != null) {
                changeTxOutPoints.add(new MyTransactionOutPoint(
                        netParams,
                        outPointFor.getHash(),
                        idx,
                        BigInteger.valueOf(value),
                        new byte[0],
                        null,
                        0));
            }
        }

        return changeTxOutPoints;
    }
}
