package com.samourai.wallet.send;

import android.content.Context;
import android.util.Pair;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.util.LogUtil.debug;

public class PushTx {

    private static boolean DO_SPEND = true;

    private static PushTx instance = null;
    private static Context context = null;

    private PushTx() { ; }

    public static PushTx getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new PushTx();
        }

        return instance;
    }

    public String samourai(String hexString, List<Integer> strictModeVouts) {

        String _url = "pushtx/";

        String strStrictVouts = "";
        if(strictModeVouts != null && strictModeVouts.size() > 0) {
            strStrictVouts = "&strict_mode_vouts=";
            for(int i = 0; i < strictModeVouts.size(); i++) {
                strStrictVouts += strictModeVouts.get(i);
                if(i < (strictModeVouts.size() - 1)) {
                    strStrictVouts += "|";
                }
            }
        }

        try {
            String response = null;

            if(!TorManager.INSTANCE.isRequired())    {
                String _base = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET : WebUtil.SAMOURAI_API2;
                debug("PushTx", strStrictVouts);
                response = WebUtil.getInstance(context).postURL(_base + _url + "?at=" + APIFactory.getInstance(context).getAccessToken(), "tx=" + hexString + strStrictVouts);
            }
            else    {
                String _base = SamouraiWallet.getInstance().isTestNet() ? WebUtil.SAMOURAI_API2_TESTNET_TOR : WebUtil.SAMOURAI_API2_TOR;
                HashMap<String,String> args = new HashMap<String,String>();
                args.put("tx", hexString);
                if(strStrictVouts.length() > "&strict_mode_vouts=".length())    {
                    args.put("strict_mode_vouts", strStrictVouts.substring("&strict_mode_vouts=".length()));
                }
                response = WebUtil.getInstance(context).tor_postURL(_base + _url + "?at=" + APIFactory.getInstance(context).getAccessToken(), args);
            }

            return response;
        }
        catch(Exception e) {
            return null;
        }

    }

    public Pair<Boolean,String> pushTx(String hexTx) throws Exception {

        String response = null;
        boolean isOK = false;
        String txid = null;

        if(DO_SPEND)    {
            response = PushTx.getInstance(context).samourai(hexTx, null);
            if(response != null)    {
                JSONObject jsonObject = new org.json.JSONObject(response);
                if(jsonObject.has("status"))    {
                    if(jsonObject.getString("status").equals("ok"))    {
                        isOK = true;
                        if (jsonObject.has("data")) {
                            txid = jsonObject.getString("data");
                        }
                    }
                }
            }
            else    {
                throw new Exception(context.getString( R.string.pushtx_returns_null));
            }
        }
        else    {
            debug("PushTx", hexTx);
            isOK = true;
        }
        return new Pair<>(isOK,txid);

    }

}
