package com.samourai.wallet.fragments;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.samourai.wallet.R;
import com.samourai.wallet.pin.PinChooserManager;
import com.samourai.wallet.pin.PinChooserManager.OnPinEntryListener;


public class PinEntryFragment extends Fragment {
    private static String ARG_TITLE = "TITLE";
    private static String ARG_DESC = "DESC";

    private PinChooserManager pinChooserManager;
    private OnPinEntryListener onPinEntryListener;

    public PinEntryFragment() {
        // Required empty public constructor
    }

    public static PinEntryFragment newInstance(final String title, final String description) {
        PinEntryFragment fragment = new PinEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        if (isNull(pinChooserManager)) {
            final String title = nonNull(getArguments()) ?  getArguments().getString(ARG_TITLE) : null;
            final String description = nonNull(getArguments()) ?  getArguments().getString(ARG_DESC) : null;
            pinChooserManager = PinChooserManager
                    .create(getActivity(), R.layout.fragment_choose_pin)
                    .setTitle(title)
                    .setDescription(description)
                    .addOnPinEntryListener(onPinEntryListener)
                    .install();
        }
        return pinChooserManager.getView();
    }

    public PinEntryFragment setOnPinEntryListener(final OnPinEntryListener onPinEntryListener) {
        this.onPinEntryListener = onPinEntryListener;
        return this;
    }
}
