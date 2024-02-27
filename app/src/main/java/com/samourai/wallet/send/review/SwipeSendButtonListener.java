package com.samourai.wallet.send.review;

public interface SwipeSendButtonListener {

    enum EnumSwipeSendButtonState {
        IS_SWIPING_ENABLED,
        IS_SWIPING_DISABLED,
        DONE,
    }
    void onStateChange(EnumSwipeSendButtonState state);
}
