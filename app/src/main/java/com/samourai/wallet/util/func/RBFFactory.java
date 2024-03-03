package com.samourai.wallet.util.func;

import static com.samourai.wallet.util.tech.LogUtil.debug;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.spongycastle.util.encoders.Hex;

public class RBFFactory {

    public static final String TAG = "RBFFactory";

    private RBFFactory() {}

    @Nullable
    public static RBFSpend createRBFSpendFromTx(final Transaction tx, final Context context) {

        final PrefsUtil prefsUtil = PrefsUtil.getInstance(context);
        if (! prefsUtil.getValue(PrefsUtil.RBF_OPT_IN, false)) return null;

        final RBFSpend rbf = new RBFSpend();
        for (final TransactionInput input : tx.getInputs()) {

            boolean _isBIP49 = false;
            boolean _isBIP84 = false;
            String _addr = null;
            String script = Hex.toHexString(input.getConnectedOutput().getScriptBytes());

            final NetworkParameters netParams = SamouraiWallet.getInstance().getCurrentNetworkParams();

            if (Bech32Util.getInstance().isBech32Script(script)) {
                try {
                    _addr = Bech32Util.getInstance().getAddressFromScript(script);
                    _isBIP84 = true;
                } catch (Exception e) {
                    ;
                }
            } else {
                Address _address = input.getConnectedOutput().getAddressFromP2SH(netParams);
                if (_address != null) {
                    _addr = _address.toString();
                    _isBIP49 = true;
                }
            }
            if (_addr == null) {
                _addr = input.getConnectedOutput().getAddressFromP2PKHScript(netParams).toString();
            }

            String path = APIFactory.getInstance(context).getUnspentPaths().get(_addr);
            if (path != null) {
                if (_isBIP84) {
                    rbf.addKey(input.getOutpoint().toString(), path + "/84");
                } else if (_isBIP49) {
                    rbf.addKey(input.getOutpoint().toString(), path + "/49");
                } else {
                    rbf.addKey(input.getOutpoint().toString(), path);
                }
            } else {
                String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);
            }

        }
        return rbf;
    }

    public static void updateRBFSpendForBroadcastTxAndRegister(
            final RBFSpend rbf,
            final Transaction _tx,
            final String destAddress,
            final int changeType,
            final Context context) {

        final boolean optIn = PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false);
        if (! optIn) return;
        for (final TransactionOutput out : _tx.getOutputs()) {
            try {
                if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(out.getScriptBytes())) && !destAddress.equals(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())))) {
                    rbf.addChangeAddr(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                    debug("SendActivity", "added change output:" + Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                } else if (changeType== 44 && nonNull(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams())) && !destAddress.equals(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                    rbf.addChangeAddr(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                    debug("SendActivity", "added change output:" + out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                } else if (changeType != 44 && nonNull(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams())) && !destAddress.equals(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                    rbf.addChangeAddr(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                    debug("SendActivity", "added change output:" + out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                }
            } catch (final NullPointerException npe) {
                Log.e(TAG, npe.getMessage(), npe);
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        rbf.setHash(_tx.getHashAsString());
        rbf.setSerializedTx(new String(Hex.encode(_tx.bitcoinSerialize())));

        RBFUtil.getInstance().add(rbf);
    }
}
