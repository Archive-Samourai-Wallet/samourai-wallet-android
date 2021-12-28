package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplierImpl;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

public class AndroidDataSource implements DataSource {
    private PushTx pushTx;

    private WalletSupplier walletSupplier;
    private MinerFeeSupplier minerFeeSupplier;
    private ChainSupplier chainSupplier;
    private Tx0ParamService tx0ParamService;
    private ExpirablePoolSupplier poolSupplier;
    private UtxoSupplier utxoSupplier;

    public AndroidDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister, PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory) throws Exception {
        this.pushTx = pushTx;
        WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
        this.walletSupplier = new WalletSupplierImpl(bip44w, walletStateSupplier);
        this.minerFeeSupplier = new AndroidMinerFeeSupplier(feeUtil);
        this.chainSupplier = new AndroidChainSupplier(apiFactory);
        WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
        this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);
        this.poolSupplier = new ExpirablePoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
        this.utxoSupplier = new AndroidUtxoSupplier(walletSupplier, dataPersister.getUtxoConfigSupplier(), chainSupplier, poolSupplier, tx0ParamService, config.getNetworkParameters(), utxoFactory);
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
    public void pushTx(String txHex) throws Exception {
        pushTx.pushTx(txHex);
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
    public Tx0ParamService getTx0ParamService() {
        return tx0ParamService;
    }
}
