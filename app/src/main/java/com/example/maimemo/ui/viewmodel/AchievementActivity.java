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

    /** 勋章数据：名称 / 说明 / 是否已获得 */
    private final String[][] medals = {
            {"初来乍到", "完成首次学习", "1"},
            {"坚持一周", "连续打卡 7 天", "1"},
            {"坚持一月", "连续打卡 30 天", "1"},
            {"百词斩", "累计学习 100 词", "1"},
            {"千词斩", "累计学习 1000 词", "1"},
            {"万词斩", "累计学习 10000 词", "0"},
            {"早起鸟", "在 6-8 点学习", "1"},
            {"夜猫子", "在 22 点后学习", "1"},
            {"复习达人", "累计复习 1000 次", "1"},
            {"满分日", "单日正确率 100%", "1"},
            {"词书全通", "学完一本词书", "0"},
            {"记忆大师", "记忆持久度超过 60 天", "0"},
            {"一月无断", "连续打卡 30 天不间断", "0"},
            {"速度之星", "单日学习 300 词", "0"},
            {"收藏家", "收藏 500 个单词", "1"},
            {"分享达人", "邀请 5 位好友", "0"},
            {"阅读者", "完成 100 个例句", "1"},
            {"挑战者", "完成一次每日挑战", "1"}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_achievement);
        com.example.maimemo.util.SystemBarUtils.apply(this, findViewById(android.R.id.content));
        UiUtils.setupTopBar(this, "成就勋章");
        buildMedalWall();
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
        boolean earned = "1".equals(medal[2]);

        ImageView icon = item.findViewById(R.id.iv_medal);
        TextView name = item.findViewById(R.id.tv_medal_name);
        TextView desc = item.findViewById(R.id.tv_medal_desc);

        name.setText(medal[0]);
        desc.setText(earned ? "已获得" : "未解锁");

        int iconTint = ContextCompat.getColor(this, earned ? R.color.brand_green : R.color.text_hint);
        int bgTint = ContextCompat.getColor(this, earned ? R.color.brand_green_light : R.color.control_bg);
        icon.setImageTintList(ColorStateList.valueOf(iconTint));
        icon.setBackgroundTintList(ColorStateList.valueOf(bgTint));

        item.setOnClickListener(v -> UiUtils.toast(this,
                medal[0] + "：" + medal[1] + (earned ? "（已获得）" : "（未解锁）")));
    }
}