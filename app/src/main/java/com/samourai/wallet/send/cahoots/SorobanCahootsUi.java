package com.samourai.wallet.send.cahoots;

import android.app.Activity;
import android.content.Intent;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.TxBroadcastInteraction;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanInteraction;
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService;
import com.samourai.wallet.widgets.CahootsCircleProgress;
import com.samourai.wallet.widgets.ViewPager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.function.Function;

public class SorobanCahootsUi extends ManualCahootsUi {

    private CahootsContext cahootsContext;

    SorobanCahootsUi(CahootsCircleProgress stepsViewGroup, ViewPager viewPager,
                     Intent intent, FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider,
                     Activity activity) throws Exception {
        super(stepsViewGroup, viewPager, intent, fragmentManager, fragmentProvider, activity);

        // listen for interactions
        sorobanCahootsService.getSorobanService().getOnInteraction().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(interaction -> {
                    setInteraction(interaction);
                });
    }

    public CahootsContext setCahootsContextInitiator(int account, long feePerB, long sendAmount, String sendAddress) throws Exception {
        cahootsContext = computeCahootsContextInitiator(account, feePerB, sendAmount, sendAddress);

        // verify
        if (!typeUser.equals(cahootsContext.getTypeUser())) {
            throw new Exception("context.typeUser mismatch");
        }
        if (!cahootsType.equals(cahootsContext.getCahootsType())) {
            throw new Exception("context.typeUser mismatch");
        }
        return cahootsContext;
    }

    public CahootsContext setCahootsContextCounterparty(int account) throws Exception {
        cahootsContext = CahootsContext.newCounterparty(cahootsType, account);

        // verify
        if (!typeUser.equals(cahootsContext.getTypeUser())) {
            throw new Exception("context.typeUser mismatch");
        }
        if (!cahootsType.equals(cahootsContext.getCahootsType())) {
            throw new Exception("context.typeUser mismatch");
        }
        return cahootsContext;
    }

    void setInteraction(OnlineSorobanInteraction onlineInteraction) throws Exception {
        SorobanInteraction originInteraction = onlineInteraction.getInteraction();
        if (originInteraction instanceof TxBroadcastInteraction) {
            setInteraction((TxBroadcastInteraction)originInteraction);
            cahootReviewFragment.setOnBroadcast(() -> {
                // notify Soroban partner - this will trigger notifyWalletAndFinish()
                onlineInteraction.sorobanAccept();
                return null;
            });
        } else {
            throw new Exception("Unknown interaction: "+originInteraction.getTypeInteraction());
        }
    }

    public AndroidSorobanCahootsService getSorobanCahootsService() {
        return sorobanCahootsService;
    }
}
