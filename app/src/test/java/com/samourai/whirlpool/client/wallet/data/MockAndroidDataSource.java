package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;

import java.util.List;

import util.Log;

public class MockAndroidDataSource extends AndroidDataSource {
    public MockAndroidDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister, PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory, BIP47Util bip47Util, BIP47Meta bip47Meta) throws Exception {
        super(whirlpoolWallet, bip44w, dataPersister, pushTx, feeUtil, apiFactory, utxoFactory, bip47Util, bip47Meta);
        WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
        this.utxoSupplier = new MockAndroidUtxoSupplier(walletSupplier, dataPersister.getUtxoConfigSupplier(), chainSupplier, poolSupplier, tx0ParamService, config.getNetworkParameters(), utxoFactory, bip47Util, bip47Meta);
    }

    @Override
    protected MinerFeeSupplier computeMinerFeeSupplier(FeeUtil feeUtil) {
        return new MockAndroidMinerFeeSupplier();
    }

    @Override
    public void pushTx(String txHex) throws Exception {
        Log.d("TEST", "pushTX ignored for test: "+txHex);
    }

    @Override
    public void pushTx(String txHex, List<Integer> strictModeVouts) throws Exception {
        Log.d("TEST", "pushTX ignored for test: "+txHex);
    }
}
