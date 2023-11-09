package com.samourai.wallet.pin;

import static org.apache.commons.lang3.StringUtils.length;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.pin.PinChooserManager.OnPinEntryListener;

import java.util.Objects;

public class PinChangeDialog extends BottomSheetDialogFragment
        implements OnPinEntryListener, View.OnClickListener {

    private PinChooserManager pinChooserManager;
    private OnSuccessCallback onSuccessCallback;

    private String pinChosen;
    private String currentPin;

    public interface OnSuccessCallback {
        void onSuccess(final String pin);
    }

    private PinChangeDialog() {}

    public static PinChangeDialog create() {
        return new PinChangeDialog();
    }

    public PinChangeDialog setOnSuccessCallback(final OnSuccessCallback onSuccessCallback) {
        this.onSuccessCallback = onSuccessCallback;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (!BuildConfig.FLAVOR.equals("staging")) {
            getActivity().getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        if (isNull(pinChooserManager)) {
            pinChooserManager = PinChooserManager
                    .create(getActivity(), R.layout.fragment_choose_pin)
                    .addOnPinEntryListener(this)
                    .install()
                    .setConfirmListener(this);
        }
        return pinChooserManager.getView();
    }

    @Override
    public void pinEntry(final String pin) {
        currentPin = pin;
        if (length(pinChosen) == 0) {
            final int currentPinlength = length(pin);
            if (currentPinlength >= AccessFactory.MIN_PIN_LENGTH &&
                    currentPinlength <= AccessFactory.MAX_PIN_LENGTH) {
                pinChooserManager.showConfirmButton();
            } else {
                pinChooserManager.hideConfirmButton();
            }
        } else {
            if (Objects.equals(currentPin, pinChosen)) {
                pinChooserManager.showConfirmButton();
            } else {
                pinChooserManager.hideConfirmButton();
            }
        }
    }

    @Override
    public void onClick(final View v) {
        if (length(pinChosen) == 0) {
            final int currentPinlength = length(currentPin);
            if (currentPinlength >= AccessFactory.MIN_PIN_LENGTH &&
                    currentPinlength <= AccessFactory.MAX_PIN_LENGTH) {
                pinChosen = currentPin;
                pinChooserManager.resetPin();
                pinChooserManager.setTitle(getActivity().getString(R.string.pin_5_8_confirm));
                pinChooserManager.setDescription(getActivity().getString(R.string.re_enter_your_pin_code));
            }
        } else {
            if (Objects.equals(currentPin, pinChosen)) {
                fireOnSuccessCallback();
            }
        }
    }

    private void fireOnSuccessCallback() {
        if (nonNull(onSuccessCallback)) {
            onSuccessCallback.onSuccess(pinChosen);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
