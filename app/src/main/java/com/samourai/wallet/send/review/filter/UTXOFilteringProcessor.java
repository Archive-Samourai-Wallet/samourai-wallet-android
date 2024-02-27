package com.samourai.wallet.send.review.filter;

import static com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex.DEPOSIT;
import static com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex.POSTMIX;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static java.util.Objects.nonNull;

import android.content.Context;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.EnumAddressType;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UTXOFilteringProcessor {

    public static List<UTXO> applyUtxoFilter(
            final Collection<UTXO> utxosToFilter,
            final UTXOFilterModel filteringModel,
            final boolean postmixAccount,
            final Context context) {

        if (filteringModel.isAllChecked(postmixAccount)) {
            return Lists.newArrayList(CollectionUtils.emptyIfNull(utxosToFilter));
        }

        final List<UTXO> result = Lists.newArrayList();

        final int account = postmixAccount ? POSTMIX : DEPOSIT;

        final Map<EnumAddressType, Map<String, Integer>> cacheOfChangeAddress = Maps.newHashMap();

        for (final UTXO utxo : CollectionUtils.emptyIfNull(utxosToFilter)) {

            final UTXO utxoToAdd = new UTXO();
            utxoToAdd.setPath(utxo.getPath());
            final List<MyTransactionOutPoint> outPoints = Lists.newArrayList();

            for (final MyTransactionOutPoint outPoint : CollectionUtils.emptyIfNull(utxo.getOutpoints())) {
                final EnumAddressType addressType = EnumAddressType.fromAddress(outPoint.getAddress());

                if (! filteringModel.isSegwitNative() && addressType.isSegwitNative()) {
                    continue;
                }
                if (! filteringModel.isSegwitCompatible() && addressType.isSegwitCompatible()) {
                    continue;
                }
                if (! filteringModel.isLegacy() && addressType.isLegacy()) {
                    continue;
                }

                if (postmixAccount) {

                    if (! filteringModel.isPostmixTransactionChange() &&
                            startsWithIgnoreCase(utxo.getPath(), "M/1/")) {
                        continue;
                    }
                    if (! filteringModel.isMixedOutputs() &&
                            startsWithIgnoreCase(utxo.getPath(), "M/0/")) {
                        continue;
                    }
                } else {

                    if (! filteringModel.isPayNymOutputs() && isPayNymOutputs(outPoint)) {
                        continue;
                    }

                    if (! filteringModel.isUnmixedToxicChange() &&
                            startsWithIgnoreCase(utxo.getPath(), "M/1/")) {
                            continue;
                    }
                }

                outPoints.add(outPoint);
            } // end of outpoint

            if (! outPoints.isEmpty()) {
                utxoToAdd.setOutpoints(outPoints);
                result.add(utxoToAdd);
            }
        } // end of utxo

        return result;
    }

    public static boolean isPayNymOutputs(final MyTransactionOutPoint outPoint) {
        return nonNull(BIP47Meta.getInstance().getIdx4Addr(outPoint.getAddress()));
    }

    public static boolean isMixedOutputs(final MyTransactionOutPoint outPoint) {

        final WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance()
                .whirlpoolWallet();

        if (nonNull(whirlpoolWallet)) {

            final WhirlpoolUtxo whirlpoolUtxo = whirlpoolWallet.getUtxoSupplier()
                    .findUtxo(outPoint.getTxHash().toString(), outPoint.getTxOutputN());

            if (nonNull(whirlpoolUtxo) && nonNull(whirlpoolUtxo.getUtxo())) {
                if (WhirlpoolAccount.POSTMIX == whirlpoolUtxo.getAccount()) {
                    return true;
                }
            }
        }

        return false;
    }
}
