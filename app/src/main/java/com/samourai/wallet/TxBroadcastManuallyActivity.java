package com.samourai.wallet;

import static com.samourai.wallet.home.BalanceActivity.ACTION_INTENT;
import static java.util.Objects.isNull;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.widgets.BroadcastManuallyView;

public class TxBroadcastManuallyActivity extends AppCompatActivity {

    private static final int TX_MAX_SIZE = 2148;
    private static final int QR_ALPHANUM_CHAR_LIMIT = TX_MAX_SIZE*2;

    private static final String TAG = "TxBroadcastManuallyAct";

    private BroadcastManuallyView broadcastManuallyView = null;

    private String hexTx;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_broadcast_manually);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.orange_send_ui));

        hexTx = getIntent().getExtras().getString("txHex", null);

        broadcastManuallyView = findViewById(R.id.BroadcastManuallyView);
        broadcastManuallyView.getLeftTopImgBtn().setOnClickListener(view -> finish());
        broadcastManuallyView.getBtnClose().setOnClickListener(view -> applyAndClose());
        broadcastManuallyView.getCopyBtn().setOnClickListener(view -> copyToClipboard());
        broadcastManuallyView.getTransactionImageView().post(() -> {
            final int width = broadcastManuallyView.getTransactionImageView().getWidth();
            broadcastManuallyView.setTransactionBitmap(createTransactionBitmap(hexTx, width));
        });

    }

    private void copyToClipboard() {

        if (isNull(hexTx)) return;

        final android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) TxBroadcastManuallyActivity.this
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TX", hexTx));

        Toast.makeText(
                TxBroadcastManuallyActivity.this,
                R.string.copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }

    private Bitmap createTransactionBitmap(final String hexTx, final int width) {

        if (isNull(hexTx)) return null;

        if (hexTx.length() > QR_ALPHANUM_CHAR_LIMIT) {
            Toast.makeText(
                    TxBroadcastManuallyActivity.this,
                    R.string.tx_too_large_qr,
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            return new QRCodeEncoder(
                    hexTx,
                    null,
                    Contents.Type.TEXT,
                    BarcodeFormat.QR_CODE.toString(),
                    width
            ).encodeAsBitmap();

        } catch (final WriterException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private void applyAndClose() {
        if (broadcastManuallyView.doNotSpendChecked()) {
            markUTXOAsNonSpendable();
        }
        gotoBalanceHomeActivity();
    }

    private void markUTXOAsNonSpendable() {
        UTXOFactory.getInstance(TxBroadcastManuallyActivity.this).markUTXOAsNonSpendable(
                hexTx,
                SendParams.getInstance().getAccount());
        final Intent intent = new Intent(ACTION_INTENT);
        intent.putExtra("notifTx", false);
        intent.putExtra("fetch", true);
        LocalBroadcastManager.getInstance(TxBroadcastManuallyActivity.this).sendBroadcast(intent);
    }

    private void gotoBalanceHomeActivity() {

        if (SendParams.getInstance().getAccount() != 0) {

            final Intent balanceHome = new Intent(this, BalanceActivity.class);
            balanceHome.putExtra("_account", SendParams.getInstance().getAccount());
            balanceHome.putExtra("refresh", true);
            final Intent parentIntent = new Intent(this, BalanceActivity.class);
            parentIntent.putExtra("_account", 0);
            balanceHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder.create(getApplicationContext())
                    .addNextIntent(parentIntent)
                    .addNextIntent(balanceHome)
                    .startActivities();

        } else {
            final Intent _intent = new Intent(TxBroadcastManuallyActivity.this, BalanceActivity.class);
            _intent.putExtra("refresh", true);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(_intent);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
