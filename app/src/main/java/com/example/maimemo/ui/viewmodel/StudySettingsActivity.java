package com.example.maimemo.ui.viewmodel;

import android.app.TimePickerDialog;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
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
import com.example.maimemo.shizuku.IKeepAliveService;
import com.example.maimemo.shizuku.KeepAliveUserService;

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
        bindAdvancedBackgroundActions();
        try { rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuPermissionListener); }
        catch (Throwable ignored) { }
    }

    private void loadDraft() {
        draft.put(AppSettings.AUTO_GROUP_SIZE, AppSettings.getInt(this, AppSettings.AUTO_GROUP_SIZE, 10));
        draft.put(AppSettings.AUTO_FAMILIARITY_ROUNDS, AppSettings.getInt(this, AppSettings.AUTO_FAMILIARITY_ROUNDS, 2));
        draft.put(AppSettings.AUTO_GROUP_QUIZ_TYPE, AppSettings.getString(this, AppSettings.AUTO_GROUP_QUIZ_TYPE, "英文选择题"));
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
        bindTextRow(R.id.row_group_size, R.drawable.ic_book, "自动学习每组单词数", draft.get(AppSettings.AUTO_GROUP_SIZE) + " 词/组");
        bindTextRow(R.id.row_familiarity_rounds, R.drawable.ic_sync, "新词熟悉轮数", draft.get(AppSettings.AUTO_FAMILIARITY_ROUNDS) + " 遍");
        bindTextRow(R.id.row_group_quiz_type, R.drawable.ic_target, "组末测试题型", (String) draft.get(AppSettings.AUTO_GROUP_QUIZ_TYPE));
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
        setRowClick(R.id.row_group_size, v -> chooseNumber("自动学习每组单词数", AppSettings.AUTO_GROUP_SIZE, 10, new int[]{5, 10, 15, 20, 25, 30, 40, 50}, " 词/组", R.id.row_group_size));
        setRowClick(R.id.row_familiarity_rounds, v -> chooseNumber("新词熟悉轮数", AppSettings.AUTO_FAMILIARITY_ROUNDS, 2, new int[]{1, 2}, " 遍", R.id.row_familiarity_rounds));
        setRowClick(R.id.row_group_quiz_type, v -> chooseString("组末测试题型", AppSettings.AUTO_GROUP_QUIZ_TYPE, new String[]{"英文选择题", "句子填空题"}, R.id.row_group_quiz_type));
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

    private static final int SHIZUKU_REQUEST_CODE = 7302;
    private final rikka.shizuku.Shizuku.OnRequestPermissionResultListener shizukuPermissionListener = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_REQUEST_CODE) return;
        runOnUiThread(() -> {
            if (grantResult == PackageManager.PERMISSION_GRANTED) applyShizukuWhitelist();
            else UiUtils.toast(this, "未授予 Shizuku 权限");
        });
    };

    private void bindAdvancedBackgroundActions() {
        View shizuku = findViewById(R.id.btn_shizuku_keepalive);
        if (shizuku != null) shizuku.setOnClickListener(v -> requestShizukuKeepAlive());
        View hide = findViewById(R.id.btn_hide_recents);
        if (hide != null) {
            boolean enabled = AppSettings.getBoolean(this, "hide_app_from_recents", false);
            ((TextView) hide).setText(enabled ? "已隐藏后台页面（点击恢复显示）" : "隐藏后台页面（最近任务）");
            hide.setOnClickListener(v -> {
                boolean next = !AppSettings.getBoolean(this, "hide_app_from_recents", false);
                AppSettings.putBoolean(this, "hide_app_from_recents", next);
                applyRecentTaskVisibility(next);
                ((TextView) hide).setText(next ? "已隐藏后台页面（点击恢复显示）" : "隐藏后台页面（最近任务）");
                UiUtils.toast(this, next ? "已尝试从最近任务中隐藏当前应用页面" : "已恢复最近任务页面显示");
            });
        }
    }

    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";

    private boolean isShizukuInstalled() {
        try {
            getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return false;
        }
    }

    private void openShizukuApp() {
        try {
            android.content.Intent launch = getPackageManager().getLaunchIntentForPackage(SHIZUKU_PACKAGE);
            if (launch != null) {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } else {
                UiUtils.toast(this, "已检测到 Shizuku，但无法直接打开；请从桌面启动 Shizuku");
            }
        } catch (Throwable error) {
            UiUtils.toast(this, "无法打开 Shizuku，请手动启动应用");
        }
    }

    private void requestShizukuKeepAlive() {
        try {
            // Package visibility and binder availability are different checks: the app can be installed
            // while its Shizuku service has not been started yet.
            if (!isShizukuInstalled()) {
                new AlertDialog.Builder(this).setTitle("未检测到 Shizuku 应用")
                        .setMessage("请先安装官方 Shizuku 应用。安装后打开 Shizuku，并按应用内提示通过无线调试或电脑启动服务，再返回这里授权。")
                        .setPositiveButton("知道了", null).show();
                return;
            }
            // Shizuku service is available: ask Shizuku to show its native Allow/Deny dialog.
            // The app cannot display or grant this privileged permission by itself.
            if (!rikka.shizuku.Shizuku.pingBinder()) {
                new AlertDialog.Builder(this)
                        .setTitle("需要启动 Shizuku")
                        .setMessage("请先在 Shizuku 中启动服务一次。服务运行后，回到这里再点一次，就会直接弹出“允许 / 拒绝”授权窗口。")
                        .setNegativeButton("稍后", null)
                        .setPositiveButton("打开 Shizuku", (dialog, which) -> openShizukuApp())
                        .show();
                return;
            }
            if (rikka.shizuku.Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                rikka.shizuku.Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
                return;
            }
            // Already authorized; no extra confirmation dialog is needed.
            applyShizukuWhitelist();
        } catch (Throwable error) {
            UiUtils.toast(this, "Shizuku 不可用：" + error.getClass().getSimpleName());
        }
    }

    private void applyShizukuWhitelist() {
        final rikka.shizuku.Shizuku.UserServiceArgs args =
                new rikka.shizuku.Shizuku.UserServiceArgs(
                        new ComponentName(getPackageName(), KeepAliveUserService.class.getName()))
                        .daemon(false)
                        .processNameSuffix("keepalive")
                        .tag("maimemo_keepalive")
                        .version(1);
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                new Thread(() -> {
                    String resultMessage;
                    try {
                        IKeepAliveService remote = IKeepAliveService.Stub.asInterface(binder);
                        resultMessage = remote.addDeviceIdleWhitelist(getPackageName());
                    } catch (RemoteException | RuntimeException error) {
                        resultMessage = "Shizuku 服务调用失败：" + error.getClass().getSimpleName();
                    }
                    final String message = resultMessage;
                    runOnUiThread(() -> new AlertDialog.Builder(StudySettingsActivity.this)
                            .setTitle("后台保活优化")
                            .setMessage(message + "\n\n该设置不能保证应用不被系统或厂商后台管理终止；如需恢复，可在系统电池/应用管理设置中调整。")
                            .setPositiveButton("确定", null).show());
                }, "shizuku-keepalive-call").start();
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                runOnUiThread(() -> UiUtils.toast(StudySettingsActivity.this, "Shizuku 服务已断开，请重试"));
            }
        };
        try {
            rikka.shizuku.Shizuku.bindUserService(args, connection);
        } catch (Throwable error) {
            UiUtils.toast(this, "无法启动 Shizuku 服务：" + error.getClass().getSimpleName());
        }
    }

    private void applyRecentTaskVisibility(boolean hide) {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (manager == null) return;
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                task.setExcludeFromRecents(hide);
            }
        } catch (Throwable error) {
            UiUtils.toast(this, "系统不允许修改最近任务显示");
        }
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
        boolean autoMode = "全自动".equals(String.valueOf(draft.get(AppSettings.STUDY_MODE)));
        boolean newDark = (Boolean) draft.get(AppSettings.DARK_MODE);
        if (autoMode) {
            String book = com.example.maimemo.data.repository.WordRepository.getCurrentBook(this);
            AppSettings.putString(this, "auto_study_book", book);
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 7301);
                return; // Start the session after the user has answered the permission prompt.
            }
            startAutoStudyAfterPermission();
        } else {
            com.example.maimemo.business.autostudy.AutoStudyForegroundService.stop(this);
            UiUtils.toast(this, "设置已保存");
            finish();
        }
        if (oldDark != newDark) new android.os.Handler(getMainLooper()).postDelayed(
                () -> AppSettings.applyTheme(getApplicationContext()), 180);
    }

    private void startAutoStudyAfterPermission() {
        com.example.maimemo.business.autostudy.AutoStudyForegroundService.start(this);
        boolean notificationGranted = android.os.Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        UiUtils.toast(this, notificationGranted
                ? "自动学习已启动：全屏弹窗 → 悬浮窗 → 通知栏"
                : "自动学习已启动，但通知权限未开启；请在系统设置中允许通知，否则三级降级通知不可见");
        boolean needsOverlay = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !android.provider.Settings.canDrawOverlays(this);
        finish();
        if (needsOverlay) {
            try {
                android.content.Intent overlaySettings = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName()));
                overlaySettings.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    try { startActivity(overlaySettings); } catch (Exception ignored) { }
                }, 250);
            } catch (Exception ignored) { }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7301) {
            startAutoStudyAfterPermission();
        }
    }

    @Override protected void onDestroy() {
        try { rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener); }
        catch (Throwable ignored) { }
        super.onDestroy();
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
