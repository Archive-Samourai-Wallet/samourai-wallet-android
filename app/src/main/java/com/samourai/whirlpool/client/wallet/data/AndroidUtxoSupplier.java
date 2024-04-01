package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.constants.BIP_WALLET;
import com.samourai.wallet.send.UTXO;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceConfig;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AndroidUtxoSupplier extends BasicUtxoSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidUtxoSupplier.class);

    private APIFactory apiFactory;
    private BIP47Util bip47Util;
    private BIP47Meta bip47Meta;
    private long lastUpdate;

    public AndroidUtxoSupplier(WalletSupplier walletSupplier, UtxoConfigSupplier utxoConfigSupplier, DataSourceConfig dataSourceConfig,
                               APIFactory apiFactory,
                               BIP47Util bip47Util,
                               BIP47Meta bip47Meta,
                               NetworkParameters params) {
        super(walletSupplier, utxoConfigSupplier, dataSourceConfig, params);
        this.apiFactory = apiFactory;
        this.bip47Util = bip47Util;
        this.bip47Meta = bip47Meta;
        this.lastUpdate = -1;
    }

    @Override
    public UtxoData getValue() {
        UtxoData value = super.getValue();
        if (value == null || lastUpdate < WhirlpoolUtils.getInstance().getUtxoLastChange()) {
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

    private synchronized UtxoData computeValue() {
        if (log.isDebugEnabled()) {
            log.debug("utxoSupplier.computeValue()");
        }
        List<UnspentOutput> utxos = new LinkedList();
        utxos.addAll(toUnspentOutputs(apiFactory.getUtxosP2PKH(true), BIP_WALLET.DEPOSIT_BIP44));
        utxos.addAll(toUnspentOutputs(apiFactory.getUtxosP2SH_P2WPKH(true), BIP_WALLET.DEPOSIT_BIP49));
        utxos.addAll(toUnspentOutputs(apiFactory.getUtxosP2WPKH(true), BIP_WALLET.DEPOSIT_BIP84));
        utxos.addAll(toUnspentOutputs(apiFactory.getUtxosPreMix(), BIP_WALLET.PREMIX_BIP84));
        utxos.addAll(toUnspentOutputs(apiFactory.getUtxosPostMix(true), BIP_WALLET.POSTMIX_BIP84));

        UnspentOutput[] utxosArray = utxos.toArray(new UnspentOutput[]{});
        WalletResponse.Tx[] txs = new WalletResponse.Tx[]{}; // ignored
        int latestBlockHeight = (int)apiFactory.getLatestBlockHeight();
        return new UtxoData(utxosArray, txs, latestBlockHeight);
    }

    private Collection<UnspentOutput> toUnspentOutputs(Collection<UTXO> utxos, BIP_WALLET bip_wallet) {
        List<UnspentOutput> unspentOutputs = new LinkedList<>();

        BipWallet bipWallet = getWalletSupplier().getWallet(bip_wallet);
        if (bipWallet == null) {
            log.error("Wallet not found for "+bip_wallet.name());
            return unspentOutputs;
        }
        for (UTXO utxo : utxos) {
            Collection<UnspentOutput> unspents = utxo.toUnspentOutputs();
            unspentOutputs.addAll(unspents);
        }
        if (log.isDebugEnabled()) {
            log.debug("set utxos["+bipWallet.getId()+"] = "+utxos.size()+" UTXO = "+unspentOutputs.size()+" unspentOutputs");
        }
        return unspentOutputs;
    }

    @Override
    public byte[] _getPrivKeyBip47(UnspentOutput unspentOutput) throws Exception {
        String address = unspentOutput.addr;
        String pcode = bip47Meta.getPCode4Addr(address);
        int idx = bip47Meta.getIdx4Addr(address);
        if (log.isDebugEnabled()) {
            log.debug("_getPrivKeyBip47: pcode="+pcode+", idx="+idx);
        }
        return bip47Util.getReceiveAddress(new PaymentCode(pcode), idx).getECKey().getPrivKeyBytes();
    }
}
