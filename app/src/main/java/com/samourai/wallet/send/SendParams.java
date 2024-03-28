package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.BatchSendUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.samourai.wallet.send.SendActivity.SPEND_BOLTZMANN;
import static com.samourai.wallet.send.SendActivity.SPEND_SIMPLE;
import static com.samourai.wallet.util.tech.LogUtil.debug;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class SendParams	{

    private static List<MyTransactionOutPoint> outpoints = null;
    private static Map<String, BigInteger> receivers = null;
    private static String strPCode = null;
    private int SPEND_TYPE =  SPEND_BOLTZMANN;
    private long changeAmount = 0L;
    private int changeType = 49;
    private int account = 0;
    private String strDestAddress = null;
    private boolean hasPrivacyWarning = false;
    private boolean hasPrivacyChecked = false;
    private long spendAmount = 0L;
    private int changeIdx = 0;
    private String note = null;
    private List<BatchSendUtil.BatchSend> batchSend = null;

    private static SendParams instance = null;

    private SendParams () { ; }

    public static SendParams getInstance() {

        if(instance == null)	{
            instance = new SendParams();
        }

        return instance;
    }

    public void reset() {
        this.outpoints = null;
        this.receivers = null;
        this.strPCode = null;
        this.batchSend = null;
        this.SPEND_TYPE = SPEND_BOLTZMANN;
        this.changeAmount = 0L;
        this.changeType = 49;
        this.account = 0;
        this.strDestAddress = null;
        this.hasPrivacyWarning = false;
        this.hasPrivacyChecked = false;
        this.spendAmount = 0L;
        this.changeIdx = 0;
        this.note = null;
    }

    public void setParams(List<MyTransactionOutPoint> outpoints,
                          Map<String, BigInteger> receivers,
                          String strPCode,
                          int SPEND_TYPE,
                          long changeAmount,
                          int changeType,
                          int account,
                          String strDestAddress,
                          boolean hasPrivacyWarning,
                          boolean hasPrivacyChecked,
                          long spendAmount,
                          int changeIdx) {
        setParams(
                outpoints,
                receivers,
                strPCode,
                SPEND_TYPE,
                changeAmount,
                changeType,
                account,
                strDestAddress,
                hasPrivacyWarning,
                hasPrivacyChecked,
                spendAmount,
                changeIdx,
                null);
    }

    public void setParams(List<MyTransactionOutPoint> outpoints,
                          Map<String, BigInteger> receivers,
                          String strPCode,
                          int SPEND_TYPE,
                          long changeAmount,
                          int changeType,
                          int account,
                          String strDestAddress,
                          boolean hasPrivacyWarning,
                          boolean hasPrivacyChecked,
                          long spendAmount,
                          int changeIdx,
                          String note) {

        this.outpoints = outpoints;
        this.receivers = receivers;
        this.strPCode = strPCode;
        this.batchSend = null;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.account = account;
        this.strDestAddress = strDestAddress;
        this.hasPrivacyWarning = hasPrivacyWarning;
        this.hasPrivacyChecked = hasPrivacyChecked;
        this.spendAmount = spendAmount;
        this.changeIdx = changeIdx;
        this.note = defaultIfBlank(note, StringUtils.EMPTY);
    }

    public void setParams(List<MyTransactionOutPoint> outpoints,
                          Map<String, BigInteger> receivers,
                          List<BatchSendUtil.BatchSend> batchSend,
                          int SPEND_TYPE,
                          long changeAmount,
                          int changeType,
                          int account,
                          String strDestAddress,
                          boolean hasPrivacyWarning,
                          boolean hasPrivacyChecked,
                          long spendAmount,
                          int changeIdx,
                          String note) {

        this.outpoints = outpoints;
        this.receivers = receivers;
        this.strPCode = null;
        this.batchSend = batchSend;
        this.SPEND_TYPE = SPEND_TYPE;
        this.changeAmount = changeAmount;
        this.changeType = changeType;
        this.account = account;
        this.strDestAddress = strDestAddress;
        this.hasPrivacyWarning = hasPrivacyWarning;
        this.hasPrivacyChecked = hasPrivacyChecked;
        this.spendAmount = spendAmount;
        this.changeIdx = changeIdx;
        this.note = defaultIfBlank(note, StringUtils.EMPTY);
    }

    public List<MyTransactionOutPoint> getOutpoints()   {
        return outpoints;
    }

    public Map<String, BigInteger> getReceivers() {
        return receivers;
    }

    public String getPCode() {
        return strPCode;
    }

    public List<BatchSendUtil.BatchSend> getBatchSend() {
        return batchSend;
    }

    public int getSpendType()   {
        return SPEND_TYPE;
    }

    public long getChangeAmount()   {
        return changeAmount;
    }

    public int getChangeType()  {
        return changeType;
    }

    public int getAccount()  {
        return account;
    }

    public String getDestAddress() {
        return strDestAddress;
    }

    public boolean hasPrivacyWarning()   {
        return hasPrivacyWarning;
    }

    public boolean hasPrivacyChecked()   {
        return hasPrivacyChecked;
    }

    public long getSpendAmount()    {
        return spendAmount;
    }

    public int getChangeIdx()   {
        return changeIdx;
    }

    public String getNote() {
        return note;
    }

    public List<Integer> getSpendOutputIndex(Transaction tx)   {

        List<Integer> ret = new ArrayList<Integer>();

        for (int i = 0; i < tx.getOutputs().size(); i++) {
            TransactionOutput output = tx.getOutput(i);
            Script script = output.getScriptPubKey();
            String scriptPubKey = Hex.toHexString(script.getProgram());
            Address _p2sh = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
            Address _p2pkh = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
            try {
                if(Bech32Util.getInstance().isBech32Script(scriptPubKey)) {
                    if(Bech32Util.getInstance().getAddressFromScript(scriptPubKey).compareToIgnoreCase(getDestAddress()) == 0) {
                        debug("SendParams", "send address identified:" + Bech32Util.getInstance().getAddressFromScript(scriptPubKey));
                        debug("SendParams", "send address output index:" + i);
                        ret.add(i);
                    }
                }
                else if(_p2sh != null && _p2pkh == null && _p2sh.toString().compareTo(getDestAddress()) == 0) {
                    debug("SendParams", "send address identified:" + _p2sh.toString());
                    debug("SendParams", "send address output index:" + i);
                    ret.add(i);
                }
                else if(_p2sh == null && _p2pkh != null && _p2pkh.toString().compareTo(getDestAddress()) == 0) {
                    debug("SendParams", "send address identified:" + _p2pkh.toString());
                    debug("SendParams", "send address output index:" + i);
                    ret.add(i);
                }
                else  {
                    ;
                }
            } catch (Exception e) {
                ;
            }

        }

        return ret;
    }

    public boolean isPostmixAccount(final Context ctx) {
        return account == WhirlpoolMeta.getInstance(ctx).getWhirlpoolPostmix();
    }

    public boolean isBadBankAccount(final Context ctx) {
        return account == WhirlpoolMeta.getInstance(ctx).getWhirlpoolBadBank();
    }

    public String generateChangeAddress(final Context ctx) {
        return generateChangeAddress(
                ctx,
                changeAmount,
                SPEND_TYPE,
                account,
                changeType,
                changeIdx,
                false);
    }

    public static String generateChangeAddress(final Context ctx,
                                               final long changeAmount,
                                               final int spendType,
                                               final int account,
                                               final int changeType,
                                               final int changeIndex,
                                               final boolean forTxBuilding) {

        if (changeAmount <= 0l) return null;
        if (spendType != SPEND_SIMPLE && (forTxBuilding || spendType != SPEND_BOLTZMANN)) return null;
        if (account == SamouraiAccountIndex.POSTMIX) {
            if (changeType == 44) {
                final HD_Address hd_addr = BIP84Util.getInstance(ctx).getWallet()
                        .getAccount(WhirlpoolMeta.getInstance(ctx)
                                .getWhirlpoolPostmix()).getChain(AddressFactory.CHANGE_CHAIN)
                        .getAddressAt(changeIndex);
                return hd_addr.getAddressString();
            } else if (changeType == 49) {
                final HD_Address hd_addr = BIP84Util.getInstance(ctx).getWallet()
                        .getAccount(WhirlpoolMeta.getInstance(ctx)
                                .getWhirlpoolPostmix()).getChain(AddressFactory.CHANGE_CHAIN)
                        .getAddressAt(changeIndex);
                final NetworkParameters networkParams = SamouraiWallet.getInstance()
                        .getCurrentNetworkParams();
                return new SegwitAddress(hd_addr.getECKey(), networkParams).getAddressAsString();
            } else {
                return BIP84Util.getInstance(ctx)
                        .getAddressAt(
                                WhirlpoolMeta.getInstance(ctx).getWhirlpoolPostmix(),
                                AddressFactory.CHANGE_CHAIN, changeIndex).getBech32AsString();
            }
        } else if (changeType == 84) {
            return BIP84Util.getInstance(ctx)
                    .getAddressAt(AddressFactory.CHANGE_CHAIN, changeIndex).getBech32AsString();
        } else if (changeType == 49) {
            return BIP49Util.getInstance(ctx)
                    .getAddressAt(AddressFactory.CHANGE_CHAIN, changeIndex).getAddressAsString();
        } else {
            return HD_WalletFactory.getInstance(ctx).get().getAccount(0).getChange()
                    .getAddressAt(changeIndex).getAddressString();
        }
    }
}
