package com.samourai.wallet.send.cahoots;

import android.app.Activity;
import android.content.Intent;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.TxBroadcastInteraction;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.wallet.sender.CahootsSorobanInitiatorListener;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.widgets.CahootsCircleProgress;
import com.samourai.wallet.widgets.ViewPager;

import java.util.function.Function;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

public class SorobanCahootsUi extends ManualCahootsUi {
    private CahootsContext cahootsContext;

    SorobanCahootsUi(CahootsCircleProgress stepsViewGroup, ViewPager viewPager,
                     Intent intent, FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider,
                     Activity activity) throws Exception {
        super(stepsViewGroup, viewPager, intent, fragmentManager, fragmentProvider, activity);
    }

    public Single<Cahoots> startInitiator(int account, long feePerB, long sendAmount, String sendAddress, String paynymDestination, PaymentCode paymentCodeCounterparty, Consumer<OnlineCahootsMessage> onProgress) throws Exception {
        cahootsContext = computeCahootsContextInitiator(account, feePerB, sendAmount, sendAddress, paynymDestination);

        // verify
        checkCahootsContext();

        // start initiator
        CahootsSorobanInitiatorListener listener = new CahootsSorobanInitiatorListener() {
            @Override
            public void onInteraction(OnlineSorobanInteraction interaction) throws Exception {
                SorobanInteraction originInteraction = interaction.getInteraction();
                if (originInteraction instanceof TxBroadcastInteraction) {
                    setInteraction((TxBroadcastInteraction)originInteraction);
                    cahootReviewFragment.setOnBroadcast(() -> {
                        // notify Soroban partner - this will trigger notifyWalletAndFinish()
                        interaction.sorobanAccept();
                        return null;
                    });
                } else {
                    throw new Exception("Unknown interaction: "+originInteraction.getTypeInteraction());
                }
            }

            @Override
            public void progress(OnlineCahootsMessage message) {
                super.progress(message);
                try {
                    onProgress.accept(message);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        return sorobanWalletService.getSorobanWalletInitiator().initiator(cahootsContext, paymentCodeCounterparty, listener);
    }

    public Single<Cahoots> startCounterparty(int account, PaymentCode paymentCode, Consumer<OnlineCahootsMessage> onProgress) throws Exception {
        CahootsWallet cahootsWallet = sorobanWalletService.getCahootsWallet();
        cahootsContext = CahootsContext.newCounterparty(cahootsWallet, cahootsType, account);

        // verify
        checkCahootsContext();

        // start counterparty
        return sorobanWalletService.getSorobanWalletCounterparty().counterparty(cahootsContext, paymentCode, onProgress);
    }

    private void checkCahootsContext() throws Exception {
        if (!typeUser.equals(cahootsContext.getTypeUser())) {
            throw new Exception("context.typeUser mismatch");
        }
        if (!cahootsType.equals(cahootsContext.getCahootsType())) {
            throw new Exception("context.typeUser mismatch");
        }
    }
}
