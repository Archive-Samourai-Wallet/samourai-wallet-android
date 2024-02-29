package com.samourai.wallet.util.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.View;

public class ViewUtil {

    private ViewUtil() {}

    public static void animateChangeColor(
            final View view,
            final int colorFrom,
            final int colorTo,
            final int durationInMs) {

        final ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                colorFrom,
                colorTo);
        colorAnimation.setDuration(durationInMs);
        colorAnimation.addUpdateListener(animator -> view.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    public static void animateChangeColor(
            final ValueAnimator.AnimatorUpdateListener listener,
            final int colorFrom,
            final int colorTo,
            final int durationInMs) {

        final ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                colorFrom,
                colorTo);
        colorAnimation.setDuration(durationInMs);
        colorAnimation.addUpdateListener(listener);
        colorAnimation.start();
    }
}
