package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.whirlpool.client.wallet.AndroidWalletSupplier;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.bitcoinj.core.ECKey;

import java.util.LinkedList;
import java.util.List;

public class AndroidCahootsWallet extends CahootsWallet {
    private static AndroidCahootsWallet instance = null;

    private APIFactory apiFactory;

    public static AndroidCahootsWallet getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidCahootsWallet(ctx);
        }
        return instance;
    }

    private AndroidCahootsWallet(Context ctx) {
        super(AndroidWalletSupplier.getInstance(ctx), BIP_FORMAT.PROVIDER, SamouraiWallet.getInstance().getCurrentNetworkParams());
        this.apiFactory = APIFactory.getInstance(ctx);
    }

    @Override
    public long fetchFeePerB() {
        long feePerB = FeeUtil.getInstance().getSuggestedFeeDefaultPerB();
        return feePerB;
    }

    @Override
    protected List<CahootsUtxo> fetchUtxos(int account) {
        List<UTXO> apiUtxos;
        if(account == SamouraiAccountIndex.POSTMIX)    {
            apiUtxos = apiFactory.getUtxosPostMix(true);
        }
        else    {
            apiUtxos = apiFactory.getUtxos(true);
        }

        List<CahootsUtxo> utxos = new LinkedList<>();
        for(UTXO utxo : apiUtxos)   {
            MyTransactionOutPoint outpoint = utxo.getOutpoints().get(0);
            String address = outpoint.getAddress();
            String path = apiFactory.getUnspentPaths().get(address);
            if(path != null)   {
                ECKey ecKey = SendFactory.getPrivKey(address, account);
                CahootsUtxo cahootsUtxo = new CahootsUtxo(outpoint, path, ecKey);
                utxos.add(cahootsUtxo);
            }
        }
        return utxos;
    }
}
