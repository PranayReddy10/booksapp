package com.jntuh.util;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Gentle, looping drift for the decorative background blobs on the auth (Mesh)
 * screens. Kept deliberately slow and low-amplitude so it reads as ambient motion,
 * never distracting during typing. Call {@link #floatView} once per blob.
 */
public final class AuthAnimator {

    private AuthAnimator() {
    }

    /**
     * Drifts a view in a slow diagonal loop.
     *
     * @param view    the blob ImageView
     * @param dx      horizontal travel in px
     * @param dy      vertical travel in px
     * @param durMs   one-way duration; the reverse doubles the full cycle
     */
    public static ObjectAnimator floatView(View view, float dx, float dy, long durMs) {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, dx);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, dy);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY);
        anim.setDuration(durMs);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        return anim;
    }
}
