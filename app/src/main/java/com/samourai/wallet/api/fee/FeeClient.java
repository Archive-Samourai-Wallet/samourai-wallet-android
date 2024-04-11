package com.samourai.wallet.api.fee;

import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TESTNET;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TESTNET_TOR;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TOR;
import static java.util.Objects.nonNull;

import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.network.WebUtil;

import java.util.List;
import java.util.Map;

public class FeeClient {

    private static final String FEES_ESTIMATOR_PATH = "fees/estimator";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Context context;
    private final boolean testMode;

    private FeeClient(final Context context, final boolean testMode) {
        this.context = context;
        this.testMode = testMode;
    }

    public static FeeClient createAnonymFeeClient() {
        return new FeeClient(null, false);
    }

    public static FeeClient createFeeClient(final Context context, final boolean testMode) {
        return new FeeClient(context, testMode);
    }

    public static FeeClient createFeeClient(final Context context) {
        return new FeeClient(context, false);
    }

    public static FeeClient createFeeClientForTest(final Context context) {
        return new FeeClient(context, true);
    }

    public RawFees getFees() throws Exception {
        final String feeAsPlainText = getFeeAsPlainText();
        final DojoUtil dojoUtil = DojoUtil.getInstance(context);
        if (dojoUtil.isDollarFeeV2()) {
            final Map<String, Integer> feeMap = objectMapper.readValue(
                    feeAsPlainText,
                    new TypeReference<Map<String, Integer>>() {});
            return RawFees.createFromMap(feeMap);
        } else if (dojoUtil.isDollarFeeV1Only()) {
            final List<Integer> feeList = objectMapper.readValue(
                    feeAsPlainText,
                    new TypeReference<List<Integer>>() {});
            return RawFees.createFromList(feeList);
        } else {
            try {
                final Map<String, Integer> feeMap = objectMapper.readValue(
                        feeAsPlainText,
                        new TypeReference<Map<String, Integer>>() {});
                return RawFees.createFromMap(feeMap);
            } catch (final Exception e) {
                final List<Integer> feeList = objectMapper.readValue(
                        feeAsPlainText,
                        new TypeReference<List<Integer>>() {});
                return RawFees.createFromList(feeList);
            }
        }
    }

    private String getFeeAsPlainText() throws Exception {
        if (nonNull(context)) {
            final String accessToken = APIFactory.getInstance(context).getAccessToken();
            if (SamouraiTorManager.INSTANCE.isRequired()) {
                final String url = testMode ? SAMOURAI_API2_TESTNET_TOR : SAMOURAI_API2_TOR;
                return WebUtil.getInstance(null).getURL(
                        url + FEES_ESTIMATOR_PATH,
                        ImmutableMap.of("Authorization", "Bearer " + accessToken));
            } else {
                final String url = testMode ? SAMOURAI_API2_TESTNET : SAMOURAI_API2;
                return WebUtil.getInstance(null).getURL(
                        url + FEES_ESTIMATOR_PATH,
                        ImmutableMap.of("Authorization", "Bearer " + accessToken));
            }
        } else {
            final String url = testMode ? SAMOURAI_API2_TESTNET : SAMOURAI_API2;
            return WebUtil.getInstance(null).getURL(
                    url + FEES_ESTIMATOR_PATH,
                    null);
        }
    }

}
