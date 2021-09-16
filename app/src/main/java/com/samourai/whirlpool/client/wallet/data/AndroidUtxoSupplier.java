package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.client.BipWallet;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AndroidUtxoSupplier extends BasicUtxoSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidUtxoSupplier.class.getSimpleName());

    private UTXOFactory utxoFactory;
    private long lastUpdate;

    public AndroidUtxoSupplier(WalletSupplier walletSupplier,
                               UtxoConfigSupplier utxoConfigSupplier,
                               ChainSupplier chainSupplier,
                               PoolSupplier poolSupplier,
                               Tx0ParamService tx0ParamService,
                               NetworkParameters params,
                               UTXOFactory utxoFactory) throws Exception {
        super(walletSupplier, utxoConfigSupplier, chainSupplier, poolSupplier, tx0ParamService, params);
        this.utxoFactory = utxoFactory;
        this.lastUpdate = -1;
    }

    @Override
    public UtxoData getValue() {
        UtxoData value = super.getValue();
        if (value == null || lastUpdate < utxoFactory.getLastUpdate()) {
            // fetch value
            value = computeValue();

            // set
            try {
                setValue(value);
                lastUpdate = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("utxoSupplier.setValue failed!");
            }
        }
        return value;
    }

    @Override
    public void refresh() {
        this.lastUpdate = 0;
    }

    private UtxoData computeValue() {
        if (log.isDebugEnabled()) {
            log.debug("utxoSupplier.computeValue()");
        }
        List<UnspentOutput> utxos = new LinkedList();
        utxos.addAll(toUnspentOutputs(utxoFactory.getAllP2PKH().values(), WhirlpoolAccount.DEPOSIT, AddressType.LEGACY));
        utxos.addAll(toUnspentOutputs(utxoFactory.getAllP2SH_P2WPKH().values(), WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_COMPAT));
        utxos.addAll(toUnspentOutputs(utxoFactory.getAllP2WPKH().values(), WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_NATIVE));
        utxos.addAll(toUnspentOutputs(utxoFactory.getPreMix().values(), WhirlpoolAccount.PREMIX, AddressType.SEGWIT_NATIVE));
        utxos.addAll(toUnspentOutputs(utxoFactory.getAllPostMix().values(), WhirlpoolAccount.POSTMIX, AddressType.SEGWIT_NATIVE));

        UnspentOutput[] utxosArray = utxos.toArray(new UnspentOutput[]{});
        WalletResponse.Tx[] txs = new WalletResponse.Tx[]{}; // ignored
        return new UtxoData(utxosArray, txs);
    }

    private Collection<UnspentOutput> toUnspentOutputs(Collection<UTXO> utxos, WhirlpoolAccount whirlpoolAccount, AddressType addressType) {
        List<UnspentOutput> unspentOutputs = new LinkedList<>();

        BipWallet bipWallet = getWalletSupplier().getWallet(whirlpoolAccount, addressType);
        if (bipWallet == null) {
            log.error("Wallet not found for "+whirlpoolAccount+"/"+addressType);
            return unspentOutputs;
        }
        String xpub = bipWallet.getPub(addressType);
        for (UTXO utxo : utxos) {
            Collection<UnspentOutput> unspents = utxo.toUnspentOutputs(xpub);
            unspentOutputs.addAll(unspents);
        }
        return unspentOutputs;
    }
}
