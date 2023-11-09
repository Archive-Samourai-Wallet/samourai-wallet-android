package com.samourai.wallet.pin;

import static java.util.Objects.isNull;

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
import com.samourai.wallet.pin.PinEntryManager.OnSuccessCallback;

public class PinEntryDialog extends BottomSheetDialogFragment {

    private PinEntryManager pinEntryManager;
    private OnSuccessCallback onSuccessCallback;

    private PinEntryDialog() {}

    public static PinEntryDialog create() {
        return new PinEntryDialog();
    }

    public PinEntryDialog setOnSuccessCallback(final OnSuccessCallback onSuccessCallback) {
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

        if (isNull(pinEntryManager)) {
            pinEntryManager = PinEntryManager.create(getActivity(), R.layout.fragment_pinentry, false)
                    .setTitle(getActivity().getString(R.string.enter_pin_code))
                    .setDescription(getActivity().getString(R.string.enter_your_current_pin_code))
                    .install(onSuccessCallback);
        }
        return pinEntryManager.getView();
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
