package com.samourai.wallet.send.review.ref;

import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.BATCH_SPEND;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.CUSTOM;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.CUSTOM_BATCH_SPEND;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.RICOCHET;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.RICOCHET_CUSTOM;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.SIMPLE;
import static com.samourai.wallet.send.review.ref.EnumCoinSelectionType.STONEWALL;
import static java.util.Objects.nonNull;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.send.review.ReviewTxModel;
import com.samourai.wallet.send.review.TxData;
import com.samourai.wallet.send.review.broadcast.SpendBatchTxBroadcaster;
import com.samourai.wallet.send.review.broadcast.SpendJoinbotTxBroadcaster;
import com.samourai.wallet.send.review.broadcast.SpendRicochetTxBroadcaster;
import com.samourai.wallet.send.review.broadcast.SpendSimpleTxBroadcaster;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public enum EnumSendType {
    SPEND_SIMPLE(0, "Simple", SIMPLE, SIMPLE) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendSimpleTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildSimpleTxData());
        }
    },
    SPEND_CUSTOM(0, "Custom", CUSTOM, CUSTOM) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendSimpleTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildSimpleTxData());
        }
    },
    SPEND_BOLTZMANN(1, "STONEWALL", STONEWALL, STONEWALL) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendSimpleTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildBoltzmannTxData());
        }
    },
    SPEND_RICOCHET(2, "Ricochet", RICOCHET, SIMPLE) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendRicochetTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildRicochetTxData());
        }
    },
    SPEND_RICOCHET_CUSTOM(2, "Custom ricochet", RICOCHET_CUSTOM, CUSTOM) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendRicochetTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildRicochetTxData());
        }
    },
    SPEND_JOINBOT(3, "Joinbot", null, null) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendJoinbotTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildJoinbotTxData());
        }
    },
    SPEND_BATCH(4, "Batch", BATCH_SPEND, SIMPLE) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendBatchTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildSpendBatchTxData());
        }
    },
    SPEND_CUSTOM_BATCH(4, "Custom batch", CUSTOM_BATCH_SPEND, CUSTOM) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendBatchTxBroadcaster.create(buildTx(model), act).broadcast();
            return this;
        }

        @Override
        public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
            return super.buildTx(model.buildSpendBatchTxData());
        }
    },
    ;

    private static final ListMultimap<Integer, EnumSendType> CACHE_TYPE_TO_ENUM = createCacheTypeToEnum();
    private static final Map<EnumCoinSelectionType, EnumSendType> CACHE_SELECTION_TO_ENUM = createCacheSelectionToEnum();

    private static Map<EnumCoinSelectionType, EnumSendType> createCacheSelectionToEnum() {
        final ImmutableMap.Builder<EnumCoinSelectionType, EnumSendType> builder = ImmutableMap.builder();
        for (final EnumSendType sendType : EnumSendType.values()) {
            final EnumCoinSelectionType selectionType = sendType.getCoinSelectionType();
            if (nonNull(selectionType)) {
                builder.put(selectionType, sendType);
            }
        }
        return builder.build();
    }

    @NonNull
    private static ListMultimap<Integer, EnumSendType> createCacheTypeToEnum() {
        final ImmutableListMultimap.Builder<Integer, EnumSendType> builder = ImmutableListMultimap.builder();
        for (final EnumSendType sendType : EnumSendType.values()) {
            builder.put(sendType.getType(), sendType);
        }
        return builder.build();
    }

    private final int type;
    private final String caption;
    private final EnumCoinSelectionType coinSelectionType;
    private final EnumCoinSelectionType coinSelectionTypeView;

    EnumSendType(final int type,
                 final String caption,
                 final EnumCoinSelectionType coinSelectionType,
                 final EnumCoinSelectionType coinSelectionTypeView
    ) {

        this.type = type;
        this.caption = caption;
        this.coinSelectionType = coinSelectionType;
        this.coinSelectionTypeView = coinSelectionTypeView;
    }

    public int getType() {
        return type;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isCustomSelection() {
        return nonNull(coinSelectionType) ? coinSelectionType.isCustomSelection() : false;
    }

    public boolean isBatchSpend() {
        return this == EnumSendType.SPEND_BATCH || this == EnumSendType.SPEND_CUSTOM_BATCH;
    }

    public boolean isJoinbot() {
        return this == EnumSendType.SPEND_JOINBOT;
    }

    public boolean isRicochet() {
        return this == EnumSendType.SPEND_RICOCHET || this == EnumSendType.SPEND_RICOCHET_CUSTOM;
    }

    public EnumCoinSelectionType getCoinSelectionType() {
        return coinSelectionType;
    }

    public EnumCoinSelectionType getCoinSelectionTypeView() {
        return coinSelectionTypeView;
    }

    public static EnumSendType firstFromType(final int type) {
        final List<EnumSendType> types = CACHE_TYPE_TO_ENUM.get(type);
        return CollectionUtils.isNotEmpty(types) ? types.get(0) : null;
    }

    public static List<EnumSendType> fromType(final int type) {
        return CACHE_TYPE_TO_ENUM.get(type);
    }

    public static EnumSendType fromSelectionType(final EnumCoinSelectionType selectionType) {
        return CACHE_SELECTION_TO_ENUM.get(selectionType);
    }

    public abstract EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act) throws Exception;

    public ReviewTxModel buildTx(final ReviewTxModel model) throws Exception {
        model.getTxData().postValue(TxData.copy(model.getTxData().getValue()));
        return model;
    }

    @NotNull
    public EnumSendType toSelection(final EnumCoinSelectionType selectionType) {
        if (isBatchSpend()) {
            switch (selectionType) {
                case SIMPLE:
                    return EnumSendType.SPEND_BATCH;
                case CUSTOM:
                    return EnumSendType.SPEND_CUSTOM_BATCH;
                case BATCH_SPEND:
                    return EnumSendType.SPEND_BATCH;
                case CUSTOM_BATCH_SPEND:
                    return EnumSendType.SPEND_CUSTOM_BATCH;
                default:
                    return this;
            }
        } else if (isRicochet()) {
            switch (selectionType) {
                case SIMPLE:
                    return EnumSendType.SPEND_RICOCHET;
                case CUSTOM:
                    return EnumSendType.SPEND_RICOCHET_CUSTOM;
                default:
                    return this;
            }
        }
        return fromSelectionType(selectionType);
    }
}
