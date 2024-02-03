package com.samourai.wallet.send.batch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * json format from String to InputBatchSpend
 *
 * { "batch": [{ "dest": "+squarecredit12C", "amt": 17000 }, { "dest": "PM8TJNHb48FLK9XfuNEG66cZx9P2dyw224Qtptu6QvQ4hiWCqoAreZkqxc1E9dXvL6fhdAHCcr2ebJya6DYZJvxbsf1q4r4dc9X8YoR7oY1CJ6PMiSfN", "amt": 20003 }, { "dest": "myoWAzp1B7t6BjJhcXNDNjT65H9LH1tgVK", "amt": 3267 }, { "dest": "2NGHetYPxzMTJHUVzQTN6Yr1oqq6JyLqA2H", "amt": 23453 }, { "dest": "tb1q5e8gwdn0vwtwng49ntzpdy2zewh84jhpa7s6py", "amt": 20006 }, { "dest": "tb1pme503kta7rdpm5zctx4u7ystrt4c9463hk9r7zvw8cfr9ay7n6rq3xm626", "amt": 13478 }, { "dest": "tb1qr2rn7s7urcptk5d6mfwec5n9q4xtd5075ug3w2qy2q98pdmfna7q0vvx5p", "amt": 8209 } ]}
 *
 */

public class InputBatchSpendHelper {

    public static final String TAG = "InputBatchSpendHelper";

    private InputBatchSpendHelper() {}

    public static InputBatchSpend loadInputBatchSpend(
            final String jsonContent
    ) throws Exception {


        final InputBatchSpend inputBatchSpend = InputBatchSpend.createInputBatchSpend();

        final JSONObject obj = new JSONObject(jsonContent);
        if(obj.has("batch")) {
            final JSONArray array = obj.getJSONArray("batch");

            for(int i = 0; i < array.length(); i++) {

                final JSONObject dest = (JSONObject) array.get(i);
                String strDestination = null;
                long amount = 0L;

                if(dest.has("dest")) {
                    strDestination = dest.getString("dest");
                }

                if(dest.has("amt")) {
                    amount = dest.getLong("amt");
                }

                inputBatchSpend.addSpend(strDestination, amount);
            }
        }

        if (inputBatchSpend.getSpendDescriptionList().isEmpty()) {
            throw new Exception("the content does not represent batch spend");
        }

        return inputBatchSpend;
    }

    public static boolean canParseAsBatchSpend(final String inputBatchSpendAsJson) {
        try {
            loadInputBatchSpend(inputBatchSpendAsJson);
            return true;
        } catch (final Exception e) {
            Log.d(TAG, "content from QR code is not parsable as InputBatchSpend:" + e.getMessage());
        }
        return false;
    }

}



/*

example of json content :

{
  "batch": [
    {
      "dest": "+squarecredit12C",
      "amt": 17000
    },
    {
      "dest": "PM8TJNHb48FLK9XfuNEG66cZx9P2dyw224Qtptu6QvQ4hiWCqoAreZkqxc1E9dXvL6fhdAHCcr2ebJya6DYZJvxbsf1q4r4dc9X8YoR7oY1CJ6PMiSfN",
      "amt": 20003
    },
    {
      "dest": "myoWAzp1B7t6BjJhcXNDNjT65H9LH1tgVK",
      "amt": 3267
    },
    {
      "dest": "2NGHetYPxzMTJHUVzQTN6Yr1oqq6JyLqA2H",
      "amt": 23453
    },
    {
      "dest": "tb1q5e8gwdn0vwtwng49ntzpdy2zewh84jhpa7s6py",
      "amt": 20006
    },
    {
      "dest": "tb1pme503kta7rdpm5zctx4u7ystrt4c9463hk9r7zvw8cfr9ay7n6rq3xm626",
      "amt": 13478
    },
    {
      "dest": "tb1qr2rn7s7urcptk5d6mfwec5n9q4xtd5075ug3w2qy2q98pdmfna7q0vvx5p",
      "amt": 8209
    }
  ]
}

 */