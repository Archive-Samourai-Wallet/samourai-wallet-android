package com.samourai.wallet.bip47;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.bip47.rpc.AndroidSecretPointFactory;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.samourai.wallet.bip47.BIP47Meta.STATUS_SENT_CFM;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
        return wallet.getAccount(0).getNotificationAddress();
    }

    public PaymentCode getPaymentCode() throws AddressFormatException {
        return wallet.getAccount(0).getPaymentCode();
    }

    public PaymentCode getFeaturePaymentCode() throws AddressFormatException {
        PaymentCode payment_code = getPaymentCode();
        return new com.samourai.wallet.bip47.rpc.PaymentCode(payment_code.makePaymentCodeSamourai());
    }

    public SegwitAddress getReceiveAddress(PaymentCode pcode, int idx) throws Exception {
        return super.getReceiveAddress(wallet.getAccount(0), pcode, idx, getNetworkParams());
    }

    public String getReceivePubKey(PaymentCode pcode, int idx) throws Exception {
        return super.getReceivePubKey(wallet.getAccount(0), pcode, idx, getNetworkParams());
    }

    public SegwitAddress getSendAddress(PaymentCode pcode, int idx) throws Exception {
        return super.getSendAddress(wallet.getAccount(0), pcode, idx, getNetworkParams());
    }

    public String getSendPubKey(PaymentCode pcode, int idx) throws Exception {
        return super.getSendPubKey(wallet.getAccount(0), pcode, idx, getNetworkParams());
    }

    public byte[] getIncomingMask(byte[] pubkey, byte[] outPoint) throws Exception {
        return super.getIncomingMask(wallet.getAccount(0), pubkey, outPoint, getNetworkParams());
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
                    OkHttpClient.Builder builder = com.samourai.wallet.util.network.WebUtil.getInstance(context).httpClientBuilder(finalUrl);
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

    synchronized public String getDestinationAddrFromPcode(
            final String pcodeAsString
    ) throws Exception {

        return getDestinationAddrFromPcode(pcodeAsString, 0);
    }


    synchronized public String getDestinationAddrFromPcode(
            final String pcodeAsString,
            final int indexOffset
    ) throws Exception {

        if (isBlank(pcodeAsString)) return null;
        return getAddress(pcodeAsString, getPaymentAddressSend(pcodeAsString, indexOffset));
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

    private PaymentAddress getPaymentAddressSend(final String pcodeAsString,
                                                 final int indexOffset)
            throws Exception {
        final int idx = indexOffset + BIP47Meta.getInstance().getOutgoingIdx(pcodeAsString);
        final HD_Address address = wallet.getAccount(0).addressAt(idx);
        return getPaymentAddress(
                        new PaymentCode(pcodeAsString),
                        idx,
                        address,
                getNetworkParams());
    }

    public String getSendAddressString(final String pcode) throws Exception {
        final PaymentAddress paymentAddress = getPaymentAddressSend(pcode, 0);
        if (BIP47Meta.getInstance().getSegwit(pcode)) {
            return new SegwitAddress(paymentAddress.getSendECKey(), getNetworkParams())
                    .getBech32AsString();
        } else {
            return paymentAddress.getSendECKey().toAddress(getNetworkParams()).toString();
        }
    }



    public int updateOutgoingStatusForNewPayNymConnections() {

        final APIFactory apiFactory = APIFactory.getInstance(context);
        final BIP47Meta bip47Meta = BIP47Meta.getInstance();

        int updatedCount = 0;
        for (final Pair<String, String> codeToTx : bip47Meta.getOutgoingUnconfirmed()) {
            final int status = BIP47Meta.getInstance().getOutgoingStatus(codeToTx.getLeft());
            if (status != STATUS_SENT_CFM) {
                final int confirmations = apiFactory.getNotifTxConfirmations(codeToTx.getRight());
                if (confirmations > 0) {
                    BIP47Meta.getInstance().setOutgoingStatus(
                            codeToTx.getLeft(),
                            codeToTx.getRight(),
                            STATUS_SENT_CFM);

                    ++updatedCount;
                }
            }
        }
        return updatedCount;
    }
}
