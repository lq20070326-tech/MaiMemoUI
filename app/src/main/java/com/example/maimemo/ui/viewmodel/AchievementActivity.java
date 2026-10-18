package com.example.maimemo.ui.viewmodel;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;
import com.example.maimemo.util.UiUtils;

/**
 * 成就勋章：总览进度 + 勋章墙（已获得高亮，未解锁置灰）。
 */
public class AchievementActivity extends AppCompatActivity {

    private static final int COLUMNS = 3;

    /** 成就只根据本机真实学习记录逐步解锁，不再预置已获得状态。 */
    private String[][] medals;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_achievement);
        com.example.maimemo.util.SystemBarUtils.apply(this, findViewById(android.R.id.content));
        UiUtils.setupTopBar(this, "成就勋章");
        loadAchievementProgress();
        buildMedalWall();
        updateAchievementSummary();
    }

    private void loadAchievementProgress() {
        int learned = com.example.maimemo.data.repository.WordRepository.getTotalLearnedWordCount(this);
        int streak = com.example.maimemo.data.repository.WordRepository.getStreakDays(this);
        int reviews = com.example.maimemo.data.repository.WordRepository.getTotalReviewEventCount(this);
        int favorites = com.example.maimemo.data.repository.WordRepository.getCategoryCount(this,
                com.example.maimemo.data.repository.WordRepository.FAVORITE);
        int todayLearned = com.example.maimemo.data.repository.WordRepository.getTodayLearnedCount(this);
        int todayAccuracy = com.example.maimemo.data.repository.WordRepository.getTodayRecallRatePercent(this);
        boolean earlyBird = com.example.maimemo.data.repository.WordRepository.hasLearningEventInHourRange(this, 6, 8);
        boolean nightOwl = com.example.maimemo.data.repository.WordRepository.hasLearningEventInHourRange(this, 22, 24);
        double maxStability = com.example.maimemo.data.repository.WordRepository.getMaximumStability(this);
        boolean completedBook = false;
        for (com.example.maimemo.data.repository.WordRepository.Book book :
                com.example.maimemo.data.repository.WordRepository.getBooks(this)) {
            if (book.total > 0 && book.learned >= book.total) { completedBook = true; break; }
        }
        // 格式：名称 / 解锁条件 / 当前进度 / 是否获得
        medals = new String[][] {
                {"初来乍到", "完成首次学习", learned + "/1", learned >= 1 ? "1" : "0"},
                {"坚持一周", "连续打卡 7 天", streak + "/7 天", streak >= 7 ? "1" : "0"},
                {"坚持一月", "连续打卡 30 天", streak + "/30 天", streak >= 30 ? "1" : "0"},
                {"百词斩", "累计学习 100 词", learned + "/100 词", learned >= 100 ? "1" : "0"},
                {"千词斩", "累计学习 1000 词", learned + "/1000 词", learned >= 1000 ? "1" : "0"},
                {"万词斩", "累计学习 10000 词", learned + "/10000 词", learned >= 10000 ? "1" : "0"},
                {"早起鸟", "在 6-8 点完成学习", earlyBird ? "已达成" : "等待 6-8 点学习", earlyBird ? "1" : "0"},
                {"夜猫子", "在 22 点后完成学习", nightOwl ? "已达成" : "等待 22 点后学习", nightOwl ? "1" : "0"},
                {"复习达人", "累计复习 1000 次", reviews + "/1000 次", reviews >= 1000 ? "1" : "0"},
                {"满分日", "单日正确率 100% 且有学习记录", todayAccuracy + "%", todayLearned > 0 && todayAccuracy == 100 ? "1" : "0"},
                {"词书全通", "学完一本词书", completedBook ? "已完成词书" : "继续学习词书", completedBook ? "1" : "0"},
                {"记忆大师", "记忆持久度超过 60 天", String.format(java.util.Locale.getDefault(), "最高 %.1f/60 天", maxStability), maxStability >= 60.0 ? "1" : "0"},
                {"一月无断", "连续打卡 30 天不间断", streak + "/30 天", streak >= 30 ? "1" : "0"},
                {"速度之星", "单日新学 300 词", todayLearned + "/300 词", todayLearned >= 300 ? "1" : "0"},
                {"收藏家", "收藏 500 个单词", favorites + "/500 词", favorites >= 500 ? "1" : "0"},
                {"分享达人", "邀请 5 位好友", "等待记录功能", "0"},
                {"阅读者", "完成 100 个例句", "等待例句统计", "0"},
                {"挑战者", "完成一次每日挑战", "等待挑战记录", "0"}
        };
    }

    private void updateAchievementSummary() {
        int earned = 0;
        for (String[] medal : medals) if ("1".equals(medal[3])) earned++;
        TextView summary = findViewById(R.id.tv_achievement_summary);
        if (summary != null) summary.setText("已解锁 " + earned + " / " + medals.length + " 项成就");
    }

    private void buildMedalWall() {
        LinearLayout container = findViewById(R.id.medal_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;

        for (int start = 0; start < medals.length; start += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int column = 0; column < COLUMNS; column++) {
                int index = start + column;
                View item = inflater.inflate(R.layout.item_achievement, row, false);
                item.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                if (index < medals.length) {
                    bindMedal(item, medals[index]);
                } else {
                    item.setVisibility(View.INVISIBLE);
                }
                row.addView(item);
            }

            container.addView(row);

            // 行间距
            View space = new View(this);
            space.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (4 * density)));
            container.addView(space);
        }
    }

    private void bindMedal(View item, String[] medal) {
        boolean earned = "1".equals(medal[3]);

        ImageView icon = item.findViewById(R.id.iv_medal);
        TextView name = item.findViewById(R.id.tv_medal_name);
        TextView desc = item.findViewById(R.id.tv_medal_desc);

        name.setText(medal[0]);
        desc.setText(earned ? "✓ 已获得" : medal[2]);

        int iconTint = ContextCompat.getColor(this, earned ? R.color.brand_green : R.color.text_hint);
        int bgTint = ContextCompat.getColor(this, earned ? R.color.brand_green_light : R.color.control_bg);
        icon.setImageTintList(ColorStateList.valueOf(iconTint));
        icon.setBackgroundTintList(ColorStateList.valueOf(bgTint));

        item.setOnClickListener(v -> UiUtils.toast(this,
                medal[0] + "：" + medal[1] + (earned ? "（已获得）" : "（未解锁，当前：" + medal[2] + "）")));
    }
}