package com.samourai.wallet.send.review;

import static com.samourai.wallet.send.review.EnumSendType.SPEND_SIMPLE;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.TxAnimUIActivity;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.BatchSendUtil;
import com.samourai.wallet.util.func.FormatsUtil;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;

public class SpendBatchTxBroadcaster {

    private final ReviewTxModel model;
    private final SamouraiActivity activity;

    private SpendBatchTxBroadcaster(final ReviewTxModel model,
                                    final SamouraiActivity activity) {

        this.model = model;
        this.activity = activity;
    }

    public static SpendBatchTxBroadcaster create(final ReviewTxModel model,
                                                 final SamouraiActivity activity) {

        return new SpendBatchTxBroadcaster(model, activity);
    }

    public SpendBatchTxBroadcaster broadcast() {

        final List<MyTransactionOutPoint> outPoints = Lists.newArrayList();
        for (final UTXO u : model.getTxData().getSelectedUTXO()) {
            outPoints.addAll(u.getOutpoints());
        }

        final String changeAddress = SendParams.generateChangeAddress(
                activity,
                model.getTxData().getChange(),
                SPEND_SIMPLE.getType(),
                model.getAccount(),
                model.getChangeType(),
                getChangeIndex(),
                true);

        if (nonNull(changeAddress)) {
            model.getTxData().getReceivers().put(
                    changeAddress,
                    BigInteger.valueOf(model.getTxData().getChange()));
        }

        SendParams.getInstance().setParams(
                outPoints,
                model.getTxData().getReceivers(),
                ImmutableList.copyOf(BatchSendUtil.getInstance().getSends()),
                SPEND_SIMPLE.getType(),
                model.getTxData().getChange(),
                model.getChangeType(),
                model.getAccount(),
                StringUtils.EMPTY,
                false,
                false,
                model.getAmount(),
                getChangeIndex(),
                model.getTxNote().getValue()
        );

        BatchSendUtil.getInstance().getSends().clear();
        activity.startActivity(new Intent(activity, TxAnimUIActivity.class));
        return this;
    }

    public int getChangeIndex() {
        final int changeType = model.getChangeType();
        if (model.isPostmixAccount()) {
            return model.getTxData().getIdxBIP84PostMixInternal();
        } else if (changeType == 84) {
            return model.getTxData().getIdxBIP84Internal();
        } else if (changeType == 49) {
            return model.getTxData().getIdxBIP49Internal();
        } else {
            return model.getTxData().getIdxBIP44Internal();
        }
    }
}
