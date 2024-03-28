package com.samourai.wallet.send.cahoots;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.wallet.sender.CahootsSorobanInitiatorListener;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.TxBroadcastInteraction;
import com.samourai.wallet.sorobanClient.SorobanInteraction;
import com.samourai.wallet.widgets.CahootsCircleProgress;
import com.samourai.wallet.widgets.ViewPager;

import androidx.fragment.app.FragmentManager;

public class SorobanCahootsUi extends ManualCahootsUi {
    SorobanCahootsUi(CahootsCircleProgress stepsViewGroup, ViewPager viewPager,
                     Intent intent, FragmentManager fragmentManager,
                     Activity activity) throws Exception {
        super(stepsViewGroup, viewPager, intent, fragmentManager, i -> SorobanCahootsStepFragment.newInstance(i), activity);
    }

    public void checkCahootsContext(CahootsContext cahootsContext) throws Exception {
        if (!typeUser.equals(cahootsContext.getTypeUser())) {
            throw new Exception("context.typeUser mismatch");
        }
        if (!cahootsType.equals(cahootsContext.getCahootsType())) {
            throw new Exception("context.typeUser mismatch");
        }
    }

    public CahootsSorobanInitiatorListener computeInitiatorListener() {
        return new CahootsSorobanInitiatorListener() {
            @Override
            public void onResponse(SorobanResponseMessage sorobanResponse) throws Exception {
                super.onResponse(sorobanResponse);

                // notify
                String text = sorobanResponse.isAccept() ? "Cahoots request accepted!" : "Cahoots request refused!";
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onInteraction(OnlineSorobanInteraction interaction) throws Exception {
                SorobanInteraction originInteraction = interaction.getInteraction();
                if (originInteraction instanceof TxBroadcastInteraction) {
                    getActivity().runOnUiThread(() -> {
                        setInteraction((TxBroadcastInteraction)originInteraction);
                        cahootReviewFragment.setOnBroadcast(() -> {
                            // notify Soroban partner - this will trigger notifyWalletAndFinish()
                            interaction.sorobanAccept();
                            return null;
                        });
                    });
                } else {
                    throw new Exception("Unknown interaction: "+originInteraction.getTypeInteraction());
                }
            }

            @Override
            public void progress(OnlineCahootsMessage message) {
                super.progress(message);
                getActivity().runOnUiThread(() -> {
                    try {
                        setCahootsMessage(message);
                    } catch (Exception e){
                        e.printStackTrace();
                    }});
            }
        };
    }
}
