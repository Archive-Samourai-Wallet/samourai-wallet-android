package com.samourai.wallet.send.batch;

import android.content.Context;

import com.google.common.collect.Maps;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.FormatsUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Map;

/**
 * example of json content :
 *
 * { "batch": [{ "dest": "+holysnow471", "amt": 17000 }, { "dest": "PM8TJQpMzANwsqTFDwsf2ZK9fmqabrmH9Vk6ocTvTJoaAsqTGYRGMu6DzMDKArdyynHNGuJt2sxXk4xGwZZyYXqjLgFTJ8Y7kJu74a32yLVmBje7sbzP", "amt": 200000 }, { "dest": "mpQcGEbsZXoCWv464TyaC6KdKa8tvB3y6W", "amt": 3267 }, { "dest": "2My2UzTAR2ehZjDger7rypkKtymRxC1VKo1", "amt": 234532 }, { "dest": "tb1qjqdj7txr6d9t54kh496kysc3v7kyawxs5fez46", "amt": 200000 }, { "dest": "tb1pme503kta7rdpm5zctx4u7ystrt4c9463hk9r7zvw8cfr9ay7n6rq3xm626", "amt": 134782 }, { "dest": "tb1qr2rn7s7urcptk5d6mfwec5n9q4xtd5075ug3w2qy2q98pdmfna7q0vvx5p", "amt": 8209 } ]}
 *
 */

public class InputBatchSpendHelper {

    private InputBatchSpendHelper() {}

    public static Map<String, BigInteger> loadReceivers(
            final String jsonContent,
            final Context context
    ) throws Exception {


        final Map<String, BigInteger> receivers = Maps.newLinkedHashMap();

        final JSONObject obj = new JSONObject(jsonContent);
        if(! obj.has("batch")) return receivers;

        final JSONArray array = obj.getJSONArray("batch");

        for(int i = 0; i < array.length(); i++) {

            final JSONObject dest = (JSONObject) array.get(i);
            String strDestination = null;
            long amount = 0l;

            if(dest.has("dest")) {
                strDestination = dest.getString("dest");
            }

            if(dest.has("amt")) {
                amount = dest.getLong("amt");
            }

            if (amount <= 0) continue;


            if(FormatsUtil.getInstance().isValidPaymentCode(strDestination) &&
                    BIP47Meta.getInstance().getOutgoingStatus(strDestination) == BIP47Meta.STATUS_SENT_CFM) {

                final int index = BIP47Meta.getInstance().getOutgoingIdx(strDestination);
                final PaymentAddress strAddress = BIP47Util.getInstance(context)
                        .getSendAddress(new PaymentCode(strDestination), index);
                BIP47Meta.getInstance().incOutgoingIdx(strDestination);
                strDestination = strAddress.getSegwitAddressSend().getBech32AsString();
            } else if(! FormatsUtil.getInstance().isValidBitcoinAddress(strDestination)) {
                continue;
            }

            if(receivers.containsKey(strDestination)) {
                receivers.get(strDestination).add(BigInteger.valueOf(amount));
            } else {
                receivers.put(strDestination, BigInteger.valueOf(amount));
            }
        }
        return receivers;
    }
}
