package com.samourai.wallet.send.batch;

import static com.samourai.wallet.bip47.BIP47Meta.STATUS_SENT_CFM;
import static com.samourai.wallet.send.batch.InputBatchSpend.SpendDescription.createSpendDescription;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java8.util.Objects.nonNull;

import android.content.Context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.FormatsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import java8.util.Objects;

public class InputBatchSpend {

    private InputBatchSpend() {}

    private final Map<String, SpendDescription> spendDescriptionList = Maps.newLinkedHashMap();

    public static InputBatchSpend createInputBatchSpend() {
        return new InputBatchSpend();
    }

    public InputBatchSpend addSpend(final String destAddress, final long amount) {
        if(spendDescriptionList.containsKey(destAddress)) {
            spendDescriptionList.get(destAddress).plus(amount);
        } else {
            spendDescriptionList.put(destAddress, createSpendDescription(destAddress, amount));
        }
        return this;
    }

    @NotNull
    public Map<String, SpendDescription> getSpendDescriptionList() {
        return ImmutableMap.copyOf(spendDescriptionList);
    }

    public List<SpendDescription> getItemsNeedToBeConnected() {
        final List<SpendDescription> spendDescriptions = Lists.newArrayList();
        for (final Map.Entry<String, SpendDescription> entry : spendDescriptionList.entrySet()) {
            if (entry.getValue().needToBeConnected) {
                spendDescriptions.add(entry.getValue());
            }
        }
        return spendDescriptions;
    }

    static public class SpendDescription {
        private final String address;
        private final String pcode;
        private long amount;
        private final boolean validAddress;
        private final boolean validAmount;
        private final boolean needToBeConnected;

        private SpendDescription(final String address, final long amount) {

            this.amount = amount;
            this.validAmount = amount >= SamouraiWallet.bDust.longValue();
            final Pair<String, Boolean> addressInfo = toValidAddress(address);
            this.validAddress = nonNull(addressInfo);
            if (this.validAddress) {
                if (addressInfo.getRight()) {
                    pcode = addressInfo.getLeft();
                    this.address = null;
                    final BIP47Meta bip47Meta = BIP47Meta.getInstance();
                    needToBeConnected = bip47Meta.getOutgoingStatus(pcode) != STATUS_SENT_CFM;
                } else {
                    this.address = address;
                    pcode = null;
                    needToBeConnected = false;
                }
            } else {
                this.pcode = null;
                this.address = null;
                needToBeConnected = false;
            }

        }

        public static SpendDescription createSpendDescription(
                final String address,
                final long amount) {

            return new SpendDescription(address, amount);
        }

        public SpendDescription plus(final long amountToAdd) {
            amount += amountToAdd;
            return this;
        }

        public String getAddress() {
            return address;
        }

        public long getAmount() {
            return amount;
        }

        public String getPcode() {
            return pcode;
        }

        public boolean isValidAddress() {
            return validAddress;
        }

        public boolean isValidAmount() {
            return validAmount;
        }

        public boolean isNeedToBeConnected() {
            return needToBeConnected;
        }

        public String computeAddress(final Context context) throws Exception {
            if (Objects.isNull(pcode)) return address;
            return getDestinationAddrFromPcode(context, pcode);
        }
    }

    private static Pair<String, Boolean> toValidAddress(
            final String strDestination) {

        final BIP47Meta bip47Meta = BIP47Meta.getInstance();
        final String pCode = bip47Meta.getPcodeFromLabel(strDestination);

        if (isNotBlank(pCode)) {
            if (FormatsUtil.getInstance().isValidPaymentCode(pCode)) {
                return Pair.of(pCode, true);
            }
            return null;
        }

        if(FormatsUtil.getInstance().isValidPaymentCode(strDestination)) {
            return Pair.of(strDestination, true);
        } else if(FormatsUtil.getInstance().isValidBitcoinAddress(strDestination)) {
            return Pair.of(strDestination, false);
        }
        return null;
    }

    public static String getDestinationAddrFromPcode(
            final Context context,
            final String pcode) throws Exception {

        final PaymentCode _pcode = new PaymentCode(pcode);
        final PaymentAddress paymentAddress = BIP47Util.getInstance(context)
                .getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(pcode));

        if (BIP47Meta.getInstance().getSegwit(pcode)) {
            return new SegwitAddress(
                    paymentAddress.getSendECKey(),
                    SamouraiWallet.getInstance().getCurrentNetworkParams()).getBech32AsString();
        } else {
            return paymentAddress.getSendECKey()
                    .toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
        }
    }

}
