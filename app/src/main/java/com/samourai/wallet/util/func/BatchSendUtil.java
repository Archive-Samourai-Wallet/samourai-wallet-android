package com.samourai.wallet.util.func;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchSendUtil {

    public static final String TAG = "BatchSendUtil";

    public class BatchSend {

        public String pcode = null;
        public String paynymCode = null;
        public int paynymIndexOffset;
        private String addr = null;
        public long amount = 0L;
        public long UUID = 0L;

        private BatchSend() {}

        public BatchSend setRawAddr(final String addr) {
            this.addr = addr;
            return this;
        }

        public boolean isPayNym() {
            return nonNull(paynymCode) || nonNull(pcode);
        }

        public String getAddr(final Context context) throws Exception {
            if (isNull(addr) && hasPcode()) {
                addr = BIP47Util.getInstance(context)
                        .getDestinationAddrFromPcode(pcode, paynymIndexOffset);
            }
            return addr;
        }

        public String getRawAddr() {
            return addr;
        }

        public String captionDestination(final Context context) throws Exception {
            if (isNull(pcode)) return getAddr(context);
            if (nonNull(paynymCode)) return paynymCode;
            final BIP47Meta bip47Meta = BIP47Meta.getInstance();
            return defaultIfBlank(bip47Meta.getDisplayLabel(pcode), pcode);
        }

        public boolean hasPcode() {
            return isNotBlank(pcode);
        }
    }

    synchronized private int getIndexOffset(final String pcode) {
        return pcodeIndexOffsets.containsKey(pcode) ? pcodeIndexOffsets.get(pcode) : -1;
    }

    private static BatchSendUtil instance = null;

    private final List<BatchSend> batchSends;
    private final Map<String, Integer> pcodeIndexOffsets;

    public BatchSend createBatchSend() {
        return new BatchSend();
    }

    private BatchSendUtil() {
        batchSends = new ArrayList<>();
        pcodeIndexOffsets = Maps.newHashMap();
    }

    public static BatchSendUtil getInstance() {
        if(isNull(instance)) {
            instance = new BatchSendUtil();
        }
        return instance;
    }

    synchronized public BatchSendUtil add(final BatchSend send) {
        batchSends.add(send);
        if (send.hasPcode()) {
            final int offset = 1 + getIndexOffset(send.pcode);
            pcodeIndexOffsets.put(send.pcode, offset);
            send.paynymIndexOffset = offset;
        }
        return this;
    }

    synchronized public BatchSendUtil addAll(final Collection<BatchSend> batchSends) {
        for (final BatchSend batchSend : batchSends) {
            add(batchSend);
        }
        return this;
    }

    synchronized public void clear() {
        batchSends.clear();
        pcodeIndexOffsets.clear();
    }

    public List<BatchSend> getCopyOfBatchSends() {
        return ImmutableList.copyOf(batchSends);
    }

    public Map<String, BatchSend> mapAddresses(
            final Set<String> addresses,
            final Context context) {

        final Map<String, BatchSend> batchSendByAddress = getBatchSendByAddress(context);

        final Map<String, BatchSend> map = Maps.newLinkedHashMap();
        for (final String addr : addresses) {
            final BatchSend batchSend = batchSendByAddress.get(addr);
            if (nonNull(batchSend)) {
                map.put(addr, batchSend);
            }
        }
        return map;
    }

    public Map<String, BatchSend> getBatchSendByAddress(final Context context) {

        final Map<String, BatchSend> map = Maps.newHashMap();
        for (final BatchSend batchSend : batchSends) {
            try {
                map.put(batchSend.getAddr(context), batchSend);
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return map;
    }

    public List<String> getAddresses(final Context context) throws Exception {
        final List<String> addresses = Lists.newArrayList();
        for (final BatchSend batchSend : getCopyOfBatchSends()) {
            addresses.add(batchSend.getAddr(context));
        }
        return addresses;
    }

    public JSONArray toJSON() {

        final JSONArray batch = new JSONArray();
        try {
            for(final BatchSend send : batchSends) {
                final JSONObject obj = new JSONObject();
                if (nonNull(send.pcode))    {
                    obj.put("pcode", send.pcode);
                }
                if (nonNull(send.paynymCode))    {
                    obj.put("paynym", send.paynymCode);
                }
                obj.put("addr", send.addr);
                obj.put("amount", send.amount);
                batch.put(obj);
            }
        } catch(final JSONException je) {}

        return batch;
    }

    public void fromJSON(JSONArray batch) {

        clear();

        try {
            for(int i = 0; i < batch.length(); i++) {
                final JSONObject send = batch.getJSONObject(i);
                final BatchSend batchSend = new BatchSend();
                if (send.has("pcode"))    {
                    batchSend.pcode = send.getString("pcode");
                }
                if (send.has("paynym"))    {
                    batchSend.paynymCode = send.getString("paynym");
                }
                if (! batchSend.hasPcode()) {
                    batchSend.addr = send.getString("addr");
                }
                batchSend.amount = send.getLong("amount");
                add(batchSend);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
