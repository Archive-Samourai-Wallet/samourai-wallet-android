package com.samourai.wallet.send.batch;

import static com.samourai.wallet.bip47.BIP47Meta.STATUS_SENT_CFM;
import static com.samourai.wallet.send.batch.InputBatchSpend.SpendDescription.createSpendDescription;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.strip;
import static java8.util.Objects.nonNull;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.FormatsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class InputBatchSpend {

    public static final String TAG = "InputBatchSpend";

    private InputBatchSpend() {}

    private final Map<String, SpendDescription> spendDescriptionMap = Maps.newLinkedHashMap();

    public static InputBatchSpend createInputBatchSpend() {
        return new InputBatchSpend();
    }

    public InputBatchSpend addSpend(final String destAddress, final long amount) {
        if(spendDescriptionMap.containsKey(destAddress)) {
            spendDescriptionMap.get(destAddress).plus(amount);
        } else {
            spendDescriptionMap.put(destAddress, createSpendDescription(destAddress, amount));
        }
        return this;
    }

    @NotNull
    public List<SpendDescription> getSpendDescriptionMap() {
        return Lists.reverse(ImmutableList.copyOf(spendDescriptionMap.values()));
    }

    public List<SpendDescription> getItemsNeedToBeConnected() {
        final List<SpendDescription> spendDescriptions = Lists.newArrayList();
        for (final Map.Entry<String, SpendDescription> entry : spendDescriptionMap.entrySet()) {
            if (entry.getValue().needToBeConnected) {
                spendDescriptions.add(entry.getValue());
            }
        }
        return spendDescriptions;
    }

    static public class SpendDescription {
        private String address;
        private final String inputAddress;
        private String pcode;
        private final String paynym;
        private long amount;
        private final boolean validAddress;
        private final boolean validAmount;
        private final boolean needToBeConnected;

        private SpendDescription(final String inputAddress, final long amount) {

            this.amount = amount;
            this.validAmount = amount >= SamouraiWallet.bDust.longValue();
            final Pair<String, Boolean> addressInfo = toValidAddress(inputAddress);
            this.validAddress = nonNull(addressInfo);
            if (this.validAddress) {
                if (addressInfo.getRight()) {
                    if (startsWith(addressInfo.getLeft(), "+")) {
                        paynym = addressInfo.getLeft();
                        needToBeConnected = true;
                        pcode = null;
                    } else {
                        pcode = addressInfo.getLeft();
                        needToBeConnected = BIP47Meta.getInstance().getOutgoingStatus(pcode) != STATUS_SENT_CFM;
                        paynym = null;
                    }
                    this.inputAddress = null;
                } else {
                    this.inputAddress = inputAddress;
                    pcode = null;
                    paynym = null;
                    needToBeConnected = false;
                }
            } else {
                this.pcode = null;
                this.inputAddress = null;
                this.paynym = null;
                this.needToBeConnected = false;
            }

        }

        public static SpendDescription createSpendDescription(
                final String address,
                final long amount) {

            return new SpendDescription(strip(address), amount);
        }

        public SpendDescription plus(final long amountToAdd) {
            amount += amountToAdd;
            return this;
        }

        public long getAmount() {
            return amount;
        }

        public String getPcode() {
            return pcode;
        }

        public boolean isValidAmount() {
            return validAmount;
        }

        public boolean isNeedToBeConnected() {
            return needToBeConnected;
        }

        public String getAddress() {
            return address;
        }

        public String getPaynym() {
            return paynym;
        }

        public void computeAddress(final Context context) {
            address = computeAddressSafe(context);
        }

        private String computeAddressSafe(final Context context) {
            try {
                if (nonNull(inputAddress)) return inputAddress;
                if (nonNull(pcode)) return getDestinationAddrFromPcode(context, pcode);
                if (nonNull(paynym)) return getDestinationAddrFromPcode(
                        context,
                        retrievePaymentCode(context, paynym));
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        private static String retrieveCodeFromJson(final JSONObject json) throws JSONException {

            if (json.has("codes")) {
                final JSONArray codes = json.getJSONArray("codes");

                for (int i = 0; i < codes.length(); ++ i) {
                    final JSONObject jsonObject = codes.getJSONObject(i);

                    if (jsonObject.has("segwit")
                            && jsonObject.has("code")
                            && !jsonObject.getBoolean("segwit")) {

                        return jsonObject.getString("code");
                    }
                }
            }
            return null;
        }

        private String retrievePaymentCode(final Context context, final String paynymCode) {

            try {
                final String jsonString = WebUtil.getInstance(context).postURL(
                        "application/json",
                        null,
                        WebUtil.PAYNYM_API + "api/v1/nym?compact=true",
                        String.format("{\"nym\":\"%s\"}", paynymCode));

                if (nonNull(jsonString)) {
                    pcode = retrieveCodeFromJson(new JSONObject(jsonString));
                    return pcode;
                }
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }
    }

    private static Pair<String, Boolean> toValidAddress(final String strDestination) {

        final BIP47Meta bip47Meta = BIP47Meta.getInstance();

        if (startsWith(strDestination, "+")) {
            final String pCode = bip47Meta.getPcodeFromLabel(strDestination);
            if (isNotBlank(pCode)) {
                if (FormatsUtil.getInstance().isValidPaymentCode(pCode)) {
                    return Pair.of(pCode, true);
                }
                return null;
            }
        }

        if(FormatsUtil.getInstance().isValidPaymentCode(strDestination)) {
            return Pair.of(strDestination, true);
        } else if(FormatsUtil.getInstance().isValidBitcoinAddress(strDestination)) {
            return Pair.of(strDestination, false);
        }

        if (startsWith(strDestination, "+")) {
            return Pair.of(strDestination, true);
        }

        return null;
    }

    public static String getDestinationAddrFromPcode(
            final Context context,
            final String pcodeAsString) throws Exception {

        if (isBlank(pcodeAsString)) return null;

        final PaymentCode pcode = new PaymentCode(pcodeAsString);
        final PaymentAddress paymentAddress = BIP47Util.getInstance(context)
                .getSendAddress(pcode, BIP47Meta.getInstance().getOutgoingIdx(pcodeAsString));

        if (BIP47Meta.getInstance().getSegwit(pcodeAsString)) {
            return new SegwitAddress(
                    paymentAddress.getSendECKey(),
                    SamouraiWallet.getInstance().getCurrentNetworkParams()).getBech32AsString();
        } else {
            return paymentAddress.getSendECKey()
                    .toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
        }
    }

}
