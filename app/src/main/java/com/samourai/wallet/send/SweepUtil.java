package com.samourai.wallet.send;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.send.beans.SweepPreview;
import com.samourai.wallet.service.JobRefreshService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.BackendApiAndroid;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;

import org.bitcoinj.core.Coin;

import java.math.BigInteger;
import java.util.Collection;

public class SweepUtil extends SweepUtilGeneric {
    private static Context context = null;
    private static SweepUtil instance = null;

    private SweepUtil() { super(); }

    public static SweepUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new SweepUtil();
        }

        return instance;
    }

    public void sweep(final PrivKeyReader privKeyReader)  {

        new Thread(() -> {

            Looper.prepare();

            try {
                BigInteger feePerKB = FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB();
                long feePerB = FeeUtil.getInstance().toFeePerB(feePerKB);
                BackendApi backendApi = BackendApiAndroid.getInstance(context);
                Collection<SweepPreview> sweepPreviews = sweepPreviews(privKeyReader, feePerB, backendApi);
                if (sweepPreviews.isEmpty()) {
                    throw new Exception("No utxo found for sweep");
                }

                // sweep each bipFormat
                for (SweepPreview sweepPreview : sweepPreviews) {
                    long amount = sweepPreview.getAmount();
                    String address = sweepPreview.getAddress();
                    long fee = sweepPreview.getFee();
                    String message = "Sweep " + Coin.valueOf(amount).toPlainString() + " from " + address + " (fee:" + Coin.valueOf(fee).toPlainString() + ")?";

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage(message);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                        final ProgressDialog progress = new ProgressDialog(context);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(context.getString(R.string.please_wait_sending));
                        progress.show();

                        try {
                            WALLET_INDEX walletIndex = (PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_SEGWIT, true) == true ? WALLET_INDEX.BIP84_RECEIVE : WALLET_INDEX.BIP44_RECEIVE);
                            String receive_address = AddressFactory.getInstance(context).getAddressAndIncrement(walletIndex).getRight();
                            boolean rbfOptin = PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false);
                            long blockHeight = APIFactory.getInstance(context).getLatestBlockHeight();

                            // sweep
                            sweep(sweepPreview, receive_address, backendApi, BIP_FORMAT.PROVIDER, rbfOptin, blockHeight);

                            // success
                            Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, JobRefreshService.class);
                            intent.putExtra("notifTx", false);
                            intent.putExtra("dragged", false);
                            intent.putExtra("launch", false);
                            JobRefreshService.enqueueWork(context.getApplicationContext(), intent);
                        } catch (Exception e) {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }

                    });
                    builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
                        ;
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            Looper.loop();

        }).start();

    }

}
