package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.http.client.IHttpClientService;
import com.samourai.http.client.MockAndroidHttpClientService;
import com.samourai.stomp.client.AndroidStompClient;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.SendFactoryGeneric;
import com.samourai.wallet.tor.ITorManager;
import com.samourai.wallet.tor.MockTorManager;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.data.MockAndroidUtxoSupplier;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import ch.qos.logback.classic.Level;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.schedulers.Schedulers;

public abstract class AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class);

    private static final String SEED_WORDS = "wise never behave tornado tool pear aunt consider season swap custom human";
    private static final String SEED_PASSPHRASE = "test";

    protected Context context = null; //new MockContext(); // TODO sdk>=29 required
    protected HD_WalletFactory hdWalletFactory = HD_WalletFactory.getInstance(context);
    protected NetworkParameters networkParameters;
    protected AndroidWhirlpoolWalletService whirlpoolWalletService;

    protected WhirlpoolWallet whirlpoolWallet;
    private WhirlpoolWalletConfig config;
    protected MockAndroidUtxoSupplier utxoSupplier;

    public void setUp(NetworkParameters networkParameters) throws Exception {
        this.networkParameters = networkParameters;

        ClientUtils.setLogLevel(Level.TRACE, Level.TRACE);

        // mock main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> Schedulers.trampoline());

        // resolve mnemonicCode
        InputStream wis = getClass().getResourceAsStream("/BIP39/en.txt");
        MnemonicCode mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
        hdWalletFactory.__setMnemonicCode(mc);
        wis.close();

        // init Samourai Wallet
        SamouraiWallet.getInstance().setCurrentNetworkParams(networkParameters);

        openWhirlpoolWallet();
    }

    private void openWhirlpoolWallet() throws Exception {
        whirlpoolWalletService = new MockAndroidWhirlpoolWalletService();

        // configure wallet
        boolean onion = false;
        String scode = null;
        ITorManager torManager = new MockTorManager();
        IHttpClientService httpClientService = new MockAndroidHttpClientService(context);
        HD_Wallet bip44w = hdWalletFactory.restoreWallet(SEED_WORDS, SEED_PASSPHRASE);
        config = whirlpoolWalletService.computeWhirlpoolWalletConfig(torManager, true, onion, scode, httpClientService, null);

        whirlpoolWallet = new WhirlpoolWallet(config, bip44w);
        whirlpoolWallet.open();

        utxoSupplier = (MockAndroidUtxoSupplier) whirlpoolWallet.getUtxoSupplier();
    }

    protected UnspentOutput newUnspentOutput(String hash, int n, String xpub, String address, long value, int confirms) throws Exception {
        UnspentOutput utxo = new UnspentOutput();
        utxo.tx_hash = hash;
        utxo.tx_output_n = n;
        utxo.xpub = new UnspentOutput.Xpub();
        utxo.xpub.m = xpub;
        utxo.confirmations = confirms;
        utxo.addr = address;
        utxo.value = value;
        utxo.script = Hex.toHexString(SendFactoryGeneric.getInstance().computeTransactionOutput(address, value, networkParameters).getScriptBytes()); // TODO ?
        return utxo;
    }
}