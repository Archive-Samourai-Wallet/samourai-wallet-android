package com.samourai.wallet.send.cahoots;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samourai.wallet.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SorobanCahootsStepFragment extends AbstractCahootsStepFragment {
    public static SorobanCahootsStepFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("step", position);
        SorobanCahootsStepFragment fragment = new SorobanCahootsStepFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cahoots_step_view_soroban, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (step == 0) {
            TextView infoText = view.findViewById(R.id.cahoots_info_text);
            infoText.setText("Have your mixing partner receive online Cahoots.");
        }
    }
}
