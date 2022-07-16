package com.samourai.whirlpool.client.wallet.data;

import android.content.Context;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.CahootsUtxo;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.CahootsUtxoProvider;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.bitcoinj.core.ECKey;
import org.bouncycastle.util.encoders.Hex;

import java.util.LinkedList;
import java.util.List;

public class AndroidCahootsUtxoProvider implements CahootsUtxoProvider {
    private static AndroidCahootsUtxoProvider instance;

    public static synchronized AndroidCahootsUtxoProvider getInstance(Context ctx) {
        if (instance == null) {
            APIFactory apiFactory = APIFactory.getInstance(ctx);
            instance = new AndroidCahootsUtxoProvider(apiFactory);
        }
        return instance;
    }

    private APIFactory apiFactory;

    private AndroidCahootsUtxoProvider(APIFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    @Override
    public List<CahootsUtxo> getUtxosWpkhByAccount(int account) {
        List<UTXO> apiUtxos;
        if(account == SamouraiAccountIndex.POSTMIX)    {
            apiUtxos = apiFactory.getUtxosPostMix(true);
        }
        else    {
            apiUtxos = apiFactory.getUtxos(true);
        }
        apiUtxos = filterUtxosWpkh(apiUtxos);

        List<CahootsUtxo> utxos = new LinkedList<>();
        for(UTXO utxo : apiUtxos)   {
            MyTransactionOutPoint outpoint = utxo.getOutpoints().get(0);
            String address = outpoint.getAddress();
            String path = apiFactory.getUnspentPaths().get(address);
            if(path != null)   {
                ECKey ecKey = SendFactory.getPrivKey(address, account);
                CahootsUtxo cahootsUtxo = new CahootsUtxo(outpoint, path, ecKey.getPrivKeyBytes());
                utxos.add(cahootsUtxo);
            }
        }
        return utxos;
    }

    protected static List<UTXO> filterUtxosWpkh(List<UTXO> utxos) {
        List<UTXO> filteredUtxos = new LinkedList<>();
        for(UTXO utxo : utxos)   {
            // filter wpkh
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if (Bech32UtilGeneric.getInstance().isP2WPKHScript(script)) {
                filteredUtxos.add(utxo);
            }
        }
        return filteredUtxos;
    }
}
