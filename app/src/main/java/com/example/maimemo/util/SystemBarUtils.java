package com.example.maimemo.util;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Consistent edge-to-edge handling for Android 15+ (targetSdk 36). */
public final class SystemBarUtils {
    private SystemBarUtils() {}
    public static void apply(Activity activity, View contentRoot) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        final int left=contentRoot.getPaddingLeft(), top=contentRoot.getPaddingTop(), right=contentRoot.getPaddingRight(), bottom=contentRoot.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot, (v, windowInsets) -> {
            Insets bars=windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left, top + bars.top, right, bottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(contentRoot);
    }
}
