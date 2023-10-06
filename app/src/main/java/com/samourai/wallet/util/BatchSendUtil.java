package com.samourai.wallet.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

import android.content.Context;

import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BatchSendUtil {

    public static class BatchSend   {

        public String pcode = null;
        public String paynymCode = null;
        public String addr = null;
        public long amount = 0L;
        public long UUID = 0L;

        public void computeAddressIfNeeded(final Context context) throws Exception {
            if (isNotBlank(pcode)) {
                addr = BIP47Util.getInstance(context).getDestinationAddrFromPcode(pcode);
            }
        }
    }

    private static BatchSendUtil instance = null;

    private static List<BatchSend> batchSends = null;

    private BatchSendUtil() { ; }

    public static BatchSendUtil getInstance() {

        if(instance == null) {
            batchSends = new ArrayList<BatchSend>();
            instance = new BatchSendUtil();
        }

        return instance;
    }

    public void add(BatchSend send) {
        batchSends.add(send);
    }

    public void clear() {
        batchSends.clear();
    }

    public List<BatchSend> getSends() {
        return batchSends;
    }

    public BatchSend getBatchSend() {
        return new BatchSend();
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
        }
        catch(JSONException je) {
            ;
        }

        return batch;
    }

    public void fromJSON(JSONArray batch) {

        batchSends.clear();

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
                batchSend.addr = send.getString("addr");
                batchSend.amount = send.getLong("amount");
                batchSends.add(batchSend);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
