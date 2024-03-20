package com.samourai.wallet.ricochet;

import static java.util.Objects.nonNull;

import android.util.Log;

import com.google.common.collect.Lists;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.func.TransactionOutPointHelper;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class RicochetTransactionInfo {

    public static final String TAG = "RicochetTransactionInfo";

    private final JSONObject ricochetScriptAsJson;
    private final List<MyTransactionOutPoint> outPoints;

    private RicochetTransactionInfo(
            final JSONObject ricochetScriptAsJson,
            final List<MyTransactionOutPoint> outPoints) {

        this.ricochetScriptAsJson = ricochetScriptAsJson;
        this.outPoints = outPoints;
    }

    public static RicochetTransactionInfo create(
            final JSONObject ricochetScriptAsJson,
            final List<MyTransactionOutPoint> outPoints) {

        return new RicochetTransactionInfo(ricochetScriptAsJson, outPoints);
    }

    public static RicochetTransactionInfo createEmpty() {
        return new RicochetTransactionInfo(null, null);
    }

    public JSONObject getRicochetScriptAsJson() {
        return ricochetScriptAsJson;
    }

    public long getRicochetFee() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getLong("samourai_fee");
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return Long.MIN_VALUE;
    }

    public long getHop0Fee() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getJSONArray("hops")
                        .getJSONObject(0).getLong("fee");
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return Long.MIN_VALUE;
    }

    public long getPerHopFee() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getJSONArray("hops")
                        .getJSONObject(0).getLong("fee_per_hop");
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return Long.MIN_VALUE;
    }

    public List<MyTransactionOutPoint> getSelectedUTXOPoints() {
        return Lists.newArrayList(CollectionUtils.emptyIfNull(outPoints));
    }

    public long getTotalSpend() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getLong("total_spend");
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return 0L;
    }

    public long getAggregatedInputAmount() {
        return TransactionOutPointHelper.retrievesAggregatedAmount(outPoints);
    }

    public long getChange() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getLong("change");
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return 0L;
    }

    public int nbHops() {
        if (nonNull(ricochetScriptAsJson)) {
            try {
                return ricochetScriptAsJson.getJSONArray("hops").length() - 1;
            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }
        return 0;
    }
}
