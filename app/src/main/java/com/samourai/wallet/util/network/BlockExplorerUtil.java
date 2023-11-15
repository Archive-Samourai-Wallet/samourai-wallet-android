package com.samourai.wallet.util.network;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.tor.SamouraiTorManager;

public class BlockExplorerUtil {

    private static String strMainNetClearExplorer = "https://m.oxt.me/";
    private static String strMainNetTorExplorer = "http://oxtmblv4v7q5rotqtbbmtbcc5aa5vehr72eiebyamclfo3rco5zm3did.onion/";
    private static String strTestNetClearExplorer = "https://blockstream.info/testnet/";
    private static String strTestNetTorExplorer = "http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion/testnet/";

    private static BlockExplorerUtil instance = null;

    private BlockExplorerUtil() { ; }

    public static BlockExplorerUtil getInstance() {

        if(instance == null) {
            instance = new BlockExplorerUtil();
        }

        return instance;
    }

    public String getUri(boolean isTx) {

        String ret = null;

        // blockstream.info
        if(SamouraiWallet.getInstance().isTestNet())    {
            if(isTorRequired())    {
                ret = strTestNetTorExplorer + (isTx ? "tx/" : "address/");
            }
            else    {
                ret = strTestNetClearExplorer + (isTx ? "tx/" : "address/");
            }
        }
        // oxt.me
        else    {
            if(isTorRequired())    {
                ret = strMainNetTorExplorer + (isTx ? "transaction/" : "address/");
            }
            else    {
                ret = strMainNetClearExplorer + (isTx ? "transaction/" : "address/");
            }
        }

        return ret;
    }

    boolean isTorRequired(){
        return SamouraiTorManager.INSTANCE.isRequired() || SamouraiTorManager.INSTANCE.isConnected();
    }

}
