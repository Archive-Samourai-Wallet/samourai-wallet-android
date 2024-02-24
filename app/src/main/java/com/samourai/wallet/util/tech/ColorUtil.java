package com.samourai.wallet.util.tech;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;

public class ColorUtil {

    private ColorUtil() {}

    public static int getAttributeColor(final Context context, final int attr) {
        int attributeColor = 0;
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            int colorResId = typedArray.getResourceId(0, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                attributeColor = context.getResources().getColor(colorResId, context.getTheme());
            } else {
                attributeColor = context.getResources().getColor(colorResId);
            }
        } finally {
            typedArray.recycle();
        }
        return attributeColor;
    }

    public static int lightenColor(final int color, final float factor) {
        final float factorNormalized = Math.max(0f, Math.min(factor, 1f));

        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        red = (int) (red + factorNormalized * (255 - red));
        green = (int) (green + factorNormalized * (255 - green));
        blue = (int) (blue + factorNormalized * (255 - blue));

        return Color.argb(alpha, red, green, blue);
    }

    public static int darkenColor(final int color, final float factor) {
        final float factorNormalized = Math.max(0f, Math.min(factor, 1f));

        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        red = (int) (red * (1 - factorNormalized));
        green = (int) (green * (1 - factorNormalized));
        blue = (int) (blue * (1 - factorNormalized));

        return Color.argb(alpha, red, green, blue);
    }

}
