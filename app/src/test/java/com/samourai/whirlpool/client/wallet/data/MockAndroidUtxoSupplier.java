package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.send.provider.SimpleUtxoKeyProvider;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;

public class MockAndroidUtxoSupplier extends AndroidUtxoSupplier {
    private SimpleUtxoKeyProvider utxoKeyProvider;

    public MockAndroidUtxoSupplier(WalletSupplier walletSupplier,
                               UtxoConfigSupplier utxoConfigSupplier,
                               ChainSupplier chainSupplier,
                               PoolSupplier poolSupplier,
                               Tx0ParamService tx0ParamService,
                               NetworkParameters params,
                               UTXOFactory utxoFactory,
                               BIP47Util bip47Util,
                               BIP47Meta bip47Meta) throws Exception {
        super(walletSupplier, utxoConfigSupplier, chainSupplier, poolSupplier, tx0ParamService, params, utxoFactory, bip47Util, bip47Meta);
        this.utxoKeyProvider = new SimpleUtxoKeyProvider();
    }

    @Override
    public ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
        return utxoKeyProvider._getPrivKey(utxoHash, utxoIndex);
    }

    public void setKey(TransactionOutPoint outPoint, ECKey ecKey) {
        utxoKeyProvider.setKey(outPoint, ecKey);
    }
}
