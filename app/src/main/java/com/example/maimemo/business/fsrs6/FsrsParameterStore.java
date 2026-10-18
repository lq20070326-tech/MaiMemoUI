package com.example.maimemo.business.fsrs6;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the currently fitted FSRS-6 weight vector locally. */
public final class FsrsParameterStore {
    private static final String PREFS = "fsrs_parameters";
    private static final String KEY_PREFIX = "w_";
    private FsrsParameterStore() { }

    public static double[] getWeights(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double[] defaults = Fsrs6Scheduler.DEFAULT_WEIGHTS;
        double[] values = new double[defaults.length];
        for (int i = 0; i < values.length; i++) {
            long bits = prefs.getLong(KEY_PREFIX + i, Double.doubleToRawLongBits(defaults[i]));
            values[i] = Double.longBitsToDouble(bits);
        }
        return values;
    }

    public static void setWeights(Context context, double[] weights) {
        if (weights == null || weights.length != Fsrs6Scheduler.DEFAULT_WEIGHTS.length)
            throw new IllegalArgumentException("FSRS-6 必须包含 21 个 w 参数");
        SharedPreferences.Editor editor = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < weights.length; i++) {
            if (!Double.isFinite(weights[i]) || weights[i] <= 0 || weights[i] > 100)
                throw new IllegalArgumentException("w" + i + " 参数必须是 0 到 100 之间的有限正数");
            editor.putLong(KEY_PREFIX + i, Double.doubleToRawLongBits(weights[i]));
        }
        editor.apply();
    }

    public static void reset(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
