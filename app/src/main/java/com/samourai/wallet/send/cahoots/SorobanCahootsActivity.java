package com.samourai.wallet.send.cahoots;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.multi.MultiCahoots;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.utxos.UTXOUtil;

import org.spongycastle.util.encoders.Hex;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SorobanCahootsActivity extends SamouraiActivity {

    private SorobanCahootsUi cahootsUi;

    // intent
    private PaymentCode paymentCode;

    private String txNote;

    private Disposable sorobanDisposable;

    public static Intent createIntentSender(
            final Context ctx,
            final int account,
            final CahootsType type,
            final long sendAmount,
            final long fees,
            final String address,
            final String pcode,
            final String destinationPcode,
            final String txNote
    ) {

        final Intent intent = ManualCahootsUi.createIntent(
                ctx,
                SorobanCahootsActivity.class,
                account,
                type,
                CahootsTypeUser.SENDER);

        intent.putExtra("sendAmount", sendAmount);
        intent.putExtra("fees", fees);
        intent.putExtra("sendAddress", address);
        intent.putExtra("pcode", pcode);
        intent.putExtra("tx_note", defaultString(txNote));
        if(destinationPcode != null){
            intent.putExtra("destPcode", destinationPcode);
        }
        return intent;
    }

    public static Intent createIntentCounterparty(
            final Context ctx,
            final int account,
            final CahootsType type,
            final String pcode) {

        final Intent intent = ManualCahootsUi.createIntent(
                ctx,
                SorobanCahootsActivity.class,
                account,
                type,
                CahootsTypeUser.COUNTERPARTY);

        intent.putExtra("pcode", pcode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manual_cahoots);
        setSupportActionBar(findViewById(R.id.toolbar));

        if (getIntent().hasExtra("tx_note")) {
            txNote = getIntent().getStringExtra("tx_note");
        }

        if (nonNull(getSupportActionBar())) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            cahootsUi = new SorobanCahootsUi(
                findViewById(R.id.step_view),
                findViewById(R.id.view_flipper),
                getIntent(),
                getSupportFragmentManager(),
                this
            );
            this.account = cahootsUi.getAccount();
            setTitle(cahootsUi.getTitle(CahootsMode.SOROBAN));

            if (getIntent().hasExtra("pcode")) {
                String pcode = getIntent().getStringExtra("pcode");
                paymentCode = new PaymentCode(pcode);
            }
            if (paymentCode == null) {
                throw new Exception("Invalid paymentCode");
            }

            // start cahoots
            switch(cahootsUi.getTypeUser()) {
                case SENDER:
                    startSender();
                    break;
                case COUNTERPARTY:
                    startReceiver();
                    break;
                default:
                    throw new Exception("Unknown typeUser");
            }
        } catch (Exception e) {
            onCahootsError(e);
            return;
        }
    }

    private void startSender() throws Exception {
        long feePerB = getIntent().getLongExtra("fees", FeeUtil.getInstance().getSuggestedFeeDefaultPerB()) ;
        long sendAmount = getIntent().getLongExtra("sendAmount", 0);
        if (sendAmount <=0) {
            throw new Exception("Invalid sendAmount");
        }
        String sendAddress = getIntent().getStringExtra("sendAddress");
        String paynymDestination = null;
        if(getIntent().hasExtra("destPcode")){
            paynymDestination = getIntent().getStringExtra("destPcode");
        }
        // send cahoots
        subscribeCahoots(cahootsUi.meetAndInitiate(account, feePerB, sendAmount, sendAddress, paynymDestination, paymentCode));
    }

    private void startReceiver() throws Exception {
        subscribeCahoots(cahootsUi.startCounterparty(account, paymentCode));
        Toast.makeText(this, "Waiting for online Cahoots", Toast.LENGTH_SHORT).show();
    }

    private void subscribeCahoots(final Single<Cahoots> onCahoots) {
        sorobanDisposable = onCahoots.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cahoots -> onCahootsSuccess(cahoots), e -> onCahootsError(e));
    }

    private void onCahootsSuccess(final Cahoots cahoots) {

        if(cahoots instanceof MultiCahoots) {

            final MultiCahoots multiCahoots = (MultiCahoots) cahoots;
            if (isNotBlank(txNote)) {

                final String txStowawayHash = multiCahoots.getStowawayTransaction().getHashAsString();
                if (isNotBlank(txStowawayHash)) {
                    UTXOUtil.getInstance().addNote(txStowawayHash, txNote);
                }

                final String txStonewallHash = multiCahoots.getStonewallTransaction().getHashAsString();
                if (isNotBlank(txStonewallHash)) {
                    UTXOUtil.getInstance().addNote(txStonewallHash, txNote);
                }
            }

            System.out.println(Hex.toHexString(multiCahoots.getStowawayTransaction().bitcoinSerialize()));
            System.out.println(Hex.toHexString(multiCahoots.getStonewallTransaction().bitcoinSerialize()));
        }
    }

    private void onCahootsError(Throwable e) {
        Toast.makeText(getApplicationContext(), "Cahoots error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).setIsInForeground(true);
        AppUtil.getInstance(this).checkTimeOut();
    }

    private void clearDisposable() {
        if (sorobanDisposable != null && !sorobanDisposable.isDisposed()) {
            sorobanDisposable.dispose();
            sorobanDisposable = null;
        }
    }

    @Override
    public void finish() {
        clearDisposable();
        super.finish();
    }

    @Override
    public void onBackPressed() {// cancel cahoots request
        clearDisposable();
        super.onBackPressed();
    }
}
