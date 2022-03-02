package com.samourai.whirlpool.client.wallet.data;

import android.util.Pair;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipFormat.BipFormatSupplierImpl;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class AndroidDataSource implements DataSource {
    private PushTx pushTx;

    private WalletSupplier walletSupplier;
    private MinerFeeSupplier minerFeeSupplier;
    private ChainSupplier chainSupplier;
    private Tx0PreviewService tx0PreviewService;
    private ExpirablePoolSupplier poolSupplier;
    private UtxoSupplier utxoSupplier;
    private final BipFormatSupplier bipFormatSupplier;

    public AndroidDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister, PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory, BIP47Util bip47Util, BIP47Meta bip47Meta) throws Exception {
        this.pushTx = pushTx;
        WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
        this.walletSupplier = new WalletSupplierImpl(walletStateSupplier, bip44w);
        this.minerFeeSupplier = new AndroidMinerFeeSupplier(feeUtil);
        this.chainSupplier = new AndroidChainSupplier(apiFactory);
        WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
        this.tx0PreviewService = new Tx0PreviewService(minerFeeSupplier, config);
        this.poolSupplier = new ExpirablePoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi(), tx0PreviewService);
        this.bipFormatSupplier = new BipFormatSupplierImpl();
        this.utxoSupplier = new AndroidUtxoSupplier(walletSupplier, dataPersister.getUtxoConfigSupplier(), chainSupplier, poolSupplier, bipFormatSupplier, config.getNetworkParameters(), utxoFactory, bip47Util, bip47Meta);
    }

    @Override
    public void open() throws Exception {
        // load pools (or fail)
        poolSupplier.load();
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public String pushTx(String txHex) throws Exception {
        Pair<Boolean,String> result = pushTx.pushTx(txHex);
        if (!result.first) {
            throw new NotifiableException("pushTx failed");
        }
        return result.second; // txid
    }

    @Override
    public WalletSupplier getWalletSupplier() {
        return walletSupplier;
    }

    @Override
    public UtxoSupplier getUtxoSupplier() {
        return utxoSupplier;
    }

    @Override
    public MinerFeeSupplier getMinerFeeSupplier() {
        return minerFeeSupplier;
    }

    @Override
    public ChainSupplier getChainSupplier() {
        return chainSupplier;
    }

    @Override
    public PoolSupplier getPoolSupplier() {
        return poolSupplier;
    }

    @Override
    public Tx0PreviewService getTx0PreviewService() {
        return tx0PreviewService;
    }

    @Override
    public PaynymSupplier getPaynymSupplier() {
        return null; // not implemented
    }
}
