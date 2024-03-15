package com.samourai.wallet.send.review.broadcast;

import static com.samourai.wallet.send.cahoots.JoinbotHelper.isJoinbotPossibleWithCurrentUserUTXOs;

import android.content.Intent;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.send.cahoots.ManualCahootsActivity;
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity;
import com.samourai.wallet.send.review.ReviewTxModel;

public class SpendJoinbotTxBroadcaster {

    public static final long JOINNBOT_MAX_AMOUNT = 125_000_000L;

    private final ReviewTxModel model;
    private final SamouraiActivity activity;

    private SpendJoinbotTxBroadcaster(final ReviewTxModel model,
                                      final SamouraiActivity activity) {

        this.model = model;
        this.activity = activity;
    }

    public static SpendJoinbotTxBroadcaster create(final ReviewTxModel model,
                                                  final SamouraiActivity activity) {

        return new SpendJoinbotTxBroadcaster(model, activity);
    }

    public SpendJoinbotTxBroadcaster broadcast() {

        if (CahootsMode.MANUAL.equals(model.getSelectedCahootsType().getCahootsMode())) {
            manualCahoots();
            return this;
        }

        if (CahootsMode.SOROBAN.equals(model.getSelectedCahootsType().getCahootsMode())) {
            onlineCahoots();
            return this;
        }

        return this;
    }

    private void onlineCahoots() {
        if (checkValidForJoinbot()) {
            final Intent intent = SorobanCahootsActivity.createIntentSender(
                    activity,
                    model.getAccount(),
                    model.getSelectedCahootsType().getCahootsType(),
                    model.getAmount(),
                    Math.round(model.getMinerFeeRates().getValue()),
                    model.getAddress(),
                    BIP47Meta.getMixingPartnerCode(),
                    BIP47Meta.getInstance().getPcodeFromLabel(model.getAddressLabel()),
                    model.getTxNote().getValue()
            );

            activity.startActivity(intent);
        }
    }

    private void manualCahoots() {

        final Intent intent = ManualCahootsActivity.createIntentSender(
                activity,
                model.getAccount(),
                model.getSelectedCahootsType().getCahootsType(),
                Math.round(model.getMinerFeeRates().getValue()),
                model.getAmount(),
                model.getAddress(),
                BIP47Meta.getInstance().getPcodeFromLabel(model.getAddressLabel()),
                model.getTxNote().getValue()
        );

        activity.startActivity(intent);
    }

    private boolean checkValidForJoinbot() {

        boolean valid = true;

        if (model.getAmount() > JOINNBOT_MAX_AMOUNT) {
            Toast.makeText(
                    activity,
                    activity.getString(R.string.joinbot_max_amount_reached),
                    Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (! isJoinbotPossibleWithCurrentUserUTXOs(
                activity,
                model.isPostmixAccount(),
                model.getAmount(),
                model.getPreselectedUTXOs())) {

            Toast.makeText(
                    activity,
                    activity.getString(R.string.joinbot_not_possible_with_current_utxo),
                    Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;

    }
}
