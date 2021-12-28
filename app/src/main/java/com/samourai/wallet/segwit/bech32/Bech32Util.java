package com.samourai.wallet.segwit.bech32;

import com.samourai.wallet.SamouraiWallet;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.encoders.Hex;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

public class Bech32Util {

    private static Bech32Util instance = null;

    private Bech32Util() { ; }

    public static Bech32Util getInstance() {

        if(instance == null) {
            instance = new Bech32Util();
        }

        return instance;
    }

    public boolean isBech32Script(String script) {
        return isP2WPKHScript(script) || isP2WSHScript(script) || isP2TRScript(script);
    }

    public boolean isP2WPKHScript(String script) {
        return script.startsWith("0014") && script.length() == (20 * 2 + 2 * 2);
    }

    public boolean isP2WSHScript(String script) {
        return script.startsWith("0020") && script.length() == (32 * 2 + 2 * 2);
    }

    public boolean isP2TRScript(String script) {
        return script.startsWith("5120") && script.length() == (32 * 2 + 2 * 2);
    }

    public String getAddressFromScript(String script) throws Exception    {
        Script _script = new Script(Hex.decode(script));
        return getAddressFromScript(_script);
    }

    public String getAddressFromScript(Script script) throws Exception    {

        String hrp = null;
        if(SamouraiWallet.getInstance().getCurrentNetworkParams() instanceof TestNet3Params)    {
            hrp = "tb";
        }
        else    {
            hrp = "bc";
        }

        byte[] buf = script.getProgram();
        byte[] scriptBytes = new byte[buf.length - 2];
        System.arraycopy(buf, 2, scriptBytes, 0, scriptBytes.length);

        byte ver = (byte)0x00;
        if(buf[0] == (byte)0x51)    {
            ver = 0x01;
        }

        return Bech32Segwit.encode(hrp, ver, scriptBytes);
    }

    public TransactionOutput getTransactionOutput(String address, long value) throws Exception    {

        TransactionOutput output = null;

        if(address.toLowerCase().startsWith("tb") || address.toLowerCase().startsWith("bc"))   {

            byte[] scriptPubKey = null;

            try {
                Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", address);
                scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
            }
            catch(Exception e) {
                return null;
            }
            output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(value), scriptPubKey);
        }

        return output;
    }

}
