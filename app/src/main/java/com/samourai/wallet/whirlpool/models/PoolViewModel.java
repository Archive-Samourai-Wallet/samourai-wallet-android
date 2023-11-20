package com.samourai.wallet.whirlpool.models;

import com.samourai.wallet.util.tech.LogUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.WhirlpoolTx0;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

import java.util.List;

public class PoolViewModel extends com.samourai.whirlpool.client.whirlpool.beans.Pool {

    private boolean isSelected = false;
    private boolean isDisabled = false;
    private Long minerFee = null;
    private long totalMinerFee = 0L;
    private long totalEstimatedBytes = 0L;
    private WhirlpoolTx0 tx0;
    private Pool pool;
    private Tx0Preview tx0Preview = null;

    public PoolViewModel(Pool pool) {
        this.pool = pool;
        this.setDenomination(pool.getDenomination());
        this.setFeeValue(pool.getFeeValue());
        this.setPoolId(pool.getPoolId());
    }

    public void setMinerFee(Long minerFee, List<UTXOCoin> coins) {
        this.minerFee = minerFee;
        try {
            this.tx0 = new WhirlpoolTx0(this.getDenomination(), minerFee, 0, coins);
            this.totalMinerFee = minerFee;
            this.totalEstimatedBytes = tx0.getEstimatedBytes();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogUtil.error("PoolViewModel",ex.getMessage());
        }
    }

    public long getTotalFee() {
        return (tx0.getFeeSamourai() + tx0.getFee());
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setTx0Preview(Tx0Preview tx0Preview) {
        this.tx0Preview = tx0Preview;
    }

    public Tx0Preview getTx0Preview() {
        return tx0Preview;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public long getTotalEstimatedBytes() {
        return totalEstimatedBytes;
    }

    public Pool getPool() {
        return pool;
    }

    public Long getMinerFee() {
        return minerFee;
    }
}
