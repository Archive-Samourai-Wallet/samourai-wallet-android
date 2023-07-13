package com.samourai.wallet.send.cahoots;

import android.content.Context;

import com.google.common.collect.Lists;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.UTXO;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JoinbotHelper {

    private JoinbotHelper() {}

    private enum AddressType {
        P2WPKH,
        P2SH_P2WPKH,
        P2PKH,
    }

    private final static Comparator<UTXO> UTXO_COMPARATOR_BY_VALUE = (u1, u2) -> Long.compare(u1.getValue(), u2.getValue());

    private static class IndexedUTXO implements Comparable<IndexedUTXO> {
        private final UTXO utxo;
        private final AddressType addressType;
        private final int index;

        private IndexedUTXO(final UTXO utxo, final AddressType addressType, final int index) {
            this.utxo = utxo;
            this.addressType = addressType;
            this.index = index;
        }

        public static IndexedUTXO create(final UTXO utxo, final AddressType addressType, final int index) {
            return new IndexedUTXO(utxo, addressType, index);
        }

        @Override
        public int compareTo(final IndexedUTXO o2) {
            final int compare = UTXO_COMPARATOR_BY_VALUE.compare(utxo, o2.utxo);
            if (compare != 0) return compare;
            return addressType.compareTo(o2.addressType);
        }

        public UTXO getUtxo() {
            return utxo;
        }

        public AddressType getAddressType() {
            return addressType;
        }

        public int getIndex() {
            return index;
        }
    }


    public static boolean isJoinbotPossibleWithCurrentUserUTXOs(
            final Context context,
            final boolean postmixAccount,
            final long amount) {

        if (amount <= 0) return true;

        final APIFactory instance = APIFactory.getInstance(context);

        final List<UTXO> utxosP2WPKH = postmixAccount
                ? Lists.newArrayList(instance.getUtxosPostMix(true))
                : Lists.newArrayList(instance.getUtxosP2WPKH(true));

        final List<UTXO> utxosP2SH_P2WPKH = postmixAccount
                ? Lists.newArrayList()
                : Lists.newArrayList(instance.getUtxosP2SH_P2WPKH(true));

        final List<UTXO> utxosP2PKH = postmixAccount
                ? Lists.newArrayList()
                : Lists.newArrayList(instance.getUtxosP2PKH(true));

        return isJoinbotPossibleWithCurrentUserUTXOs(
                amount,
                utxosP2WPKH,
                utxosP2SH_P2WPKH,
                utxosP2PKH);
    }

    static boolean isJoinbotPossibleWithCurrentUserUTXOs(
            final long amount,
            final List<UTXO> utxosP2WPKH,
            final List<UTXO> utxosP2SH_P2WPKH,
            final List<UTXO> utxosP2PKH) {

        Collections.sort(utxosP2WPKH, UTXO_COMPARATOR_BY_VALUE);
        Collections.sort(utxosP2SH_P2WPKH, UTXO_COMPARATOR_BY_VALUE);
        Collections.sort(utxosP2PKH, UTXO_COMPARATOR_BY_VALUE);

        final Map<AddressType, List<UTXO>> utxoByAddressType = new LinkedHashMap<AddressType, List<UTXO>>() {
            {
                 put(AddressType.P2WPKH, utxosP2WPKH);
                 put(AddressType.P2SH_P2WPKH, utxosP2SH_P2WPKH);
                 put(AddressType.P2PKH, utxosP2PKH);
            }
        };

        final long joinbotFees = Math.round(amount * 35d / 1000d); // joinbot fees are 3.5% of amount to spend
        if (joinbotFees > 0) {
            if (! enoughBalanceToPayFees(utxoByAddressType, joinbotFees)) {
                return false;
            }
            if (! removedUtxoForJoinbotFees(utxoByAddressType, joinbotFees)) {
                return false;
            }
        }

        /**
         * after removing UTXOs for Joinbot fees,
         * it needs to check if the amount to spend is possible
         * => check if any group validate the amount0
         */
        for (final List<UTXO> utxoList : utxoByAddressType.values()) {
            if (UTXO.sumValue(utxoList) >= amount) {
                return true;
            }
        }

        return false;
    }

    private static boolean enoughBalanceToPayFees(
            final Map<AddressType, List<UTXO>> utxoByAddressType,
            final long joinbotFees) {

        long balance = 0l;
        for (final List<UTXO> utxoList : utxoByAddressType.values()) {
            balance += UTXO.sumValue(utxoList);
            if (balance >= joinbotFees) return true;
        }

        return balance >= joinbotFees;
    }

    private static boolean removedUtxoForJoinbotFees(
            final Map<AddressType, List<UTXO>> utxoByAddressType,
            final long joinbotFees) {

        /**
         * find the smallest UTXOs index set which could paid the fees and from this index create
         * the candidate sublist (the next utxo are to big to be intéresting)
         */
        final List<IndexedUTXO> utxoList = toIndexedUTXOList(utxoByAddressType);
        final int validatingUtxoIndex = getValidatingUtxoIndex(utxoList, joinbotFees);
        final boolean foundSoloUtxo = validatingUtxoIndex < utxoList.size();

        if (foundSoloUtxo && validatingUtxoIndex <= 1) {
            utxoByAddressType.get(utxoList.get(validatingUtxoIndex).addressType)
                    .remove(utxoList.get(validatingUtxoIndex).index);
            return true;
        }

        final List<IndexedUTXO> smallCandidateList = foundSoloUtxo
                ? utxoList.subList(0, validatingUtxoIndex + 1)
                : utxoList;


        /**
         * to find the best solution we need to apply factorial combination algo.
         * In order to avoid to have big computation we apply naive algo :
         *
         * 1) add the amounts from the smallest to the largest utxo until we exceed the fees
         * 2) from the list of utxos selected in step 1, remove the utxos from index n-1
         * (penultimate in the list) to 0 if the amount is still greater than the fees
         * 1) and 2) are done in method extractUTXOsForJoinbotFees
         * 3)
         * - If we have a list of only 1 utxo => finished
         * - If we have a list of several utxo and there is 1 utxo which
         * alone validates the payment of fees then take the one with the lowest value in satoshi
         */

        final List<IndexedUTXO> selectedUtxos = extractUTXOsForJoinbotFees(
                smallCandidateList,
                joinbotFees);

        if (selectedUtxos == null || selectedUtxos.isEmpty()) return false;
        if (!foundSoloUtxo ||
                selectedUtxos.size() == 1 ||
                getUTXOValue(selectedUtxos) < utxoList.get(validatingUtxoIndex).utxo.getValue()) {

            for (final IndexedUTXO utxoIndexedForFees : selectedUtxos) {
                utxoByAddressType.get(utxoIndexedForFees.addressType)
                        .remove(utxoIndexedForFees.index);
            }
            return true;
        } else if (foundSoloUtxo) {
            utxoByAddressType.get(utxoList.get(validatingUtxoIndex).addressType)
                    .remove(utxoList.get(validatingUtxoIndex).index);
            return true;
        }

        return false;
    }

    private static long getUTXOValue(final List<IndexedUTXO> candidates) {
        long sum = 0;
        if (candidates != null) {
            for (final IndexedUTXO indexedUTXO : candidates) {
                sum += indexedUTXO.utxo.getValue();
            }
        }
        return sum;
    }

    private static List<IndexedUTXO> extractUTXOsForJoinbotFees(
            final List<IndexedUTXO> utxoList,
            final long joinbotFees) {

        if (utxoList == null) return null;
        if (utxoList.isEmpty()) return null;

        long sum = 0l;
        int i = 0;
        while (i < utxoList.size() && sum < joinbotFees) {
            sum += utxoList.get(i).utxo.getValue();
            ++ i;
        }
        if (sum < joinbotFees) return null;

        final List<IndexedUTXO> firstPassCandidates = utxoList.subList(0, i);
        if (firstPassCandidates.size() == 1) {
            return firstPassCandidates;
        }

        long candidateValue = sum;
        final List<IndexedUTXO> secondPassCandidates = Lists.newArrayList(firstPassCandidates.get(i-1));
        for (int j = i-2; j >= 0; --j) {
            final IndexedUTXO toCheckUtxoIndex = firstPassCandidates.get(j);
            final long value = toCheckUtxoIndex.utxo.getValue();
            final long checkValue = candidateValue - value;
            if (checkValue < joinbotFees) { // need to keep this utxo
                secondPassCandidates.add(toCheckUtxoIndex);
            } else { // useless utxo : update the new candidate value
                candidateValue = checkValue;
            }
        }

        /**
         * order of items in secondPassCandidates is reversed by report array index
         * => it is pretty good for the next step which remove utxo from array
         */
        return secondPassCandidates;
    }

    private static List<IndexedUTXO> toIndexedUTXOList(
            final Map<AddressType, List<UTXO>> utxoByAddressType) {

        final List<IndexedUTXO> indexedUTXOList = Lists.newArrayList();
        for (final Map.Entry<AddressType, List<UTXO>> utxoEntry : utxoByAddressType.entrySet()) {
            final List<UTXO> utxoList = utxoEntry.getValue();
            for (int i = 0; i < utxoList.size(); ++ i) {
                indexedUTXOList.add(IndexedUTXO.create(utxoList.get(i), utxoEntry.getKey(), i));
            }
        }
        Collections.sort(indexedUTXOList);
        return indexedUTXOList;
    }

    private static int getValidatingUtxoIndex(final List<IndexedUTXO> utxoList,
                                              final long joinbotFees) {
        int i = 0;
        while (i < utxoList.size() && utxoList.get(i).utxo.getValue() < joinbotFees) {
            ++ i;
        }
        return i;
    }


}
