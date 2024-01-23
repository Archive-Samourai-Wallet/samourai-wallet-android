package com.samourai.wallet.send.review;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;
import com.samourai.wallet.SamouraiActivity;

import java.util.Map;

public enum EnumSendType {
    SPEND_SIMPLE(0) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendSimpleTxBroadcaster.create(model.buildSimleTxData(), act).broadcast();
            return this;
        }
    },
    SPEND_BOLTZMANN(1) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendSimpleTxBroadcaster.create(model.buildBoltzmannTxData(), act).broadcast();
            return this;
        }
    },
    SPEND_RICOCHET(2) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendRicochetTxBroadcaster.create(model.buildRicochetTxData(), act).broadcast();
            return this;
        }
    },
    SPEND_JOINBOT(3) {
        @Override
        public EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act)
                throws Exception {

            SpendJoinbotTxBroadcaster.create(model.buildJoinbotTxData(), act).broadcast();
            return this;
        }
    },
    ;

    private static final Map<Integer, EnumSendType> CACHE_TYPE_TO_ENUM = createCacheTypeToEnum();

    @NonNull
    private static Map<Integer, EnumSendType> createCacheTypeToEnum() {
        final ImmutableMap.Builder<Integer, EnumSendType> mapBuilder = ImmutableMap.builder();
        for (final EnumSendType sendType : EnumSendType.values()) {
            mapBuilder.put(sendType.getType(), sendType);
        }
        return mapBuilder.build();
    }

    private final int type;

    EnumSendType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static EnumSendType fromType(final int type) {
        return CACHE_TYPE_TO_ENUM.get(type);
    }

    public abstract EnumSendType broadcastTx(final ReviewTxModel model, final SamouraiActivity act) throws Exception;
}
