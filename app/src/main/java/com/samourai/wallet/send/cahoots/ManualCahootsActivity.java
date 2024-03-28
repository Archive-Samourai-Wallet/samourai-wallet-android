package com.samourai.wallet.send.cahoots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.cahoots.AndroidSorobanWalletService;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.TxBroadcastInteraction;
import com.samourai.wallet.cahoots.manual.ManualCahootsMessage;
import com.samourai.wallet.cahoots.manual.ManualCahootsService;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.sorobanClient.SorobanReply;
import com.samourai.wallet.util.QRBottomSheetDialog;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.widgets.CahootsCircleProgress;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class ManualCahootsActivity extends SamouraiActivity {

    private ManualCahootsUi cahootsUi;
    private CahootsContext cahootsContext;

    public static Intent createIntentResume(Context ctx, int account, String payload) throws Exception {
        ManualCahootsService manualCahootsService = AndroidSorobanWalletService.getInstance(ctx).getManualCahootsService();
        ManualCahootsMessage msg = manualCahootsService.parse(payload);
        CahootsTypeUser typeUser = msg.getTypeUser().getPartner();
        Intent intent = ManualCahootsUi.createIntent(ctx, ManualCahootsActivity.class, account, msg.getType(), typeUser);
        intent.putExtra("payload", payload);
        return intent;
    }

    public static Intent createIntentSender(
            final Context ctx,
            final int account,
            final CahootsType type,
            final long fees,
            final long amount,
            final String address,
            final String destinationPcode,
            final String txNote
    ) {

        final Intent intent = ManualCahootsUi.createIntent(
                ctx,
                ManualCahootsActivity.class,
                account,
                type,
                CahootsTypeUser.SENDER);

        intent.putExtra("sendAmount", amount);
        intent.putExtra("fees", fees);
        intent.putExtra("sendAddress", address);
        intent.putExtra("tx_note", txNote);

        if(destinationPcode != null) {
            intent.putExtra("destPcode", destinationPcode);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_cahoots);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final int account = getIntent().getIntExtra("_account", 0);

        if (account == SamouraiAccountIndex.POSTMIX) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.postmix_spending_blue_color));
            final AppBarLayout viewById = findViewById(R.id.appBarLayout2);
            viewById.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.postmix_spending_blue_color));
            final Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setBackgroundColor(getResources().getColor(R.color.postmix_spending_blue_color));
            final CahootsCircleProgress stepView = findViewById(R.id.step_view);
            stepView.setBackgroundColor(getResources().getColor(R.color.postmix_spending_blue_color));
        }

        try {
            ManualCahootsStepFragment.CahootsFragmentListener listener = new ManualCahootsStepFragment.CahootsFragmentListener() {
                @Override
                public void onScan(int step, String qrData) {
                    onScanCahootsPayload(qrData);
                }

                @Override
                public void onShare(int step) {
                    try {
                        shareCahootsPayload();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            };
            cahootsUi = new ManualCahootsUi(
                    findViewById(R.id.step_view),
                    findViewById(R.id.view_flipper),
                    getIntent(),
                    getSupportFragmentManager(),
                    i -> ManualCahootsStepFragment.newInstance(i, listener),
                    this
            );
            this.account = cahootsUi.getAccount();
            setTitle(cahootsUi.getTitle(CahootsMode.MANUAL));

            if (getIntent().hasExtra("payload")) {
                // resume cahoots
                String cahootsPayload = getIntent().getStringExtra("payload");
                onScanCahootsPayload(cahootsPayload);
            } else {
                // start cahoots
                startSender();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startSender() throws Exception {
        long feePerB =   getIntent().getLongExtra("fees", FeeUtil.getInstance().getSuggestedFeeDefaultPerB()) ;
        long sendAmount = getIntent().getLongExtra("sendAmount", 0);
        if (sendAmount <= 0) {
            throw new Exception("Invalid sendAmount");
        }
        String sendAddress = getIntent().getStringExtra("sendAddress");
        ManualCahootsService manualCahootsService = cahootsUi.getManualCahootsService();
        String paynymDestination = null; // TODO Sarath set destination pCode
        if(getIntent().hasExtra("destPcode")){
            paynymDestination = getIntent().getStringExtra("destPcode");
        }
        cahootsContext = cahootsUi.computeCahootsContextInitiator(account, feePerB, sendAmount, sendAddress, paynymDestination);
        cahootsUi.setCahootsMessage(manualCahootsService.initiate(cahootsContext));
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).setIsInForeground(true);

        AppUtil.getInstance(this).checkTimeOut();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_cahoots_menu, menu);

        menu.findItem(R.id.action_menu_display_psbt).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }

        if (menuItem.getItemId() == R.id.action_menu_paste_cahoots) {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                onScanCahootsPayload(item.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            }
        } else if (menuItem.getItemId() == R.id.action_menu_display_psbt) {
            doDisplayPSBT();
        }
        return true;
    }

    private void onScanCahootsPayload(String qrData) {
        try {
            // continue cahoots
            ManualCahootsService manualCahootsService = cahootsUi.getManualCahootsService();
            ManualCahootsMessage cahootsMessage = manualCahootsService.parse(qrData);

            if (cahootsContext == null) {
                // start as counterparty
                CahootsWallet cahootsWallet = cahootsUi.getCahootsWallet();
                cahootsContext = CahootsContext.newCounterparty(cahootsWallet, cahootsMessage.getType(), account);
            }

            SorobanReply reply = manualCahootsService.reply(cahootsContext, cahootsMessage);
            if (reply instanceof ManualCahootsMessage) {
                cahootsUi.setCahootsMessage((ManualCahootsMessage) reply);
            } else if (reply instanceof TxBroadcastInteraction) {
                cahootsUi.setInteraction((TxBroadcastInteraction) reply);
            } else {
                throw new Exception("Unknown SorobanReply: " + reply.getClass());
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    private void shareCahootsPayload() throws Exception {
        String strCahoots = this.cahootsUi.getCahootsMessage().toPayload();
        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148
        if(strCahoots==null){
            return;
        }
        if (strCahoots != null && strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT) {
        } else {
            Toast.makeText(getApplicationContext(), R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();
        }

        if (!(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)) {
            Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
            txtIntent.setType("text/plain");
            txtIntent.putExtra(android.content.Intent.EXTRA_TEXT, strCahoots);
            startActivity(Intent.createChooser(txtIntent, "Share"));
            return;
        }
        QRBottomSheetDialog dialog = new QRBottomSheetDialog(
              strCahoots,
             "Share",
                "payload"

        );

        dialog.show(getSupportFragmentManager(), dialog.getTag());

    }

    private void doDisplayPSBT() {

        try {
            PSBT psbt = cahootsUi.getCahootsMessage().getCahoots().getPSBT();
            if (psbt == null) {
                Toast.makeText(ManualCahootsActivity.this, R.string.psbt_error, Toast.LENGTH_SHORT).show();
            }

            String strPSBT = psbt.toString();

            final TextView tvHexTx = new TextView(ManualCahootsActivity.this);
            float scale = getResources().getDisplayMetrics().density;
            tvHexTx.setSingleLine(false);
            tvHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            tvHexTx.setLines(10);
            tvHexTx.setGravity(Gravity.START);
            tvHexTx.setText(strPSBT);
            tvHexTx.setPadding((int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f));

            MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(ManualCahootsActivity.this)
                    .setTitle(R.string.app_name)
                    .setView(tvHexTx)
                    .setCancelable(false)
                    .setPositiveButton(R.string.copy_to_clipboard, (dialog, whichButton) -> {
                        dialog.dismiss();
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = null;
                        clip = ClipData.newPlainText("tx", strPSBT);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(ManualCahootsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    });
            if(!isFinishing())    {
                dlg.show();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

}
