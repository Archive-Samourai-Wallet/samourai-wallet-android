package com.samourai.wallet.send.cahoots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.cahoots.AndroidSorobanWalletService;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.TxBroadcastInteraction;
import com.samourai.wallet.cahoots.manual.ManualCahootsMessage;
import com.samourai.wallet.cahoots.manual.ManualCahootsService;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.widgets.CahootsCircleProgress;
import com.samourai.wallet.widgets.ViewPager;

import java.util.ArrayList;
import java.util.function.Function;

import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ManualCahootsUi {
    private Activity activity;
    private CahootsCircleProgress stepsViewGroup;
    private ViewPager viewPager;

    protected CahootReviewFragment cahootReviewFragment;
    private ArrayList<Fragment> steps = new ArrayList<>();

    // intent
    private int account;
    protected CahootsTypeUser typeUser;
    protected CahootsType cahootsType;

    private ManualCahootsMessage cahootsMessage;

    protected AndroidSorobanWalletService sorobanWalletService;

    static Intent createIntent(Context ctx, Class activityClass, int account, CahootsType type, CahootsTypeUser typeUser) {
        Intent intent = new Intent(ctx, activityClass);
        intent.putExtra("_account", account);
        intent.putExtra("typeUser", typeUser.getValue());
        intent.putExtra("cahootsType", type.getValue());
        return intent;
    }

    ManualCahootsUi(CahootsCircleProgress stepsViewGroup, ViewPager viewPager,
                    Intent intent, FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider,
                    Activity activity) throws Exception {
        this.activity = activity;
        this.stepsViewGroup = stepsViewGroup;
        this.viewPager = viewPager;

        viewPager.enableSwipe(false);
        cahootReviewFragment = CahootReviewFragment.newInstance(intent);

        // sender+receiver
        if (intent.hasExtra("_account")) {
            account = intent.getIntExtra("_account", 0);
        }
        if (intent.hasExtra("cahootsType")) {
            int cahootsType = intent.getIntExtra("cahootsType", -1);
            this.cahootsType = CahootsType.find(cahootsType).get();
        }
        if (intent.hasExtra("typeUser")) {
            int typeUserInt = intent.getIntExtra("typeUser", -1);
            typeUser = CahootsTypeUser.find(typeUserInt).get();
        }

        // validate
        if (typeUser == null) {
            throw new Exception("Invalid typeUser");
        }
        if (cahootsType == null) {
            throw new Exception("Invalid cahootsType");
        }

        createSteps(fragmentManager, fragmentProvider);

        // setup cahoots
        sorobanWalletService = AndroidSorobanWalletService.getInstance(activity.getApplicationContext());
    }

    private void createSteps(FragmentManager fragmentManager, Function<Integer, Fragment> fragmentProvider) {
        int lastStep = ManualCahootsMessage.getLastStep(this.cahootsType);
        for (int i = 0; i < lastStep; i++) {
            Fragment stepView = fragmentProvider.apply(i);
            steps.add(stepView);
        }
        if (CahootsTypeUser.SENDER.equals(typeUser)) {
            steps.add(cahootReviewFragment);
        } else {
            Fragment stepView = fragmentProvider.apply(lastStep);
            steps.add(stepView);
        }
        stepsViewGroup.setTotalSteps(steps.size());
        viewPager.setAdapter(new StepAdapter(fragmentManager));

        setStep(0);
    }

    void setCahootsMessage(ManualCahootsMessage msg) throws Exception {
        Log.d("ManualCahootsUi", "# Cahoots => " + msg.toString());

        // check cahootsType
        if (cahootsType != null) {
            if (!msg.getType().equals(cahootsType)) {
                // possible attack?
                throw new Exception("Unexpected Cahoots cahootsType");
            }
        } else {
            cahootsType = msg.getType();
        }

        cahootsMessage = msg;

        // show cahoots progress
        int step = cahootsMessage.getStep();
        setStep(step);

        // show step screen
        Fragment stepFragment = steps.get(step);
        if (stepFragment instanceof AbstractCahootsStepFragment) {
            ((AbstractCahootsStepFragment) steps.get(step)).setCahootsMessage(cahootsMessage);
        }

        if (cahootsMessage.isDone()) {
            notifyWalletAndFinish();
        }
    }

    void setInteraction(TxBroadcastInteraction interaction) {
        // review last step
        cahootReviewFragment.setCahoots(interaction.getSignedCahoots());
        cahootReviewFragment.setOnBroadcast(() -> {
            // manual cahoots => finish on broadcast success
            notifyWalletAndFinish();
            return null;
        });
        setStep(interaction.getTypeInteraction().getStep());
    }

    private void setStep(final int step) {
        stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 1));
        viewPager.post(() -> viewPager.setCurrentItem(step, true));
    }

    private class StepAdapter extends FragmentPagerAdapter {
        StepAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public int getCount() {
            return steps.size();
        }
    }

    /**
     * Following a transaction broadcast after user have collaborated, wallet will be navigated to the following screens:
     * <p>
     * 1:Initiate Stonewall X2 or Stowaway: always returned to the account which user selected
     * 2:Participate —> Stonewall X2: always returned to the account which user selected .
     * 3:Participate —> Stowaway: always returned to the Deposit screen
     */
    private void notifyWalletAndFinish() {
        activity.runOnUiThread(() -> Toast.makeText(activity, "Cahoots success", Toast.LENGTH_LONG).show());
        if (typeUser == CahootsTypeUser.COUNTERPARTY) {
            if (cahootsType == CahootsType.STOWAWAY) {
                Intent _intent = new Intent(activity, BalanceActivity.class);
                _intent.putExtra("refresh", true);
                _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(_intent);
                return;
            }
        }
        navigateToAccountBalanceScreen();
    }

    public void navigateToAccountBalanceScreen() {
        if (account == SamouraiAccountIndex.POSTMIX) {
            Intent balanceHome = new Intent(activity, BalanceActivity.class);
            balanceHome.putExtra("_account", SamouraiAccountIndex.POSTMIX);
            balanceHome.putExtra("refresh", true);
            Intent parentIntent = new Intent(activity, BalanceActivity.class);
            parentIntent.putExtra("_account", 0);
            balanceHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder.create(activity.getApplicationContext())
                    .addNextIntent(parentIntent)
                    .addNextIntent(balanceHome)
                    .startActivities();
        } else {
            Intent _intent = new Intent(activity, BalanceActivity.class);
            _intent.putExtra("refresh", true);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(_intent);
        }
    }

    public String getTitle(CahootsMode cahootsMode) {
        if(cahootsType == CahootsType.MULTI)
            // JoinBot
            return getActivity().getResources().getString(R.string.joinbot);
        else
            return (CahootsTypeUser.SENDER.equals(typeUser) ? "Sending" : "Receiving") + " " + cahootsMode.getLabel().toLowerCase() + " " + cahootsType.getLabel();
    }

    public CahootsContext computeCahootsContextInitiator(int account, long feePerB, long sendAmount, String sendAddress, String paynymDestination) throws Exception {
        return CahootsContext.newInitiator(getCahootsWallet(), cahootsType, account, feePerB, sendAmount, sendAddress, paynymDestination);
    }

    public Activity getActivity() {
        return activity;
    }

    public int getAccount() {
        return account;
    }

    public CahootsTypeUser getTypeUser() {
        return typeUser;
    }

    public CahootsType getCahootsType() {
        return cahootsType;
    }

    public ManualCahootsMessage getCahootsMessage() {
        return cahootsMessage;
    }

    public ManualCahootsService getManualCahootsService() {
        return sorobanWalletService.getManualCahootsService();
    }
    public CahootsWallet getCahootsWallet() {
        return sorobanWalletService.getCahootsWallet();
    }
}
