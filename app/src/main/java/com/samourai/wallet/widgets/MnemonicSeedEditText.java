package com.samourai.wallet.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;

import org.bitcoinj.crypto.MnemonicCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sarath kumar on 1/23/2018.
 */
public class MnemonicSeedEditText extends androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView {
    TextWatcher textWatcher;
    private static final String TAG = "MnemonicSeedEditText";
    String lastString;
    int counter = 0;
    String separator = " ";

    private Context context = null;

    private List<String> validWordList = null;

    public MnemonicSeedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.setDropDownBackgroundResource(R.drawable.rounded_rectangle_pinview);
        init();
    }

    public MnemonicSeedEditText(Context context) {
        super(context);
        init();
    }

    public MnemonicSeedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setMovementMethod(LinkMovementMethod.getInstance());
        setSelection(0);
        setThreshold(2);
        setCursorVisible(true);
        requestFocus();
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String thisString = s.toString();
                String[] arr = thisString.split(separator);
                if(arr.length != 0 && !thisString.isEmpty()) {
                    String lastItem = arr[arr.length - 1];
                    if (validWordList.indexOf(lastItem) != -1 && !thisString.equals(lastString)) {
                        format(thisString);
                    }

                    if(lastItem.length() > 1 && lastItem.length() <= 4){
                        MnemonicSeedEditText.this.showDropDown();
                    }else {
                        MnemonicSeedEditText.this.dismissDropDown();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        addTextChangedListener(textWatcher);

        validWordList = MnemonicCode.INSTANCE.getWordList();
    }

    private void format(String thisString) {

        SpannableStringBuilder sb = new SpannableStringBuilder();
        String[] words = thisString.split(separator);

        if(thisString.charAt(thisString.length() - 1) == separator.charAt(0)) {
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                sb.append(word.replace("\n", ""));
                if (validWordList.indexOf(word.trim()) == -1) {
                    Toast.makeText(context, R.string.invalid_mnemonic_word, Toast.LENGTH_SHORT).show();
                    break;
                }
                BitmapDrawable bd = convertViewToDrawable(createTokenView(word));
                bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
                int startIdx = sb.length() - (word.length());
                int endIdx = sb.length();
                sb.setSpan(new ImageSpan(bd), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                SpanClick myClickableSpan = new SpanClick(startIdx, endIdx);
                sb.setSpan(myClickableSpan, Math.max(endIdx - 2, startIdx), endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (i < words.length - 1) {
                    sb.append(separator);
                } else if (thisString.charAt(thisString.length() - 1) == separator.charAt(0)) {
                    sb.append(separator);
                }
            }
            lastString = sb.toString();
            setText(sb);
            setSelection(sb.length());
        }
    }

    public View createTokenView(String text) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundResource(R.drawable.round_rectangle_token_word_background);
        TextView tv = new TextView(getContext());
        tv.setPadding(22, 5, 22, 5);
        layout.addView(tv);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        tv.setTextColor(Color.WHITE);
        return layout;
    }


    public BitmapDrawable convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(getContext().getResources(), viewBmp);
    }

    private class SpanClick extends ClickableSpan {

        int startIdx;
        int endIdx;

        public SpanClick(int startIdx, int endIdx) {
            super();
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        @Override
        public void onClick(View widget) {
            String s = getText().toString();
            String s1 = s.substring(0, startIdx);
            String s2 = s.substring(Math.min(endIdx + 1, s.length() - 1), s.length());
            String finalString = s1 + s2;
            if(finalString.trim().isEmpty()) {
                MnemonicSeedEditText.this.setText(null);
            } else {
                MnemonicSeedEditText.this.setText(s1 + s2);
            }

        }

    }


    /**
     * Tokenizer class for handling autocomplete tokens
     * SpaceTokenizer splits given text based white space character
     * when user adds spaces we will show suggestions
     */
    public static class SpaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {

        Context mContext;

        public SpaceTokenizer(Context context) {
            mContext = context;
        }

        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == ' ') {
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            if (i > 0 && text.charAt(i - 1) == ' ') {
                return text;
            } else {
                return text + " ";
            }
        }
    }

}
