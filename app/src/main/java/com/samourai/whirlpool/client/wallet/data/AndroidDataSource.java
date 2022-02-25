package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.PushTxResponse;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceWithStrictMode;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplierImpl;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import java.util.List;

public class AndroidDataSource implements DataSource, DataSourceWithStrictMode {
    private PushTx pushTx;

    protected WalletSupplier walletSupplier;
    protected MinerFeeSupplier minerFeeSupplier;
    protected ChainSupplier chainSupplier;
    protected Tx0ParamService tx0ParamService;
    protected ExpirablePoolSupplier poolSupplier;
    protected UtxoSupplier utxoSupplier;

    public AndroidDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, DataPersister dataPersister, PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory, BIP47Util bip47Util, BIP47Meta bip47Meta) throws Exception {
        this.pushTx = pushTx;
        WalletStateSupplier walletStateSupplier = dataPersister.getWalletStateSupplier();
        this.walletSupplier = new WalletSupplierImpl(bip44w, walletStateSupplier);
        this.minerFeeSupplier = computeMinerFeeSupplier(feeUtil);
        this.chainSupplier = new AndroidChainSupplier(apiFactory);
        WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
        this.tx0ParamService = new Tx0ParamService(minerFeeSupplier, config);
        this.poolSupplier = new ExpirablePoolSupplier(config.getRefreshPoolsDelay(), config.getServerApi(), tx0ParamService);
        this.utxoSupplier = new AndroidUtxoSupplier(walletSupplier, dataPersister.getUtxoConfigSupplier(), chainSupplier, poolSupplier, tx0ParamService, config.getNetworkParameters(), utxoFactory, bip47Util, bip47Meta);
    }

    protected MinerFeeSupplier computeMinerFeeSupplier(FeeUtil feeUtil) {
        return new AndroidMinerFeeSupplier(feeUtil);
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
    public void pushTx(String txHex, List<Integer> strictModeVouts) throws Exception {
        String response = pushTx.samourai(txHex, strictModeVouts);

        // check strict-mode response
        PushTxResponse pushTxResponse = JSONUtils.getInstance().getObjectMapper().readValue(response, PushTxResponse.class);
        BackendApi.checkPushTxResponse(pushTxResponse);
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
