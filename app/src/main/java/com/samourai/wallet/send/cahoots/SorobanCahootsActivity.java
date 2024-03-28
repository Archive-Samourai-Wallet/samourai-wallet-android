package com.samourai.wallet.send.cahoots;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.wallet.SorobanWallet;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.CahootsSorobanInitiatorListener;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.AndroidSorobanWalletService;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.multi.MultiCahoots;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.utxos.UTXOUtil;
import com.samourai.wallet.widgets.CahootsCircleProgress;

import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.Callable;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SorobanCahootsActivity extends SamouraiActivity {

    private SorobanCahootsUi cahootsUi;

    // intent
    private PaymentCode paymentCode;

    private String txNote;

    private Disposable sorobanDisposable;
    private SorobanWallet sorobanWallet;

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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manual_cahoots);
        setSupportActionBar(findViewById(R.id.toolbar));

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
        String paynymDestination = getIntent().hasExtra("destPcode") ?
                getIntent().getStringExtra("destPcode") : null;
        // send cahoots
        subscribeCahoots(() -> doStartInitiator(feePerB, sendAmount, sendAddress, paynymDestination));
    }

    private Cahoots doStartInitiator(long feePerB, long sendAmount, String sendAddress, String paynymDestination) throws Exception {
        // context
        CahootsContext cahootsContext = cahootsUi.computeCahootsContextInitiator(account, feePerB, sendAmount, sendAddress, paynymDestination);
        cahootsUi.checkCahootsContext(cahootsContext);

        // start initiator
        CahootsSorobanInitiatorListener listener = cahootsUi.computeInitiatorListener();
        sorobanWallet = cahootsUi.sorobanWalletService.getSorobanWalletInitiator();
        return ((SorobanWalletInitiator)sorobanWallet).meetAndInitiate(cahootsContext, paymentCode, listener);
    }

    private void startReceiver() throws Exception {
        subscribeCahoots(() -> doStartReceiver());
        Toast.makeText(this, "Waiting for online Cahoots", Toast.LENGTH_SHORT).show();
    }

    public Cahoots doStartReceiver() throws Exception {
        AndroidSorobanWalletService sorobanWalletService = cahootsUi.sorobanWalletService;
        CahootsWallet cahootsWallet = sorobanWalletService.getCahootsWallet();

        // context
        CahootsContext cahootsContext = CahootsContext.newCounterparty(cahootsWallet, cahootsUi.cahootsType, account);
        cahootsUi.checkCahootsContext(cahootsContext);

        // start counterparty
        Consumer<OnlineCahootsMessage> onProgress = cahootsMessage -> cahootsUi.setCahootsMessage(cahootsMessage);
        sorobanWallet = sorobanWalletService.getSorobanWalletCounterparty();
        return ((SorobanWalletCounterparty)sorobanWallet).counterparty(cahootsContext, paymentCode, onProgress);
    }

    private void subscribeCahoots(Callable<Cahoots> runCahoots) {
        try {
            sorobanDisposable = Single.fromCallable(runCahoots)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(cahoots -> onCahootsSuccess(cahoots), e -> onCahootsError(e));
        } catch (Exception e) {
            onCahootsError(e);
        }
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
        if (sorobanWallet != null) {
            sorobanWallet.exit();
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
