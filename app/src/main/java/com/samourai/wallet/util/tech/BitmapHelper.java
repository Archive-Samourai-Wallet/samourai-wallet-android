package com.samourai.wallet.util.tech;

import android.graphics.Bitmap;

public class BitmapHelper {

    private BitmapHelper() {}

    public static Bitmap cropBitmap(final Bitmap originalBitmap, final int borderSize) {
        return Bitmap.createBitmap(
                originalBitmap,
                borderSize,
                borderSize,
                originalBitmap.getWidth() - 2 * borderSize,
                originalBitmap.getHeight() - 2 * borderSize);
    }
}
