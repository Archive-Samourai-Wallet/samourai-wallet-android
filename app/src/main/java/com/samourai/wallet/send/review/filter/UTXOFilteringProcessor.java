package com.samourai.wallet.send.review.filter;

import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static java.util.Objects.nonNull;

import com.google.common.collect.Lists;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.EnumAddressType;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;

public class UTXOFilteringProcessor {

    public static List<UTXO> applyUtxoFilter(
            final Collection<UTXO> utxosToFilter,
            final UTXOFilterModel filteringModel,
            final boolean postmixAccount) {

        if (filteringModel.isAllChecked(postmixAccount)) {
            return Lists.newArrayList(CollectionUtils.emptyIfNull(utxosToFilter));
        }

        final List<UTXO> result = Lists.newArrayList();

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

                if (!filteringModel.isUnconfirmed() && outPoint.getConfirmations() < 1) {
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

}
