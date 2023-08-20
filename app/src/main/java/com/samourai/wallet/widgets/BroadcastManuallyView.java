package com.samourai.wallet.widgets;

import static java.util.Objects.nonNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.samourai.wallet.R;

public class BroadcastManuallyView extends FrameLayout {

    private View mainView;
    private CheckBox checkBoxDoNotSpend;
    private Button btnClose;
    private ImageView transactionImageView;
    private ImageButton leftTopImgBtn, copyBtn;

    public BroadcastManuallyView(@NonNull Context context) {
        super(context);
        init();
    }

    public BroadcastManuallyView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BroadcastManuallyView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public BroadcastManuallyView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mainView = LayoutInflater.from(getContext()).inflate(R.layout.broadcast_manually_view, null);
        btnClose = mainView.findViewById(R.id.close_button);
        leftTopImgBtn = mainView.findViewById(R.id.leftTopImageButton);
        copyBtn = mainView.findViewById(R.id.copyAddressToClipboard);
        transactionImageView = mainView.findViewById(R.id.tx_qr_code_view);
        checkBoxDoNotSpend = mainView.findViewById(R.id.do_not_spend_option_checkbox);
        addView(mainView);
    }

    public boolean doNotSpendChecked() {
        return checkBoxDoNotSpend.isChecked();
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public ImageButton getLeftTopImgBtn() {
        return leftTopImgBtn;
    }

    public ImageButton getCopyBtn() {
        return copyBtn;
    }

    public ImageView getTransactionImageView() {
        return transactionImageView;
    }

    public void setTransactionBitmap(final Bitmap transactionBitmap) {
        if (nonNull(transactionBitmap)) {
            transactionImageView.setImageBitmap(transactionBitmap);
        }
    }
}
