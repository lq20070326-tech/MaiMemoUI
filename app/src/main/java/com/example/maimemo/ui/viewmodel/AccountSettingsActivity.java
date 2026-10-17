package com.example.maimemo.ui.viewmodel;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.util.UiUtils;

import java.io.File;

/** 账号与设置：本地开关持久化、缓存清理等设备端功能。 */
public class AccountSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_account_settings);
        com.example.maimemo.util.SystemBarUtils.apply(this, findViewById(android.R.id.content));
        UiUtils.setupTopBar(this, "账号与设置");

        UiUtils.bindRow(this, R.id.row_profile, R.drawable.ic_nav_mine, "个人资料",
                AppSettings.getString(this, "profile_name", "必须背用户 007"));
        UiUtils.bindRow(this, R.id.row_security, R.drawable.ic_lock, "账号与安全", "本机模式");
        UiUtils.bindRow(this, R.id.row_third, R.drawable.ic_share, "第三方账号绑定", "未绑定");
        UiUtils.bindRow(this, R.id.row_password, R.drawable.ic_lock, "修改密码", "");

        UiUtils.bindSwitchRow(this, R.id.row_notify_switch, R.drawable.ic_notification, "消息通知",
                AppSettings.getBoolean(this, AppSettings.MESSAGE_NOTIFICATIONS, true));
        SwitchCompat notificationSwitch = findViewById(R.id.row_notify_switch).findViewById(R.id.sw_row);
        notificationSwitch.setOnCheckedChangeListener((button, checked) ->
                AppSettings.putBoolean(this, AppSettings.MESSAGE_NOTIFICATIONS, checked));

        UiUtils.bindRow(this, R.id.row_theme, R.drawable.ic_palette, "主题皮肤",
                AppSettings.getBoolean(this, AppSettings.DARK_MODE, false) ? "深色" : "浅色");
        UiUtils.bindRow(this, R.id.row_privacy, R.drawable.ic_lock, "隐私设置", "本地存储");
        UiUtils.bindRow(this, R.id.row_clean, R.drawable.ic_clean, "清理缓存", formatSize(cacheSize()));
        UiUtils.bindRow(this, R.id.row_update, R.drawable.ic_info, "检查更新", "v1.0.0");

        bindToast(R.id.row_profile, "个人资料");
        bindToast(R.id.row_security, "账号与安全");
        bindToast(R.id.row_third, "第三方账号绑定");
        bindToast(R.id.row_password, "修改密码");
        findViewById(R.id.row_theme).setOnClickListener(v -> chooseTheme());
        findViewById(R.id.row_privacy).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("隐私设置")
                .setMessage("当前版本采用本地模式。词库、学习记录、收藏和设置保存在本机应用私有目录，不会自动上传到服务器。卸载应用可能导致本地数据被删除。")
                .setPositiveButton("知道了", null).show());
        bindToast(R.id.row_update, "当前为本地版本，暂不支持在线更新");

        findViewById(R.id.row_clean).setOnClickListener(v -> confirmCleanCache());
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> editProfile());
        findViewById(R.id.btn_logout).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("本地模式")
                .setMessage("当前版本没有接入在线账号，暂时不需要退出登录。")
                .setPositiveButton("知道了", null).show());
    }

    private void editProfile() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("请输入昵称");
        input.setText(AppSettings.getString(this, "profile_name", "必须背用户 007"));
        input.setSelection(input.getText().length());
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        new AlertDialog.Builder(this).setTitle("编辑昵称").setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "必须背用户";
                    AppSettings.putString(this, "profile_name", name);
                    UiUtils.bindRow(this, R.id.row_profile, R.drawable.ic_nav_mine, "个人资料", name);
                    UiUtils.toast(this, "昵称已保存在本机");
                }).show();
    }

    private void chooseTheme() {
        boolean dark = AppSettings.getBoolean(this, AppSettings.DARK_MODE, false);
        android.widget.LinearLayout panel = new android.widget.LinearLayout(this);
        panel.setOrientation(android.widget.LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(8), dp(18), dp(8));
        TextView subtitle = new TextView(this); subtitle.setText("选择更适合当前环境的界面外观"); subtitle.setTextSize(13);
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); panel.addView(subtitle);
        final int[] selected = {dark ? 1 : 0};
        TextView[] marks = new TextView[2];
        String[] labels = {"浅色模式", "深色模式"}; String[] hints = {"明亮、清爽，适合白天使用", "降低界面亮度，适合夜间使用"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            android.widget.LinearLayout row = new android.widget.LinearLayout(this); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12)); row.setBackgroundResource(R.drawable.bg_card);
            android.widget.LinearLayout words = new android.widget.LinearLayout(this); words.setOrientation(android.widget.LinearLayout.VERTICAL);
            TextView name = new TextView(this); name.setText(labels[i]); name.setTextSize(15); name.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); name.setTypeface(null, android.graphics.Typeface.BOLD); words.addView(name);
            TextView hint = new TextView(this); hint.setText(hints[i]); hint.setTextSize(12); hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); words.addView(hint);
            row.addView(words, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
            marks[i] = new TextView(this); marks[i].setText(i == selected[0] ? "✓" : "○"); marks[i].setTextSize(18); marks[i].setTextColor(ContextCompat.getColor(this, i == selected[0] ? R.color.brand_green : R.color.text_hint)); row.addView(marks[i]);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(8); panel.addView(row, lp);
            row.setOnClickListener(v -> { selected[0] = index; for (int j = 0; j < marks.length; j++) { marks[j].setText(j == index ? "✓" : "○"); marks[j].setTextColor(ContextCompat.getColor(this, j == index ? R.color.brand_green : R.color.text_hint)); } });
        }
        new AlertDialog.Builder(this).setTitle("主题皮肤").setView(panel)
                .setNegativeButton("取消", null).setPositiveButton("应用主题", (dialog, which) -> {
                    boolean useDark = selected[0] == 1;
                    AppSettings.putBoolean(this, AppSettings.DARK_MODE, useDark);
                    UiUtils.bindRow(this, R.id.row_theme, R.drawable.ic_palette, "主题皮肤", useDark ? "深色" : "浅色");
                    AppSettings.applyTheme(this);
                }).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void bindToast(int rowId, String message) {
        View row = findViewById(rowId);
        if (row != null) row.setOnClickListener(v -> UiUtils.toast(this, message));
    }

    private void confirmCleanCache() {
        long size = cacheSize();
        new AlertDialog.Builder(this)
                .setTitle("清理缓存")
                .setMessage("将清理约 " + formatSize(size) + " 的临时缓存，不会删除词库数据库和学习设置。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清理", (dialog, which) -> {
                    deleteChildren(getCacheDir());
                    File external = getExternalCacheDir();
                    if (external != null) deleteChildren(external);
                    UiUtils.bindRow(this, R.id.row_clean, R.drawable.ic_clean, "清理缓存", formatSize(cacheSize()));
                    UiUtils.toast(this, "缓存已清理");
                }).show();
    }

    private long cacheSize() {
        long size = sizeOf(getCacheDir());
        File external = getExternalCacheDir();
        if (external != null) size += sizeOf(external);
        return size;
    }

    private long sizeOf(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) total += sizeOf(child);
        return total;
    }

    private void deleteChildren(File directory) {
        if (directory == null || !directory.exists()) return;
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) deleteChildren(child);
                // 只删除缓存目录中的内容，不碰 files/ 或 databases/。
                child.delete();
            }
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
