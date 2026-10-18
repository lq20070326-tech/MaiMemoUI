package com.example.maimemo.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 本地应用设置。所有配置保存在 App 私有 SharedPreferences 中，不依赖网络或后端服务。
 */
public final class AppSettings {
    private static final String PREFS_NAME = "maimemo_settings";

    public static final String DAILY_NEW_WORDS = "daily_new_words";
    public static final String DAILY_REVIEW_LIMIT = "daily_review_limit";
    public static final String WORD_LIMIT = "word_limit";
    public static final String STUDY_ORDER = "study_order";
    public static final String STUDY_MODE = "study_mode";
    public static final String MEMORY_MODE = "memory_mode";
    public static final String REVIEW_MODE = "review_mode";
    public static final String AUTO_SPEAK = "auto_speak";
    public static final String ACCENT = "accent";
    public static final String REMINDER_ENABLED = "reminder_enabled";
    public static final String REMINDER_TIME = "reminder_time";
    public static final String SHOW_EXAMPLE = "show_example";
    public static final String SHOW_PHONETIC = "show_phonetic";
    public static final String DARK_MODE = "dark_mode";
    public static final String MESSAGE_NOTIFICATIONS = "message_notifications";
    public static final String AUTO_GROUP_SIZE = "auto_group_size";
    public static final String AUTO_FAMILIARITY_ROUNDS = "auto_familiarity_rounds";
    public static final String AUTO_GROUP_QUIZ_TYPE = "auto_group_quiz_type";

    private AppSettings() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return prefs(context).getInt(key, defaultValue);
    }

    public static String getString(Context context, String key, String defaultValue) {
        return prefs(context).getString(key, defaultValue);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return prefs(context).getBoolean(key, defaultValue);
    }

    public static void putInt(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }

    public static void putString(Context context, String key, String value) {
        prefs(context).edit().putString(key, value).apply();
    }

    public static void putBoolean(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    /** 应用全局浅色/深色主题。 */
    public static void applyTheme(Context context) {
        boolean dark = getBoolean(context, DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
