package com.samourai.wallet.send.batch;

import static com.samourai.wallet.bip47.BIP47Meta.STATUS_SENT_CFM;
import static com.samourai.wallet.send.batch.InputBatchSpend.SpendDescription.createSpendDescription;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.strip;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.util.func.FormatsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InputBatchSpend {

    public static final String TAG = "InputBatchSpend";

    private InputBatchSpend() {}

    private final Map<String, SpendDescription> spendDescriptionMap = Maps.newLinkedHashMap();

    public static InputBatchSpend createInputBatchSpend() {
        return new InputBatchSpend();
    }

    public InputBatchSpend addSpend(final String destAddress, final long amount) {
        final SpendDescription spendDescription = createSpendDescription(destAddress, amount);
        final String key = spendDescription.getKey();
        if(spendDescriptionMap.containsKey(key)) {
            spendDescriptionMap.get(key).plus(amount);
        } else {
            spendDescriptionMap.put(key, spendDescription);
        }
        return this;
    }

    @NotNull
    public List<SpendDescription> getSpendDescriptionList() {
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
        private final String address;
        private String pcode;
        private String paynym;
        private long amount;
        private final boolean validAddress;
        private final boolean validAmount;
        private final boolean needToBeConnected;
        private static final AtomicInteger INDEXER = new AtomicInteger();
        private final int index = INDEXER.incrementAndGet();

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
                    this.address = null;
                } else {
                    this.address = inputAddress;
                    pcode = null;
                    paynym = null;
                    needToBeConnected = false;
                }
            } else {
                this.pcode = null;
                this.address = null;
                this.paynym = null;
                this.needToBeConnected = false;
            }

        }

        private String getKey() {
            if (nonNull(address)) return address;
            return defaultIfBlank(paynym, pcode) + "_" +index;
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

        public String getAddress() {
            return address;
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

        public String getPaynym() {
            return paynym;
        }

        private String retrievePaymentCode(final Context context, final String pCodeOrPayNym) {

            try {
                final String jsonString = callPayNymApi(context, pCodeOrPayNym);
                if (nonNull(jsonString)) {
                    pcode = retrieveCodeFromJson(new JSONObject(jsonString));
                    return pcode;
                }
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        private static String callPayNymApi(final Context context,
                                            final String pCodeOrPayNym) throws Exception {
            return WebUtil.getInstance(context).postURL(
                    "application/json",
                    null,
                    WebUtil.PAYNYM_API + "api/v1/nym?compact=true",
                    String.format("{\"nym\":\"%s\"}", pCodeOrPayNym));
        }

        private String retrievePayNym(final Context context, final String pcode) {
            final String label = BIP47Meta.getInstance().getLabel(pcode);
            if (isNotBlank(label)) return label;
            try {
                final String jsonString = callPayNymApi(context, pcode);
                if (nonNull(jsonString)) {
                    return retrievePayNymFromJson(new JSONObject(jsonString));
                }
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

        private static String retrievePayNymFromJson(final JSONObject json) throws JSONException {
            if (json.has("nymName")) {
                return json.getString("nymName");
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

}
