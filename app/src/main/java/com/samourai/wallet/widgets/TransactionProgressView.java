package com.samourai.wallet.widgets;

import static com.samourai.wallet.TxAnimUIActivity.BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.samourai.wallet.R;
import com.samourai.wallet.util.view.ViewHelper;


public class TransactionProgressView extends FrameLayout {

    private ArcProgress mArcProgress;
    private ImageView mCheckMark;
    private Button optionBtn1, optionBtn2;
    private ImageButton leftTopImgBtn, optionInfoDetails;
    private TextView txProgressText, txSubText, optionTitle;
    private View mainView;
    private ProgressBar optionProgressBar;
    private Theme theme;

    public TransactionProgressView(@NonNull Context context) {
        super(context);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    public TransactionProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mainView = LayoutInflater.from(getContext()).inflate(R.layout.transaction_progress_view, null);
        mArcProgress = mainView.findViewById(R.id.arc_progress);
        mCheckMark = mainView.findViewById(R.id.check_vd);
        leftTopImgBtn = mainView.findViewById(R.id.leftTopImageButton);
        txProgressText = mainView.findViewById(R.id.tx_progress_text);
        txSubText = mainView.findViewById(R.id.tx_progress_sub_text);
        optionTitle = mainView.findViewById(R.id.options_title);
        optionBtn1 = mainView.findViewById(R.id.option_button_1);
        optionBtn2 = mainView.findViewById(R.id.option_button_2);
        optionInfoDetails = mainView.findViewById(R.id.option_info_details);
        optionProgressBar = mainView.findViewById(R.id.option_progress_bar);
        addView(mainView);
        reset();
    }

    public ArcProgress getmArcProgress() {
        return mArcProgress;
    }

    public void showCheck() {
        mCheckMark.setImageDrawable(getResources().getDrawable(R.drawable.animated_check_vd, theme));
        ((Animatable) mCheckMark.getDrawable()).start();
    }

    public ImageView getmCheckMark() {
        return mCheckMark;
    }

    public void setTxStatusMessage(int resId) {
        txProgressText.setText(resId);
    }

    public void reset() {
        leftTopImgBtn.setVisibility(INVISIBLE);
        optionTitle.setVisibility(INVISIBLE);
        optionBtn1.setVisibility(INVISIBLE);
        optionBtn2.setVisibility(INVISIBLE);
        optionInfoDetails.setVisibility(INVISIBLE);
        optionProgressBar.setVisibility(INVISIBLE);
        txProgressText.setText(null);
        txSubText.setText(null);
        mArcProgress.reset();
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public View getMainView() {
        return mainView;
    }

    public ImageButton getLeftTopImgBtn() {
        return leftTopImgBtn;
    }

    public Button getOptionBtn1() {
        return optionBtn1;
    }

    public Button getOptionBtn2() {
        return optionBtn2;
    }

    public ProgressBar getOptionProgressBar() {
        return optionProgressBar;
    }

    public void showSuccessSentTxOptions(final boolean changeBtnVisible, final int changeBtnText) {

        leftTopImgBtn.setVisibility(VISIBLE);
        mArcProgress.setVisibility(INVISIBLE);

        if (changeBtnVisible) {
            //optionInfoDetails.setVisibility(VISIBLE);
            optionTitle.setText(R.string.tx_option_section_change);
            optionTitle.setVisibility(VISIBLE);
            optionBtn1.setText(changeBtnText);
            optionBtn1.setVisibility(VISIBLE);
            optionBtn1.setTextColor(getResources().getColor(R.color.blue_send_ui));
            optionBtn2.setVisibility(INVISIBLE);
        } else {
            optionInfoDetails.setVisibility(INVISIBLE);
            optionTitle.setVisibility(INVISIBLE);
            optionBtn1.setVisibility(INVISIBLE);
            optionBtn2.setVisibility(INVISIBLE);
        }
        getResources().getDrawable(R.drawable.ic_close_button);
        getLeftTopImgBtn().setImageDrawable(getResources().getDrawable(R.drawable.ic_close_button, theme));
    }

    public void showOfflineTxOptions(final int resIdDetails) {

        leftTopImgBtn.setVisibility(VISIBLE);
        mArcProgress.setVisibility(INVISIBLE);

        setBackgroundColorForOnlineMode(
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS,
                R.color.blue_send_ui,
                R.color.orange_send_ui);

        txProgressText.setText(R.string.tx_status_not_sent);
        txSubText.setText(resIdDetails);

        //optionInfoDetails.setVisibility(VISIBLE);
        optionTitle.setText(R.string.tx_option_section_broadcast);
        optionTitle.setVisibility(VISIBLE);

        optionBtn1.setText(getResources().getText(R.string.broadcast_with_txtenna));
        optionBtn1.setVisibility(VISIBLE);
        ViewHelper.animateChangeColor(
                animator -> optionBtn1.setTextColor((int) animator.getAnimatedValue()),
                getResources().getColor(R.color.blue_send_ui),
                getResources().getColor(R.color.orange_send_ui),
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

        optionBtn2.setText(R.string.tx_option_broadcast_manually);
        optionBtn2.setVisibility(VISIBLE);
        ViewHelper.animateChangeColor(
                animator -> optionBtn2.setTextColor((int) animator.getAnimatedValue()),
                getResources().getColor(R.color.blue_send_ui),
                getResources().getColor(R.color.orange_send_ui),
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

        getLeftTopImgBtn().setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_arrow_back_24, theme));
        getmCheckMark().setImageDrawable(getResources().getDrawable(R.drawable.ic_network_strength_off, theme));
    }

    public void showFailedTxOptions(final int resIdDetails) {

        leftTopImgBtn.setVisibility(VISIBLE);
        mArcProgress.setVisibility(INVISIBLE);

        setBackgroundColorForOnlineMode(
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS,
                R.color.blue_send_ui,
                R.color.red_send_ui);

        txProgressText.setText(R.string.tx_status_broadcast_error);
        txSubText.setText(resIdDetails);

        //optionInfoDetails.setVisibility(VISIBLE);
        optionTitle.setText(R.string.tx_option_section_troubleshoot);
        optionTitle.setVisibility(VISIBLE);

        optionBtn1.setText(R.string.tx_option_attempt_broadcast);
        optionBtn1.setVisibility(VISIBLE);
        ViewHelper.animateChangeColor(
                animator -> optionBtn1.setTextColor((int) animator.getAnimatedValue()),
                getResources().getColor(R.color.blue_send_ui),
                getResources().getColor(R.color.red_send_ui),
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

        optionBtn2.setText(R.string.tx_option_contact_support);
        optionBtn2.setVisibility(VISIBLE);
        ViewHelper.animateChangeColor(
                animator -> optionBtn2.setTextColor((int) animator.getAnimatedValue()),
                getResources().getColor(R.color.blue_send_ui),
                getResources().getColor(R.color.red_send_ui),
                BACKGROUND_COLOR_CHANGE_ANIM_DURATION_IN_MS);

        getLeftTopImgBtn().setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_arrow_back_24, theme));
        getmCheckMark().setImageDrawable(getResources().getDrawable(R.drawable.ic_alert_circle, theme));
    }

    public void setBackgroundColorForOnlineMode(final int durationInMs,
                                                final int colorFrom,
                                                final int colorTo) {

        ViewHelper.animateChangeColor(
                mainView,
                getResources().getColor(colorFrom),
                getResources().getColor(colorTo),
                durationInMs);
    }
}
