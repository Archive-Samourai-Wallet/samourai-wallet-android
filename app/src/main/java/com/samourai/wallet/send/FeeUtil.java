package com.samourai.wallet.send;

import static java.lang.Math.max;
import static java.util.Objects.isNull;

import android.util.Log;

import com.google.common.collect.Maps;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.fee.EnumFeeRepresentation;
import com.samourai.wallet.api.fee.RawFees;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.NetworkParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class FeeUtil extends com.samourai.wallet.util.FeeUtil {

    private static SuggestedFee suggestedFee = null;
    private static SuggestedFee highFee = null;
    private static SuggestedFee normalFee = null;
    private static SuggestedFee lowFee = null;
    private static List<SuggestedFee> estimatedFees = null;
    private static Map<EnumFeeRepresentation, RawFees> rawFeesMap = null;
    private static EnumFeeRepresentation feeRepresentation = null;

    private static FeeUtil instance = null;

    private FeeUtil() { ; }

    public static FeeUtil getInstance() {

        if(instance == null)    {
            estimatedFees = new ArrayList<>();
            highFee = new SuggestedFee();
            suggestedFee = new SuggestedFee();
            lowFee = new SuggestedFee();
            rawFeesMap = Maps.newConcurrentMap();
            instance = new FeeUtil();
        }

        return instance;
    }

    public FeeUtil putRawFees(final RawFees rawFees) {
        if (isNull(rawFees)) return this;
        if (rawFees.hasFee()) {
            final EnumFeeRepresentation feeRepresentation = rawFees.getFeeRepresentation();
            rawFeesMap.put(feeRepresentation, rawFees);
            setEstimatedFees(feeRepresentation.createSuggestedFeeList(rawFees));
            FeeUtil.feeRepresentation = feeRepresentation;
        }
        return this;
    }

    public EnumFeeRepresentation getFeeRepresentation() {
        return feeRepresentation;
    }

    public RawFees getRawFees() {
        return getRawFees(feeRepresentation);
    }

    public RawFees getRawFees(final EnumFeeRepresentation feeRepresentation) {
        return rawFeesMap.get(feeRepresentation);
    }

    public String retrievesNearFeeIdentifier(final int fee) {
        if (isNull(feeRepresentation)) return null;
        return rawFeesMap.get(feeRepresentation).retrievesNearRepresentation(fee);
    }

    public synchronized SuggestedFee getSuggestedFee() {
        if(suggestedFee != null)    {
            return suggestedFee;
        }
        else    {
            SuggestedFee fee = new SuggestedFee();
            fee.setDefaultPerKB(BigInteger.valueOf(10000L));
            return fee;
        }
    }

    public synchronized void setSuggestedFee(SuggestedFee suggestedFee) {
        FeeUtil.suggestedFee = suggestedFee;
    }

    public synchronized SuggestedFee getHighFee() {
        if(highFee == null)    {
            highFee = getSuggestedFee();
        }

        return highFee;
    }

    public synchronized void setHighFee(SuggestedFee highFee) {
        FeeUtil.highFee = highFee;
    }

    public synchronized SuggestedFee getNormalFee() {
        if(normalFee == null)    {
            normalFee = getSuggestedFee();
        }

        return normalFee;
    }

    public synchronized void setNormalFee(SuggestedFee normalFee) {
        FeeUtil.normalFee = normalFee;
    }

    public synchronized SuggestedFee getLowFee() {
        if(lowFee == null)    {
            lowFee = getSuggestedFee();
        }

        return lowFee;
    }

    public synchronized void setLowFee(SuggestedFee lowFee) {
        FeeUtil.lowFee = lowFee;
    }

    public synchronized List<SuggestedFee> getEstimatedFees() {
        return estimatedFees;
    }

    private synchronized void setEstimatedFees(List<SuggestedFee> estimatedFees) {
        FeeUtil.estimatedFees = estimatedFees;

        switch(estimatedFees.size()) {
            case 1:
                suggestedFee = highFee = normalFee = lowFee = estimatedFees.get(0);
                break;
            case 2:
                suggestedFee = highFee = normalFee = estimatedFees.get(0);
                lowFee = estimatedFees.get(1);
                break;
            case 3:
                highFee = estimatedFees.get(0);
                suggestedFee = estimatedFees.get(1);
                normalFee = estimatedFees.get(1);
                lowFee = estimatedFees.get(2);
                break;
            default:
                break;
        }

    }

    public BigInteger estimatedFee(int inputs, int outputs)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public BigInteger estimatedFeeSegwit(int inputsP2PKH, int inputsP2SHP2WPKH, int inputsP2WPKH, int outputs)   {
        int size = estimatedSizeSegwit(inputsP2PKH, inputsP2SHP2WPKH, inputsP2WPKH, outputs, 0);
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public BigInteger estimatedFeeSegwit(
            final int inputsP2PKH,
            final int inputsP2SHP2WPKH,
            final int inputsP2WPKH,
            final int outputsNonTaproot,
            final int outputsTaproot)   {

        final int size = estimatedSizeSegwit(
                inputsP2PKH,
                inputsP2SHP2WPKH,
                inputsP2WPKH,
                outputsNonTaproot,
                outputsTaproot,
                0);

        if (SamouraiWallet.getInstance().isTestNet()) {
            return calculateFee(size + 1, getSuggestedFee().getDefaultPerKB());
        }
        return calculateFee(size, getSuggestedFee().getDefaultPerKB());
    }

    public int estimatedSize(int inputs, int outputs)   {
        return estimatedSizeSegwit(inputs, 0, 0, outputs, 0);
    }

    public BigInteger estimatedFee(int inputs, int outputs, BigInteger feePerKb)   {
        int size = estimatedSize(inputs, outputs);
        return calculateFee(size, feePerKb);
    }

    public BigInteger calculateFee(final int txSize, final BigInteger feePerKb)   {
        final long feePerB = toFeePerB(feePerKb);
        return BigInteger.valueOf(calculateFee(txSize, feePerB));
    }

    public void sanitizeFee()  {
        if(FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() < 1000L)    {
            SuggestedFee suggestedFee = new SuggestedFee();
            suggestedFee.setDefaultPerKB(BigInteger.valueOf(1200L));
            Log.d("FeeUtil", "adjusted fee:" + suggestedFee.getDefaultPerKB().longValue());
            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
        }
    }

    public long getSuggestedFeeDefaultPerB() {
        return toFeePerB(getSuggestedFee().getDefaultPerKB());
    }

    public Triple<Integer, Integer, Integer> getOutpointCount(Vector<MyTransactionOutPoint> outpoints) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
        return super.getOutpointCount(outpoints, params);
    }

    public void normalize() {

        long feeLow = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue();
        long feeMed = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue();
        long feeHigh = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue();

        /**
         * Finally we don't want to adjust the fees from 1$fee estimator.
         * So this condition is added to avoid that.
         */
        if (!FeeUtil.getInstance().getFeeRepresentation().is1DolFeeEstimator()) {
            if (feeLow == feeMed && feeMed == feeHigh) {
                // offset of low and high
                feeLow = max(1, feeMed * 85L / 100L);
                feeHigh = feeMed * 115L / 100L;
                final SuggestedFee lo_sf = new SuggestedFee();
                lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow / 1000L * 1000L));
                FeeUtil.getInstance().setLowFee(lo_sf);
                final SuggestedFee hi_sf = new SuggestedFee();
                hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh / 1000L * 1000L));
                FeeUtil.getInstance().setHighFee(hi_sf);
            } else if (feeLow == feeMed || feeMed == feeHigh) {
                if (FeeUtil.getInstance().getFeeRepresentation().isBitcoindFeeEstimator()) {
                    // offset of mid
                    feeMed = (feeLow + feeHigh) / 2L;
                    final SuggestedFee mi_sf = new SuggestedFee();
                    mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed / 1000L * 1000L));
                    FeeUtil.getInstance().setNormalFee(mi_sf);
                } else if (feeLow == feeMed) {
                    feeLow = max(1, feeMed * 85L / 100L);
                    final SuggestedFee lo_sf = new SuggestedFee();
                    lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow / 1000L * 1000L));
                    FeeUtil.getInstance().setLowFee(lo_sf);
                } else {
                    feeHigh = feeMed * 115L / 100L;
                    final SuggestedFee hi_sf = new SuggestedFee();
                    hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh / 1000L * 1000L));
                    FeeUtil.getInstance().setHighFee(hi_sf);
                }
            }
        }

        /**
         * just for security, avoid inconsistent value
         */
        if (feeLow < 1000L) {
            feeLow = 1000L;
            final SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (feeMed < 1000L) {
            feeMed = 1000L;
            final SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (feeHigh < 1000L) {
            feeHigh = 1000L;
            final SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }
    }
}
