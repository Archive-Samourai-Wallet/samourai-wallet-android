package com.samourai.wallet.api.seen;

import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TESTNET;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TESTNET_TOR;
import static com.samourai.wallet.util.network.WebUtil.SAMOURAI_API2_TOR;
import static org.apache.commons.lang3.StringUtils.strip;
import static java.util.Objects.nonNull;

import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.network.WebUtil;
import com.samourai.wallet.util.tech.AppUtil;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Map;

public class SeenClient {

    private static final String SEEN_PATH = "seen";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int TIMEOUT = 15_000;

    private final Context context;
    private final boolean testMode;
    private final String accessToken;

    private SeenClient(final Context context, final boolean testMode) {
        this.context = context;
        this.testMode = testMode;
        this.accessToken = null;
    }

    private SeenClient(final String accessToken, final boolean testMode) {
        this.context = null;
        this.testMode = testMode;
        this.accessToken = accessToken;
    }

    public static SeenClient createSeenClient(final Context context, final boolean testMode) {
        return new SeenClient(context, testMode);
    }

    public static SeenClient createSeenClient(final String accessToken, final boolean testMode) {
        return new SeenClient(accessToken, testMode);
    }

    public static SeenClient createSeenClient(final String accessToken) {
        return new SeenClient(accessToken, SamouraiWallet.getInstance().isTestNet());
    }

    public static SeenClient createSeenClient(final Context context) {
        return new SeenClient(context, SamouraiWallet.getInstance().isTestNet());
    }

    public static SeenClient createSeenClientForTest(final Context context) {
        return new SeenClient(context, true);
    }

    public RawSeenAddresses getSeenAddresses(final Collection<String> addresses) throws Exception {

        if (! AppUtil.getInstance(context).isOfflineMode()) {

            final String resultAsPlainText = getSeenAsPlainText(addresses);
            final Map<String, Boolean> seenAddressesMap = objectMapper.readValue(
                    resultAsPlainText,
                    new TypeReference<Map<String, Boolean>>() {});
            return RawSeenAddresses.create(seenAddressesMap);

        } else {
            return RawSeenAddresses.createEmpty();
        }
    }

    private String getSeenAsPlainText(final Collection<String> addresses) throws Exception {
        final String accessToken = getApiToken();
        if (SamouraiTorManager.INSTANCE.isRequired()) {
            final String url = testMode ? SAMOURAI_API2_TESTNET_TOR : SAMOURAI_API2_TOR;
            return WebUtil.getInstance(null).getURL(
                    url + SEEN_PATH + "?addresses=" + buildAddressesRequestParam(addresses),
                    ImmutableMap.of("Authorization", "Bearer " + accessToken),
                    TIMEOUT);
        } else {
            final String url = testMode ? SAMOURAI_API2_TESTNET : SAMOURAI_API2;
            return WebUtil.getInstance(null).getURL(
                    url + SEEN_PATH + "?addresses=" + buildAddressesRequestParam(addresses),
                    ImmutableMap.of("Authorization", "Bearer " + accessToken),
                    TIMEOUT);
        }
    }

    private String buildAddressesRequestParam(final Collection<String> addresses) {
        final StringBuilder sb = new StringBuilder();
        for (final String addr : CollectionUtils.emptyIfNull(addresses)) {
            sb.append(addr);
            sb.append("%7C"); // pipe encoded for url
        }
        return strip(sb.toString(), "%7C");
    }

    private String getApiToken() {
        if (nonNull(accessToken)) {
            return accessToken;
        } else if (nonNull(context)) {
            return APIFactory.getInstance(context).getAccessToken();
        } else {
            return null;
        }

    }
}
