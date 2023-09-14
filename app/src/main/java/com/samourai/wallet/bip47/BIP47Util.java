package com.samourai.wallet.bip47;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BIP47Util extends BIP47UtilGeneric {

    private static BIP47Wallet wallet = null;

    private static Context context = null;
    private static BIP47Util instance = null;
    private MutableLiveData<Bitmap> paynymLogo = new MutableLiveData();

    public static BIP47Util getInstance(final Context ctx) {

        context = ctx;

        if (instance == null || wallet == null) {

            try {
                wallet = HD_WalletFactory.getInstance(context).getBIP47();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            } catch (MnemonicException.MnemonicLengthException mle) {
                mle.printStackTrace();
                Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
            }

            instance = new BIP47Util();
        }

        return instance;
    }

    private BIP47Util() {
        super(AndroidSecretPointFactory.getInstance(), true);
    }

    private NetworkParameters getNetworkParams() {
        return SamouraiWallet.getInstance().getCurrentNetworkParams();
    }

    public LiveData<Bitmap> getPayNymLogoLive() {
        return paynymLogo;
    }

    public File avatarImage() {
        File directory = ContextCompat.getDataDir(context);
        return new File(directory.getPath().concat(File.separator).concat("paynym.png"));
    }

    public void reset() {
        instance = new BIP47Util();
        wallet = null;
    }

    public BIP47Wallet getWallet() {
        return wallet;
    }

    public HD_Address getNotificationAddress() {
        return super.getNotificationAddress(wallet);
    }

    public HD_Address getNotificationAddress(int account) {
        return super.getNotificationAddress(wallet, account);
    }

    public PaymentCode getPaymentCode() throws AddressFormatException {
        return super.getPaymentCode(wallet);
    }

    public PaymentCode getPaymentCode(int account) throws AddressFormatException {
        return super.getPaymentCode(wallet, account);
    }

    public PaymentCode getFeaturePaymentCode() throws AddressFormatException {
        return super.getFeaturePaymentCode(wallet);
    }

    public PaymentCode getFeaturePaymentCode(int account) throws AddressFormatException {
        return super.getFeaturePaymentCode(wallet, account);
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getReceiveAddress(wallet, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getReceiveAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getReceiveAddress(wallet, account, pcode, idx, getNetworkParams());
    }

    public String getReceivePubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getReceivePubKey(wallet, pcode, idx, getNetworkParams());
    }

    public String getReceivePubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getReceivePubKey(wallet, account, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return super.getSendAddress(wallet, pcode, idx, getNetworkParams());
    }

    public PaymentAddress getSendAddress(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception {
        return getSendAddress(wallet, account, pcode, idx, getNetworkParams());
    }

    public String getSendPubKey(PaymentCode pcode, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getSendPubKey(wallet, pcode, idx, getNetworkParams());
    }

    public String getSendPubKey(PaymentCode pcode, int account, int idx) throws AddressFormatException, NotSecp256k1Exception, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        return super.getSendPubKey(wallet, account, pcode, idx, getNetworkParams());
    }

    public byte[] getIncomingMask(byte[] pubkey, byte[] outPoint) throws AddressFormatException, Exception {
        return super.getIncomingMask(wallet, pubkey, outPoint, getNetworkParams());
    }

    public byte[] getIncomingMask(byte[] pubkey, int account, byte[] outPoint) throws AddressFormatException, Exception {
        return super.getIncomingMask(wallet, account, pubkey, outPoint, getNetworkParams());
    }

    public PaymentAddress getPaymentAddress(PaymentCode pcode, int idx, HD_Address address) throws AddressFormatException, NotSecp256k1Exception {
        return super.getPaymentAddress(pcode, idx, address, getNetworkParams());
    }

    public void setAvatar(@Nullable Bitmap bitmap) {
        if (bitmap != null) {
            paynymLogo.postValue(bitmap);
        }
    }

    public Completable fetchBotImage() {
        String url = WebUtil.PAYNYM_API + "preview/" + getPaymentCode().toString();
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.PAYNYM_CLAIMED,false)){
            url = WebUtil.PAYNYM_API +  getPaymentCode().toString() + "/avatar";
        }
        String finalUrl = url;
        return Completable.fromCallable(() -> {
                    Request.Builder rb = new Request.Builder().url(finalUrl);
                    OkHttpClient.Builder builder = com.samourai.wallet.util.WebUtil.getInstance(context).httpClientBuilder(finalUrl);
                    OkHttpClient client = builder.build();
                    Response response = client.newCall(rb.build()).execute();
                    if (response.isSuccessful()) {
                        File file = avatarImage();
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        byte[] stream = response.body().bytes();
                        OutputStream outStream = new FileOutputStream(file);
                        outStream.write(stream);
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                        setAvatar(bitmap);
                    }
                    return true;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    synchronized public String getDestinationAddrFromPcode(final String pcodeAsString) throws Exception {

        if (isBlank(pcodeAsString)) return null;

        final String address = getAddress(pcodeAsString, getPaymentAddress(pcodeAsString));

        if (isNull(BIP47Meta.getInstance().getPCode4Addr(address))) {
            return address;
        } else {
            String candidateAddr;
            do {
                BIP47Meta.getInstance().incOutgoingIdx(pcodeAsString);
                candidateAddr = getAddress(pcodeAsString, getPaymentAddress(pcodeAsString));
            } while (nonNull(BIP47Meta.getInstance().getPCode4Addr(candidateAddr)));

            return candidateAddr;
        }
    }

    private static String getAddress(final String pcodeAsString,
                                     final PaymentAddress paymentAddress)
            throws Exception {

        if (BIP47Meta.getInstance().getSegwit(pcodeAsString)) {
            return new SegwitAddress(
                    paymentAddress.getSendECKey(),
                    SamouraiWallet.getInstance().getCurrentNetworkParams()).getBech32AsString();

        } else {
            return paymentAddress.getSendECKey()
                    .toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
        }
    }

    private PaymentAddress getPaymentAddress(final String pcodeAsString)
            throws NotSecp256k1Exception {

        return getInstance(context)
                .getSendAddress(
                        new PaymentCode(pcodeAsString),
                        BIP47Meta.getInstance().getOutgoingIdx(pcodeAsString));
    }
}
