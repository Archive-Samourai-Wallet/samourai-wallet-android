package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.httpClient.HttpResponseException;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.network.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.samourai.wallet.util.tech.LogUtil.debug;

public class PushTx implements IPushTx {

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

    public String samourai(String hexString, List<Integer> strictModeVouts) throws Exception {

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

            if(!SamouraiTorManager.INSTANCE.isRequired())    {
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
        catch(HttpResponseException e) {
            try{
                JSONObject object = new JSONObject(e.getResponseBody());
                if(object.has("error")){
                    throw new Exception(object.getString("error"),e);
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } catch(Exception e) {

            e.printStackTrace();
            return null;
        }

    }

    @Override
    public String pushTx(String hexTx) throws Exception {
        return pushTx(hexTx, null);
    }

    @Override
    public String pushTx(String hexTx, Collection<Integer> strictModeVoutsOrNull) throws Exception {
        String txid = null;

        if(DO_SPEND)    {
            List<Integer> listVouts = strictModeVoutsOrNull!=null ? new LinkedList<>(strictModeVoutsOrNull) : null;
            String response = PushTx.getInstance(context).samourai(hexTx, listVouts);
            if(response == null) {
                throw new Exception(context.getString(R.string.pushtx_returns_null));
            }
            JSONObject jsonObject = new org.json.JSONObject(response);
            if(!jsonObject.has("status") || !jsonObject.getString("status").equals("ok")) {
                throw new Exception(context.getString(R.string.pushtx_returns_null));
            }
            if (jsonObject.has("data")) {
                txid = jsonObject.getString("data");
            }
        }
        else    {
            debug("PushTx", hexTx);
        }
        return txid;
    }
}
