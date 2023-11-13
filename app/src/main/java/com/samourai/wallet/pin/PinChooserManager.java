package com.samourai.wallet.pin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.common.collect.Lists;
import com.samourai.wallet.R;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.widgets.PinEntryView;

import java.util.List;

public class PinChooserManager {

    public static final String TAG = "PinChooserManager";

    private final FragmentActivity activity;
    private int layoutResource;
    private View viewBuilt;
    private List<OnPinEntryListener> onPinEntryListenerList = Lists.newLinkedList();
    private PinEntryView entryView;
    private StringBuilder passPhrase = new StringBuilder();
    private ImageView[] pinEntries;
    private LinearLayout maskPassPhraseContainer;

    public interface OnPinEntryListener {
        void pinEntry(String pin);
    }

    private PinChooserManager(final FragmentActivity activity, final int layoutResource) {
        this.activity = activity;
        this.layoutResource = layoutResource;
    }

    public static PinChooserManager create(final FragmentActivity activity,
                                           final int layoutResource) {

        return new PinChooserManager(activity, layoutResource);
    }

    public View getView() {
        if (isNull(viewBuilt)) {
            viewBuilt = activity.getLayoutInflater().inflate(layoutResource, null);
        }
        return viewBuilt;
    }

    public PinChooserManager install() {

        final View viewBuilt = getView();

        entryView = viewBuilt.findViewById(R.id.pin_entry_view);
        maskPassPhraseContainer = viewBuilt.findViewById(R.id.passphrase_mask_container);
        initMaskView();
        entryView.setEntryListener((key, view1) -> {
            passPhrase.append(key);
            addKeyText();
            propagateToActivity();

        });
        entryView.setClearListener(clearType -> {
            if (clearType == PinEntryView.KeyClearTypes.CLEAR) {
                if (passPhrase.length() - 1 >= 0) {
                    passPhrase.deleteCharAt(passPhrase.length() - 1);
                }
            } else {
                passPhrase.setLength(0);
            }
            propagateToActivity();
            addKeyText();
        });

        if (!PrefsUtil.getInstance(activity).getValue(PrefsUtil.HAPTIC_PIN, true)) {
            entryView.disableHapticFeedBack();
        }

        return this;
    }

    public void resetPin() {
        entryView.resetPinLen();
        passPhrase.setLength(0);
        hideConfirmButton();
        addKeyText();
    }

    private void initMaskView() {
        pinEntries = new ImageView[8];
        for (int i = 0; i < 8; i++) {
            pinEntries[i] = new ImageView(activity);
            pinEntries[i].setImageDrawable(activity.getResources().getDrawable(R.drawable.circle_dot_white));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            maskPassPhraseContainer.addView(pinEntries[i], params);
        }
    }

    private void addKeyText() {
        for (int i = 0; i < 8; i++) {
            if (passPhrase.toString().length() > i) {
                pinEntries[i].getDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
            } else {
                pinEntries[i].setImageDrawable(activity.getDrawable(R.drawable.circle_dot_white));
            }
        }
    }

    private void propagateToActivity() {
        for (final OnPinEntryListener onPinEntryListener : onPinEntryListenerList) {
            onPinEntryListener.pinEntry(passPhrase.toString());
        }
    }

    public PinChooserManager setTitle(final String title) {
        return setTitleOnView(title);
    }

    public PinChooserManager setDescription(final String description) {
        return setDescriptionOnView(description);
    }

    private PinChooserManager setTitleOnView(final String title) {
        if (nonNull(title)) {
            ((TextView) getView().findViewById(R.id.pin_entry_title)).setText(title);
        }
        return this;
    }

    private PinChooserManager setDescriptionOnView(final String description) {
        if (nonNull(description)) {
            ((TextView) getView().findViewById(R.id.pin_entry_description)).setText(description);
        }
        return this;
    }

    public void showConfirmButton() {
        entryView.showCheckButton();
    }

    public void hideConfirmButton() {
        entryView.hideCheckButton();
    }

    public PinChooserManager addOnPinEntryListener(final OnPinEntryListener onPinEntryListener) {
        if (nonNull(onPinEntryListener)) {
            onPinEntryListenerList.add(onPinEntryListener);
        }
        return this;
    }

    public PinChooserManager setConfirmListener(final View.OnClickListener clickListener) {
        entryView.setConfirmClickListener(clickListener);
        return this;
    }
}
