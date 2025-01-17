package com.samourai.wallet.widgets;

import android.content.Context;
import android.os.Vibrator;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.access.ScrambledPin;

import static android.content.Context.VIBRATOR_SERVICE;


/**
 * Re-Usable custom KeypadView for pin entry
 */
public class PinEntryView extends FrameLayout implements View.OnClickListener {

    public enum KeyClearTypes {
        CLEAR_ALL,
        CLEAR
    }

    private boolean isDisabled = false;
    private TextView ta = null;
    private TextView tb = null;
    private TextView tc = null;
    private TextView td = null;
    private TextView te = null;
    private TextView tf = null;
    private TextView tg = null;
    private TextView th = null;
    private TextView ti = null;
    private TextView tj = null;
    private ImageButton tconfirm = null;
    private ImageButton tback = null;
    private ScrambledPin keypad = null;
    private int pinLen = 0;
    private boolean scramble = false;
    private pinEntryListener entryListener = null;
    private pinClearListener clearListener = null;
    private Vibrator vibrator;
    private boolean enableHaptic = true;

    public PinEntryView(Context context) {
        super(context);
        initView();
    }

    public PinEntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinEntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setEntryListener(pinEntryListener entryListener) {
        this.entryListener = entryListener;
    }

    public void setClearListener(pinClearListener clearListener) {
        this.clearListener = clearListener;
    }

    public void setScramble(boolean scramble) {
        this.scramble = scramble;
        setButtonLabels();
    }

    private void initView() {
        vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);
        View view = inflate(getContext(), R.layout.keypad_view, null);
        ta = view.findViewById(R.id.ta);
        tb = view.findViewById(R.id.tb);
        tc = view.findViewById(R.id.tc);
        td = view.findViewById(R.id.td);
        te = view.findViewById(R.id.te);
        tf = view.findViewById(R.id.tf);
        tg = view.findViewById(R.id.tg);
        th = view.findViewById(R.id.th);
        ti = view.findViewById(R.id.ti);
        tj = view.findViewById(R.id.tj);
        tconfirm = view.findViewById(R.id.tconfirm);
        tback = view.findViewById(R.id.tback);
        tback.setOnClickListener(view1 -> {
            hapticFeedBack();
            removeLastPinDigit();
        });
        tback.setOnLongClickListener(view12 -> {
            clearPinDigits();
            hapticFeedBack();
            return false;
        });
        setButtonLabels();
        addView(view);
    }

    @Override
    public void onClick(View view) {
        if(isDisabled){
            return;
        }
        hapticFeedBack();
        addPinDigit(view);
    }

    synchronized private void addPinDigit(View view) {
        if (pinLen <= (AccessFactory.MAX_PIN_LENGTH -1 )) {
            if (entryListener != null) {
                if (((TextView) view).getText().toString().length() < AccessFactory.MAX_PIN_LENGTH) {
                    entryListener.onPinEntered(((TextView) view).getText().toString(), view);
                }
            }
            pinLen++;
        }
    }

    synchronized private void clearPinDigits() {
        pinLen = 0;
        if (clearListener != null  && !isDisabled) {
            clearListener.onPinClear(KeyClearTypes.CLEAR_ALL);
        }
    }

    synchronized private void removeLastPinDigit() {
        pinLen--;
        if (clearListener != null && !isDisabled) {
            clearListener.onPinClear(KeyClearTypes.CLEAR);
        }
    }

    private void hapticFeedBack() {
        if (enableHaptic) {
            vibrator.vibrate(44);
        }
    }


    public void showCheckButton() {
        tconfirm.setVisibility(VISIBLE);
    }

    public void hideCheckButton() {
        tconfirm.setVisibility(GONE);
    }

    synchronized public void resetPinLen() {
        pinLen = 0;
    }

    public void setConfirmClickListener(OnClickListener clickListener){
        tconfirm.setOnClickListener(clickListener);
    }

    public void disableHapticFeedBack(){
        enableHaptic = false;
    }

    private void setButtonLabels() {
        keypad = new ScrambledPin();
        ta.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(0).getValue()) : "1");
        ta.setOnClickListener(this);
        tb.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(1).getValue()) : "2");
        tb.setOnClickListener(this);
        tc.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(2).getValue()) : "3");
        tc.setOnClickListener(this);
        td.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(3).getValue()) : "4");
        td.setOnClickListener(this);
        te.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(4).getValue()) : "5");
        te.setOnClickListener(this);
        tf.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(5).getValue()) : "6");
        tf.setOnClickListener(this);
        tg.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(6).getValue()) : "7");
        tg.setOnClickListener(this);
        th.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(7).getValue()) : "8");
        th.setOnClickListener(this);
        ti.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(8).getValue()) : "9");
        ti.setOnClickListener(this);
        tj.setText(this.scramble ? Integer.toString(keypad.getMatrix().get(9).getValue()) : "0");
        tj.setOnClickListener(this);
    }


    public void disable(boolean b) {
        isDisabled = b;
        if(b){
            clearListener.onPinClear(KeyClearTypes.CLEAR_ALL);
        }
        this.setAlpha(b ? 0.6f : 1f );
    }

    public interface pinEntryListener {
        void onPinEntered(String key, View view);
    }

    public interface pinClearListener {
        void onPinClear(KeyClearTypes clearType);
    }
}
