package com.example.maimemo.ui;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.database.sqlite.SQLiteException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.maimemo.R;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.data.database.LocalDatabase;
import com.example.maimemo.util.ReminderScheduler;
import com.example.maimemo.ui.viewmodel.HomeFragment;
import com.example.maimemo.ui.viewmodel.MineFragment;
import com.example.maimemo.ui.viewmodel.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主界面：底部三项导航（首页 / 统计 / 我的），分别对应三个 Fragment。
 */
public class MainActivity extends AppCompatActivity {

    private android.view.View loadingOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.view.View appRoot = findViewById(R.id.main_root);
        com.example.maimemo.util.SystemBarUtils.apply(this, appRoot);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int itemId = item.getItemId();
            if (itemId == R.id.tab_home) fragment = new HomeFragment();
            else if (itemId == R.id.tab_stats) fragment = new StatsFragment();
            else if (itemId == R.id.tab_mine) fragment = new MineFragment();
            else return false;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host, fragment).commit();
            return true;
        });

        if (savedInstanceState == null) {
            showLoading();
            // 首次复制约 27 MB 的内置词库必须放在后台线程，避免主线程卡死/ANR。
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                Exception failure = null;
                try { LocalDatabase.get(getApplicationContext()); }
                catch (Exception e) { failure = e; }
                final Exception error = failure;
                runOnUiThread(() -> {
                    hideLoading();
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("词库初始化失败")
                                .setMessage("应用无法打开本地词库。请检查存储空间后重试。\n\n" + error.getMessage())
                                .setCancelable(false)
                                .setPositiveButton("重试", (d, w) -> recreate())
                                .setNegativeButton("退出", (d, w) -> finish())
                                .show();
                        return;
                    }
                    ReminderScheduler.schedule(getApplicationContext());
                    bottomNav.setSelectedItemId(R.id.tab_home);
                });
            });
        } else if (getSupportFragmentManager().findFragmentById(R.id.nav_host) == null) {
            bottomNav.setSelectedItemId(R.id.tab_home);
        }
    }

    private void showLoading() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.rgb(248, 250, 248));
        ProgressBar progress = new ProgressBar(this);
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.CENTER);
        progressLp.bottomMargin = dp(28);
        overlay.addView(progress, progressLp);
        TextView label = new TextView(this);
        label.setText("正在准备本地词库，请稍候…");
        label.setTextColor(Color.DKGRAY);
        label.setTextSize(15);
        label.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        labelLp.topMargin = dp(64);
        overlay.addView(label, labelLp);
        root.addView(overlay, new android.view.ViewGroup.LayoutParams(-1, -1));
        loadingOverlay = overlay;
    }

    private void hideLoading() {
        if (loadingOverlay != null && loadingOverlay.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) loadingOverlay.getParent()).removeView(loadingOverlay);
        }
        loadingOverlay = null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

}