package com.example.maimemo.ui.viewmodel;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.example.maimemo.R;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.util.ReminderScheduler;
import com.example.maimemo.util.UiUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 设置采用草稿模式：只有点击“保存设置”才写入本机配置。 */
public class StudySettingsActivity extends AppCompatActivity {
    private static final int DEFAULT_DAILY = 50;
    private static final int DEFAULT_REVIEW_LIMIT = 200;
    private static final int DEFAULT_WORD_LIMIT = 8000;
    private final Map<String, Object> draft = new LinkedHashMap<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_study_settings);
        com.example.maimemo.util.SystemBarUtils.apply(this, findViewById(android.R.id.content));
        UiUtils.setupTopBar(this, "单词背诵设置");
        loadDraft();
        bindRows();
        bindSwitches();
        bindClickActions();
    }

    private void loadDraft() {
        draft.put(AppSettings.DAILY_NEW_WORDS, AppSettings.getInt(this, AppSettings.DAILY_NEW_WORDS, DEFAULT_DAILY));
        draft.put(AppSettings.DAILY_REVIEW_LIMIT, AppSettings.getInt(this, AppSettings.DAILY_REVIEW_LIMIT, DEFAULT_REVIEW_LIMIT));
        draft.put(AppSettings.WORD_LIMIT, AppSettings.getInt(this, AppSettings.WORD_LIMIT, DEFAULT_WORD_LIMIT));
        draft.put(AppSettings.STUDY_ORDER, AppSettings.getString(this, AppSettings.STUDY_ORDER, "按词频"));
        draft.put(AppSettings.STUDY_MODE, AppSettings.getString(this, AppSettings.STUDY_MODE, "手动"));
        draft.put(AppSettings.MEMORY_MODE, AppSettings.getString(this, AppSettings.MEMORY_MODE, "必须背记忆法"));
        draft.put(AppSettings.REVIEW_MODE, AppSettings.getString(this, AppSettings.REVIEW_MODE, "智能复习"));
        draft.put(AppSettings.ACCENT, AppSettings.getString(this, AppSettings.ACCENT, "美式"));
        draft.put(AppSettings.REMINDER_TIME, AppSettings.getString(this, AppSettings.REMINDER_TIME, "20:00"));
        draft.put(AppSettings.AUTO_SPEAK, AppSettings.getBoolean(this, AppSettings.AUTO_SPEAK, true));
        draft.put(AppSettings.REMINDER_ENABLED, AppSettings.getBoolean(this, AppSettings.REMINDER_ENABLED, true));
        draft.put(AppSettings.SHOW_EXAMPLE, AppSettings.getBoolean(this, AppSettings.SHOW_EXAMPLE, true));
        draft.put(AppSettings.SHOW_PHONETIC, AppSettings.getBoolean(this, AppSettings.SHOW_PHONETIC, true));
        draft.put(AppSettings.DARK_MODE, AppSettings.getBoolean(this, AppSettings.DARK_MODE, false));
    }

    private void bindRows() {
        bindTextRow(R.id.row_daily, R.drawable.ic_target, "每日学习量", draft.get(AppSettings.DAILY_NEW_WORDS) + " 词/天");
        bindTextRow(R.id.row_review_limit, R.drawable.ic_clock, "每日复习上限", draft.get(AppSettings.DAILY_REVIEW_LIMIT) + " 词/天");
        bindTextRow(R.id.row_word_limit, R.drawable.ic_book, "词上限", draft.get(AppSettings.WORD_LIMIT) + " 词");
        bindTextRow(R.id.row_order, R.drawable.ic_sync, "学习顺序", (String) draft.get(AppSettings.STUDY_ORDER));
        bindTextRow(R.id.row_study_mode, R.drawable.ic_target, "学习模式", (String) draft.get(AppSettings.STUDY_MODE));
        bindTextRow(R.id.row_memory_mode, R.drawable.ic_medal, "记忆模式", (String) draft.get(AppSettings.MEMORY_MODE));
        bindTextRow(R.id.row_review_mode, R.drawable.ic_sync, "复习模式", (String) draft.get(AppSettings.REVIEW_MODE));
        bindTextRow(R.id.row_accent, R.drawable.ic_volume, "发音口音", (String) draft.get(AppSettings.ACCENT));
        bindTextRow(R.id.row_remind_time, R.drawable.ic_clock, "提醒时间", (String) draft.get(AppSettings.REMINDER_TIME));
    }

    private void bindTextRow(int id, int icon, String title, String value) {
        UiUtils.bindRow(this, id, icon, title, value);
    }

    private void bindSwitches() {
        bindSwitch(R.id.row_auto_speak, R.drawable.ic_volume, "自动发音", AppSettings.AUTO_SPEAK);
        bindSwitch(R.id.row_remind_switch, R.drawable.ic_notification, "每日学习提醒", AppSettings.REMINDER_ENABLED);
        bindSwitch(R.id.row_show_example, R.drawable.ic_notebook, "卡片显示例句", AppSettings.SHOW_EXAMPLE);
        bindSwitch(R.id.row_show_phonetic, R.drawable.ic_volume, "显示音标", AppSettings.SHOW_PHONETIC);
        bindSwitch(R.id.row_dark_mode, R.drawable.ic_palette, "深色模式", AppSettings.DARK_MODE);
    }

    private void bindSwitch(int rowId, int icon, String title, String key) {
        boolean checked = (Boolean) draft.get(key);
        UiUtils.bindSwitchRow(this, rowId, icon, title, checked);
        View row = findViewById(rowId);
        if (row == null) return;
        SwitchCompat toggle = row.findViewById(R.id.sw_row);
        if (toggle == null) return;
        // 先移除初始化阶段可能设置的监听器，再设置唯一监听器；不在这里持久化或应用主题。
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, value) -> draft.put(key, value));
        toggle.setOnClickListener(v -> v.setPressed(false));
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
    }

    private void bindClickActions() {
        setRowClick(R.id.row_daily, v -> chooseNumber("每日学习量", AppSettings.DAILY_NEW_WORDS, DEFAULT_DAILY,
                new int[]{10, 20, 30, 50, 80, 100, 150, 200}, " 词/天", R.id.row_daily));
        setRowClick(R.id.row_review_limit, v -> chooseNumber("每日复习上限", AppSettings.DAILY_REVIEW_LIMIT, DEFAULT_REVIEW_LIMIT,
                new int[]{50, 100, 200, 300, 500, 800, 1000}, " 词/天", R.id.row_review_limit));
        setRowClick(R.id.row_word_limit, v -> chooseNumber("词上限", AppSettings.WORD_LIMIT, DEFAULT_WORD_LIMIT,
                new int[]{1000, 3000, 5000, 8000, 10000, 20000, 50000}, " 词", R.id.row_word_limit));
        setRowClick(R.id.row_order, v -> chooseString("学习顺序", AppSettings.STUDY_ORDER,
                new String[]{"按词频", "按词书顺序", "随机学习"}, R.id.row_order));
        setRowClick(R.id.row_study_mode, v -> chooseString("学习模式", AppSettings.STUDY_MODE,
                new String[]{"全自动", "手动"}, R.id.row_study_mode));
        setRowClick(R.id.row_memory_mode, v -> chooseString("记忆模式", AppSettings.MEMORY_MODE,
                new String[]{"必须背记忆法", "标准模式"}, R.id.row_memory_mode));
        setRowClick(R.id.row_review_mode, v -> chooseString("复习模式", AppSettings.REVIEW_MODE,
                new String[]{"智能复习", "按学习顺序", "先易后难"}, R.id.row_review_mode));
        setRowClick(R.id.row_accent, v -> chooseString("发音口音", AppSettings.ACCENT,
                new String[]{"美式", "英式"}, R.id.row_accent));
        setRowClick(R.id.row_remind_time, v -> chooseReminderTime());
        View save = findViewById(R.id.btn_save);
        if (save != null) save.setOnClickListener(v -> saveDraft());
    }

    private void setRowClick(int id, View.OnClickListener listener) {
        View row = findViewById(id);
        if (row == null) return;
        row.setClickable(true);
        row.setOnClickListener(listener);
        bindChildClicks(row, listener);
    }

    private void bindChildClicks(View parent, View.OnClickListener listener) {
        if (!(parent instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) parent;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setOnClickListener(listener);
            bindChildClicks(child, listener);
        }
    }

    private void chooseNumber(String title, String key, int defaultValue, int[] choices, String suffix, int rowId) {
        String[] labels = new String[choices.length];
        int current = (Integer) draft.get(key), selected = 0;
        for (int i = 0; i < choices.length; i++) { labels[i] = choices[i] + suffix; if (choices[i] == current) selected = i; }
        final int[] selectedIndex = {selected};
        showBeautifulChoice(title, "选择适合自己的学习节奏", labels, selected, index -> {
            selectedIndex[0] = index;
            int value = choices[index]; draft.put(key, value); setRowValue(rowId, value + suffix);
        });
    }

    private interface ChoiceCallback { void onSelected(int index); }

    private void showBeautifulChoice(String title, String subtitle, String[] choices, int selected, ChoiceCallback callback) {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(8), dp(18), dp(8));
        TextView sub = new TextView(this); sub.setText(subtitle); sub.setTextSize(13);
        sub.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        android.widget.LinearLayout.LayoutParams subLp = new android.widget.LinearLayout.LayoutParams(-1, -2); subLp.bottomMargin = dp(10); container.addView(sub, subLp);
        final int[] checked = {selected};
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setView(container)
                .setNegativeButton("取消", null).setPositiveButton("确定", (d, w) -> callback.onSelected(checked[0])).create();
        for (int i = 0; i < choices.length; i++) {
            final int index = i;
            android.widget.LinearLayout option = new android.widget.LinearLayout(this);
            option.setGravity(android.view.Gravity.CENTER_VERTICAL); option.setPadding(dp(14), dp(12), dp(14), dp(12));
            option.setBackgroundResource(R.drawable.bg_card);
            TextView label = new TextView(this); label.setText(choices[i]); label.setTextSize(15);
            label.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            option.addView(label, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
            TextView mark = new TextView(this); mark.setText(i == selected ? "✓" : "○"); mark.setTextSize(18);
            mark.setTextColor(androidx.core.content.ContextCompat.getColor(this, i == selected ? R.color.brand_green : R.color.text_hint));
            option.addView(mark); option.setTag(mark);
            android.widget.LinearLayout.LayoutParams optLp = new android.widget.LinearLayout.LayoutParams(-1, -2); optLp.bottomMargin = dp(7); container.addView(option, optLp);
            option.setOnClickListener(v -> {
                checked[0] = index;
                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    if (child.getTag() instanceof TextView) {
                        TextView m = (TextView) child.getTag(); boolean isChecked = j - 1 == index;
                        m.setText(isChecked ? "✓" : "○");
                        m.setTextColor(androidx.core.content.ContextCompat.getColor(this, isChecked ? R.color.brand_green : R.color.text_hint));
                    }
                }
            });
        }
        dialog.show();
    }

    private void chooseString(String title, String key, String[] choices, int rowId) {
        String current = (String) draft.get(key);
        int selected = 0;
        for (int i = 0; i < choices.length; i++) if (choices[i].equals(current)) selected = i;
        showBeautifulChoice(title, "轻点选项，确认后保存到设置草稿", choices, selected, index -> {
            draft.put(key, choices[index]); setRowValue(rowId, choices[index]);
        });
    }

    private void chooseReminderTime() {
        String value = (String) draft.get(AppSettings.REMINDER_TIME);
        String[] parts = value.split(":");
        int hour = parts.length == 2 ? parseInt(parts[0], 20) : 20;
        int minute = parts.length == 2 ? parseInt(parts[1], 0) : 0;
        new TimePickerDialog(this, (picker, h, m) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            draft.put(AppSettings.REMINDER_TIME, time);
            setRowValue(R.id.row_remind_time, time);
        }, hour, minute, true).show();
    }

    private void saveDraft() {
        boolean oldDark = AppSettings.getBoolean(this, AppSettings.DARK_MODE, false);
        for (Map.Entry<String, Object> entry : draft.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            if (value instanceof Boolean) AppSettings.putBoolean(this, key, (Boolean) value);
            else if (value instanceof Integer) AppSettings.putInt(this, key, (Integer) value);
            else AppSettings.putString(this, key, String.valueOf(value));
        }
        boolean remindersEnabled = (Boolean) draft.get(AppSettings.REMINDER_ENABLED);
        if (remindersEnabled) ReminderScheduler.schedule(this); else ReminderScheduler.cancel(this);
        UiUtils.toast(this, "设置已保存");
        boolean newDark = (Boolean) draft.get(AppSettings.DARK_MODE);
        // 保存成功后立即返回“我的”页面；主题切换在当前页面结束后执行，避免重建设置页。
        finish();
        if (oldDark != newDark) new android.os.Handler(getMainLooper()).postDelayed(
                () -> AppSettings.applyTheme(getApplicationContext()), 180);
    }

    private int parseInt(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (NumberFormatException ignored) { return fallback; }
    }

    private void setRowValue(int rowId, String value) {
        View row = findViewById(rowId);
        if (row == null) return;
        TextView text = row.findViewById(R.id.tv_row_value);
        if (text != null) text.setText(value);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

}
