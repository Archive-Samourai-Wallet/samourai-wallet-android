package com.samourai.wallet.send.cahoots;

import static com.samourai.wallet.send.SendActivity.stubAddress;
import static com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState.DONE;
import static com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState.IS_SWIPING_ENABLED;
import static com.samourai.wallet.util.tech.ColorUtil.getAttributeColor;
import static com.samourai.wallet.util.tech.ColorUtil.lightenColor;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;

import com.samourai.boltzmann.beans.BoltzmannSettings;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.processor.TxProcessor;
import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.api.backend.IPushTx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.multi.MultiCahoots;
import com.samourai.wallet.cahoots.stowaway.Stowaway;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonFragment;
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener;
import com.samourai.wallet.widgets.EntropyBar;

import org.bitcoinj.core.TransactionOutput;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CahootReviewFragment extends Fragment {

    private static final String TAG = "CahootReviewFragment";
    TextView toAddress, amountInBtc, amountInSats, feeInBtc, feeInSats, entropyBits, step, samouraiFeeBtc, samouraiFeeSats, samouraiFeeLabel;
    EntropyBar entropyBar;
    ViewGroup cahootsEntropyGroup, cahootsSamouraiFeeGroup;
    Group cahootsProgressGroup;
    View cahootsSamouraiFeeGroupDivider, cahootsSamouraiEntropyGroupDivider;
    private Cahoots payload;
    private Callable onBroadcast;
    private CompositeDisposable disposables = new CompositeDisposable();

    private MutableLiveData<Long> amountToLeaveWallet = new MutableLiveData<>(0L);
    private MutableLiveData<Boolean> sendBtnEnable = new MutableLiveData<>(true);

    public static CahootReviewFragment newInstance(final Intent intent) {
        final CahootReviewFragment fragment = new CahootReviewFragment();
        fragment.setArguments(intent.getExtras());
        return fragment;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        showPayloadInfo();
    }

    private void onSuccessfulBroadcast() {
        // increment paynym index if destination

        if (nonNull(getArguments())) {

            if (getArguments().containsKey("typeUser")) {
                final int typeUserInt = getArguments().getInt("typeUser", -1);
                final CahootsTypeUser typeUser = CahootsTypeUser.find(typeUserInt).get();
                if (typeUser == CahootsTypeUser.SENDER) {
                    String paynymDestination = payload.getPaynymDestination();
                    if (getArguments().containsKey("destPcode")) {
                        paynymDestination = getArguments().getString("destPcode");
                    }
                    if (isNotBlank(paynymDestination)) {
                        final String sendAddress = getArguments().getString("sendAddress");
                        if (isNotBlank(sendAddress)) {
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(sendAddress, paynymDestination);
                        }
                        BIP47Meta.getInstance().incOutgoingIdx(paynymDestination);
                    }
                }
            }
        }


        getActivity().runOnUiThread(() -> {
            Toast.makeText(getActivity(), R.string.tx_sent, Toast.LENGTH_SHORT).show();
        });
//        // notify
        if (onBroadcast != null) {
            try {
                onBroadcast.call();
            } catch (Exception e) {
            }
        }
    }

    private void showError() {
        Toast.makeText(this.getActivity(), "Error broadcasting tx", Toast.LENGTH_SHORT).show();
        getActivity().runOnUiThread(() -> {
            cahootsProgressGroup.setVisibility(View.GONE);
            sendBtnEnable.postValue(true);
        });
    }

    private void calculateEntropy() {

        CalculateEntropy(payload)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TxProcessorResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(TxProcessorResult entropyResult) {
                        cahootsEntropyGroup.setVisibility(View.VISIBLE);
                        cahootsSamouraiEntropyGroupDivider.setVisibility(View.VISIBLE);
                        entropyBar.post(() ->
                                entropyBar.setRange(entropyResult)
                        );
                        DecimalFormat decimalFormat = new DecimalFormat("##.00");
                        entropyBits.setText(decimalFormat.format(entropyResult.getEntropy()).concat(" bits"));
                    }

                    @Override
                    public void onError(Throwable e) {
                        entropyBar.disable();
                        entropyBits.setText("0.0 bits");
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void showPayloadInfo() {
        if (nonNull(payload)) {
            if (payload instanceof MultiCahoots) {
                MultiCahoots multiCahootsPayload = (MultiCahoots) payload;
                long halfOfStonewallFee = (multiCahootsPayload.getStonewallx2().getFeeAmount()/2L);
                long totalMinerFee = multiCahootsPayload.getStowaway().getFeeAmount() + halfOfStonewallFee; // stowaway tx fee + our half of stonewall miner fee we pay
                long serviceFee = multiCahootsPayload.getStowaway().getSpendAmount(); // 3.5% + other half of stonewall miner fee that we also pay
                amountToLeaveWallet.postValue(multiCahootsPayload.getSpendAmount() + totalMinerFee + serviceFee);
            } else {
                amountToLeaveWallet.postValue(payload.getSpendAmount() + payload.getFeeAmount());
            }
            toAddress.setText(payload.getDestination());
            amountInBtc.setText(formatForBtc(payload.getSpendAmount()));
            amountInSats.setText(String.valueOf(payload.getSpendAmount()).concat(" sat"));
            if ((payload.getFeeAmount() == 0)) {
                feeInBtc.setText("__");
                feeInSats.setText("__");
            } else {
                if (payload instanceof MultiCahoots) {
                    MultiCahoots multiCahootsPayload = (MultiCahoots) payload;
                    long halfOfStonewallFee = (multiCahootsPayload.getStonewallx2().getFeeAmount()/2L);
                    long totalMinerFee = multiCahootsPayload.getStowaway().getFeeAmount() + halfOfStonewallFee; // stowaway tx fee + our half of stonewall miner fee we pay
                    long serviceFee = multiCahootsPayload.getStowaway().getSpendAmount(); // 3.5% + other half of stonewall miner fee that we also pay
                    cahootsSamouraiFeeGroup.setVisibility(View.VISIBLE);
                    cahootsSamouraiFeeGroupDivider.setVisibility(View.VISIBLE);
                    samouraiFeeBtc.setText(formatForBtc(serviceFee));
                    samouraiFeeSats.setText(String.valueOf(serviceFee).concat(" sat"));
                    feeInBtc.setText(formatForBtc(totalMinerFee));
                    feeInSats.setText(String.valueOf(totalMinerFee).concat(" sat"));
                } else {
                    cahootsSamouraiFeeGroup.setVisibility(View.GONE);
                    cahootsSamouraiFeeGroupDivider.setVisibility(View.GONE);
                    feeInBtc.setText(formatForBtc(payload.getFeeAmount()));
                    feeInSats.setText(String.valueOf(payload.getFeeAmount()).concat(" sat"));
                }
            }
            if (payload instanceof Stowaway) {
                cahootsEntropyGroup.setVisibility(View.GONE);
                cahootsSamouraiEntropyGroupDivider.setVisibility(View.GONE);
            } else {
                calculateEntropy();
            }
            step.setText("Step " + (payload.getStep() + 1));
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.cahoots_broadcast_details, container, false);

        toAddress = view.findViewById(R.id.cahoots_review_address);
        amountInBtc = view.findViewById(R.id.cahoots_review_amount);
        amountInSats = view.findViewById(R.id.cahoots_review_amount_sats);
        feeInBtc = view.findViewById(R.id.cahoots_review_fee);
        entropyBits = view.findViewById(R.id.cahoots_entropy_value);
        entropyBar = view.findViewById(R.id.cahoots_entropy_bar);
        feeInSats = view.findViewById(R.id.cahoots_review_fee_sats);
        cahootsEntropyGroup = view.findViewById(R.id.cahoots_entropy_group);
        cahootsSamouraiFeeGroupDivider = view.findViewById(R.id.cahoots_samourai_fee_group_divider);
        cahootsSamouraiEntropyGroupDivider = view.findViewById(R.id.cahoots_entropy_group_divider);
        cahootsSamouraiFeeGroup = view.findViewById(R.id.cahoots_samourai_fee_group);
        cahootsProgressGroup = view.findViewById(R.id.cahoots_progress_group);
        step = view.findViewById(R.id.textView56);
        samouraiFeeBtc = view.findViewById(R.id.cahoots_review_fee_samourai);
        samouraiFeeSats = view.findViewById(R.id.cahoots_review_fee_samourai_sats);

        final CardView broadcastDetailsCard = view.findViewById(R.id.cahoots_broadcast_details_card);
        final int cardDefaultBckg = broadcastDetailsCard.getCardBackgroundColor().getDefaultColor();

        final SamouraiActivity activity = (SamouraiActivity) getActivity();
        final int statusBarColor = activity.getStatusBarColor();
        final int navigationBarColor = activity.getNavigationBarColor();

        final View headView = activity.findViewById(R.id.appBarLayout2);
        final ColorStateList headViewBckgTintList = headView.getBackgroundTintList();
        final View toolbar = activity.findViewById(R.id.toolbar);
        final ColorStateList toolbarBckgTintList = toolbar.getBackgroundTintList();
        final View stepView = activity.findViewById(R.id.step_view);
        final ColorStateList stepViewBckgTintList = toolbar.getBackgroundTintList();

        final int windowBackground = getAttributeColor(getContext(), android.R.attr.windowBackground);

        final FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.compose_send_view, new SwipeSendButtonFragment(
                amountToLeaveWallet,
                () -> sentAction(),
                sendBtnEnable,
                new SwipeSendButtonListener() {
                    @Override
                    public void onStateChange(final EnumSwipeSendButtonState state) {

                        getActivity().runOnUiThread(() -> {

                            final boolean lightenColor =
                                    state == IS_SWIPING_ENABLED ||
                                    state == DONE;
                            final float alpha = lightenColor ? 0.1f : 0f;

                            broadcastDetailsCard.setBackgroundColor(lightenColor(cardDefaultBckg, alpha));
                            if (lightenColor) {
                                final int[][] states = new int[][] {new int[] {}};
                                final int[] colors = new int[] {lightenColor(headViewBckgTintList.getDefaultColor(), alpha)};
                                headView.setBackgroundTintList(new ColorStateList(states, colors));
                                toolbar.setBackgroundTintList(new ColorStateList(states, colors));
                                stepView.setBackgroundTintList(new ColorStateList(states, colors));
                            } else {
                                headView.setBackgroundTintList(headViewBckgTintList);
                                toolbar.setBackgroundTintList(headViewBckgTintList);
                                stepView.setBackgroundTintList(headViewBckgTintList);
                            }

                            view.setBackgroundColor(lightenColor(windowBackground, alpha));
                            activity.setNavigationBarColor(lightenColor(navigationBarColor, alpha));
                            activity.setStatusBarColor(lightenColor(statusBarColor, alpha));

                        });
                    }
                }));
        fragmentTransaction.commit();

        return view;
    }

    private void sentAction() {
        if (nonNull(payload)) {
            sendBtnEnable.postValue(false);
            cahootsProgressGroup.setVisibility(View.VISIBLE);
            new Thread(() -> {
                Looper.prepare();

                try {
                    IPushTx pushTx = PushTx.getInstance(getActivity());
                    payload.pushTx(pushTx);
                    onSuccessfulBroadcast();
                } catch (Exception e) {
                    showError();
                    e.printStackTrace();
                }

                Looper.loop();

            }).start();
        }
    }

    public void setCahoots(Cahoots payload) {
        this.payload = payload;
        if (isAdded()) {
            showPayloadInfo();
        }
    }

    public void setOnBroadcast(Callable onBroadcast) {
        this.onBroadcast = onBroadcast;
    }

    private String formatForBtc(Long amount) {
        return (String.format(Locale.ENGLISH, "%.8f", getBtcValue((double) amount)).concat(" BTC"));
    }

    private String formatForSats(Long amount) {
        return formattedSatValue(getBtcValue((double) amount)).concat(" sat");
    }

    private Double getBtcValue(Double sats) {
        return (sats / 1e8);
    }

    private String formattedSatValue(Object number) {
        NumberFormat nformat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat) nformat;
        decimalFormat.applyPattern("#,###");
        return decimalFormat.format(number).replace(",", " ");
    }

    private Observable<TxProcessorResult> CalculateEntropy(Cahoots payload) {

        return Observable.create(emitter -> {

            Map<String, Long> inputs = new HashMap<>();
            Map<String, Long> outputs = new HashMap<>();

            int counter = 0;
            for (Map.Entry<String, Long> entry : payload.getOutpoints().entrySet()) {
                inputs.put(stubAddress[counter], entry.getValue());
                counter++;
            }

            for (int i = 0; i < payload.getTransaction().getOutputs().size(); i++) {
                TransactionOutput output = payload.getTransaction().getOutputs().get(i);
                outputs.put(stubAddress[counter + i], output.getValue().value);

            }

            TxProcessor txProcessor = new TxProcessor(BoltzmannSettings.MAX_DURATION_DEFAULT, BoltzmannSettings.MAX_TXOS_DEFAULT);
            Txos txos = new Txos(inputs, outputs);
            TxProcessorResult result = txProcessor.processTx(txos, 0.005f, TxosLinkerOptionEnum.PRECHECK, TxosLinkerOptionEnum.LINKABILITY, TxosLinkerOptionEnum.MERGE_INPUTS);
            emitter.onNext(result);
        });

    }

    @Override
    public void onDestroy() {
        // notify Soroban partner
        super.onDestroy();
        disposables.dispose();
    }
}
