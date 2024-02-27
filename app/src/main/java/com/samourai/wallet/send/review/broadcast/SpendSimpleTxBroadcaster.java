package com.samourai.wallet.send.review.broadcast;

import static java.util.Objects.nonNull;

import android.content.Intent;

import com.google.common.collect.Lists;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.TxAnimUIActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.review.ReviewTxModel;
import com.samourai.wallet.util.func.SendAddressUtil;

import java.math.BigInteger;
import java.util.List;

public class SpendSimpleTxBroadcaster {

    private final ReviewTxModel model;
    private final SamouraiActivity activity;

    private SpendSimpleTxBroadcaster(final ReviewTxModel model,
                                     final SamouraiActivity activity) {

        this.model = model;
        this.activity = activity;
    }

    public static SpendSimpleTxBroadcaster create(final ReviewTxModel model,
                                                  final SamouraiActivity activity) {

        return new SpendSimpleTxBroadcaster(model, activity);
    }

    public SpendSimpleTxBroadcaster broadcast() {

        final List<MyTransactionOutPoint> outPoints = Lists.newArrayList();
        for (final UTXO u : model.getTxData().getValue().getSelectedUTXO()) {
            outPoints.addAll(u.getOutpoints());
        }

        final String changeAddress = SendParams.generateChangeAddress(
                activity,
                model.getTxData().getValue().getChange(),
                model.getSendType().getType(),
                model.getAccount(),
                model.getChangeType(),
                getChangeIndex(),
                true);

        if (nonNull(changeAddress)) {
            model.getTxData().getValue().getReceivers().put(
                    changeAddress,
                    BigInteger.valueOf(model.getTxData().getValue().getChange()));
        }

        SendParams.getInstance().setParams(
                outPoints,
                model.getTxData().getValue().getReceivers(),
                BIP47Meta.getInstance().getPcodeFromLabel(model.getAddressLabel()),
                model.getSendType().getType(),
                model.getTxData().getValue().getChange(),
                model.getChangeType(),
                model.getAccount(),
                model.getAddress(),
                SendAddressUtil.getInstance().get(model.getAddress()) == 1,
                false,
                model.getAmount(),
                getChangeIndex(),
                model.getTxNote().getValue()
        );

        activity.startActivity(new Intent(activity, TxAnimUIActivity.class));


        return this;
    }

    public int getChangeIndex() {
        final int changeType = model.getChangeType();
        if (model.isPostmixAccount()) {
            return model.getTxData().getValue().getIdxBIP84PostMixInternal();
        } else if (changeType == 84) {
            return model.getTxData().getValue().getIdxBIP84Internal();
        } else if (changeType == 49) {
            return model.getTxData().getValue().getIdxBIP49Internal();
        } else {
            return model.getTxData().getValue().getIdxBIP44Internal();
        }
    }
}
