package com.example.maimemo.ui.viewmodel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;

import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.data.repository.WordRepository;

import com.example.maimemo.R;
import com.example.maimemo.util.UiUtils;

/**
 * 我的页：头部资料卡 + 今日学习 + 功能宫格 + 各类设置入口。
 */
public class MineFragment extends Fragment {

    private final int[] gridIds = {
            R.id.grid_1, R.id.grid_2, R.id.grid_3, R.id.grid_4,
            R.id.grid_5, R.id.grid_6, R.id.grid_7, R.id.grid_8
    };
    private final int[] gridIcons = {
            R.drawable.ic_book, R.drawable.ic_notebook, R.drawable.ic_star, R.drawable.ic_medal,
            R.drawable.ic_clock, R.drawable.ic_mall, R.drawable.ic_share, R.drawable.ic_target
    };
    private final String[] gridTitles = {
            "我的词书", "生词本", "我的收藏", "成就勋章",
            "学习记录", "必须背商店", "邀请好友", "FSRS数据与参数"
    };

    public MineFragment() {
        super(R.layout.fragment_mine);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindGrid(view);
        bindRows(view);
        refreshMineStats(view);

        view.findViewById(R.id.btn_start_study)
                .setOnClickListener(v -> UiUtils.toast(requireContext(), "开始今日学习"));
        
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            refreshMineStats(getView());
            UiUtils.bindRow(getView(), R.id.row_wordbook, R.drawable.ic_notebook, "词书与单词本", WordRepository.getBookCount(requireContext()) + " 本词书");
            UiUtils.bindRow(getView(), R.id.row_daily, R.drawable.ic_clock, "每日学习量",
                    AppSettings.getInt(requireContext(), AppSettings.DAILY_NEW_WORDS, 50) + " 词/天");
            UiUtils.bindRow(getView(), R.id.row_remind, R.drawable.ic_notification, "复习提醒",
                    AppSettings.getString(requireContext(), AppSettings.REMINDER_TIME, "20:00"));
        }
    }

    private void refreshMineStats(View root) {
        ((TextView) root.findViewById(R.id.tv_mine_total_learned)).setText(String.valueOf(WordRepository.getTotalLearnedCardCount(requireContext())));
        ((TextView) root.findViewById(R.id.tv_mine_mastered)).setText(String.valueOf(WordRepository.getMasteredCardCount(requireContext())));
        ((TextView) root.findViewById(R.id.tv_mine_streak)).setText(String.valueOf(WordRepository.getStreakDays(requireContext())));
        ((TextView) root.findViewById(R.id.tv_mine_today_new)).setText(String.valueOf(WordRepository.getTodayUniqueNewCardCount(requireContext())));
        ((TextView) root.findViewById(R.id.tv_mine_due)).setText(String.valueOf(WordRepository.getDueReviewCount(requireContext())));
        ((TextView) root.findViewById(R.id.tv_mine_accuracy)).setText(WordRepository.getTodayRecallRatePercent(requireContext()) + "%");
        ((TextView) root.findViewById(R.id.tv_mine_active_days)).setText(String.valueOf(WordRepository.getActiveStudyDays(requireContext())));
    }

    private void bindGrid(View view) {
        for (int i = 0; i < gridIds.length; i++) {
            View item = view.findViewById(gridIds[i]);
            ((ImageView) item.findViewById(R.id.iv_grid_icon)).setImageResource(gridIcons[i]);
            ((TextView) item.findViewById(R.id.tv_grid_label)).setText(gridTitles[i]);
            final int index = i;
            item.setOnClickListener(v -> openGrid(index, gridTitles[index]));
        }
    }

    private void openGrid(int index, String title) {
        switch (index) {
            case 0:
            case 1:
            case 2:
                startActivity(WordbookActivity.newIntent(requireContext(), index));
                break;
            case 3:
                startActivity(new Intent(requireContext(), AchievementActivity.class));
                break;
            case 4:
                startActivity(new Intent(requireContext(), StudyHistoryActivity.class));
                break;
            case 5:
                startActivity(new Intent(requireContext(), MustBuyShopActivity.class));
                break;
            case 7:
                startActivity(new Intent(requireContext(), FsrsDataActivity.class));
                break;
            default:
                UiUtils.toast(requireContext(), title + " 开发中");
                break;
        }
    }

    private void bindRows(View view) {
        UiUtils.bindRow(view, R.id.row_wordbook, R.drawable.ic_notebook, "词书与单词本", WordRepository.getBookCount(requireContext()) + " 本词书");
        UiUtils.bindRow(view, R.id.row_study_settings, R.drawable.ic_target, "单词背诵设置", "");
        UiUtils.bindRow(view, R.id.row_achievement, R.drawable.ic_medal, "成就勋章", "12 / 40");
        UiUtils.bindRow(view, R.id.row_daily, R.drawable.ic_clock, "每日学习量",
                AppSettings.getInt(requireContext(), AppSettings.DAILY_NEW_WORDS, 50) + " 词/天");
        UiUtils.bindRow(view, R.id.row_remind, R.drawable.ic_notification, "复习提醒",
                AppSettings.getString(requireContext(), AppSettings.REMINDER_TIME, "20:00"));

        view.findViewById(R.id.row_wordbook)
                .setOnClickListener(v -> startActivity(WordbookActivity.newIntent(requireContext(), 0)));
        view.findViewById(R.id.row_study_settings)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), StudySettingsActivity.class)));
        view.findViewById(R.id.row_achievement)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), AchievementActivity.class)));
        view.findViewById(R.id.row_daily)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), StudySettingsActivity.class)));
        view.findViewById(R.id.row_remind)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), StudySettingsActivity.class)));

        UiUtils.bindRow(view, R.id.row_account, R.drawable.ic_lock, "账号与设置", "");
        UiUtils.bindSwitchRow(view, R.id.row_notify, R.drawable.ic_notification, "消息通知",
                AppSettings.getBoolean(requireContext(), AppSettings.MESSAGE_NOTIFICATIONS, true));
        SwitchCompat notificationSwitch = view.findViewById(R.id.row_notify).findViewById(R.id.sw_row);
        notificationSwitch.setOnCheckedChangeListener((button, checked) ->
                AppSettings.putBoolean(requireContext(), AppSettings.MESSAGE_NOTIFICATIONS, checked));
        UiUtils.bindRow(view, R.id.row_theme, R.drawable.ic_palette, "主题皮肤",
                AppSettings.getBoolean(requireContext(), AppSettings.DARK_MODE, false) ? "深色" : "浅色");
        UiUtils.bindRow(view, R.id.row_clean, R.drawable.ic_clean, "清除缓存", "12.6 MB");
        UiUtils.bindRow(view, R.id.row_feedback, R.drawable.ic_feedback, "意见反馈", "");
        UiUtils.bindRow(view, R.id.row_about, R.drawable.ic_info, "关于必须背", "v4.8.0");

        view.findViewById(R.id.row_account)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), AccountSettingsActivity.class)));
        view.findViewById(R.id.row_theme).setOnClickListener(v -> {
            boolean dark = !AppSettings.getBoolean(requireContext(), AppSettings.DARK_MODE, false);
            AppSettings.putBoolean(requireContext(), AppSettings.DARK_MODE, dark);
            UiUtils.bindRow(view, R.id.row_theme, R.drawable.ic_palette, "主题皮肤", dark ? "深色" : "浅色");
            UiUtils.toast(requireContext(), dark ? "已切换深色模式" : "已切换浅色模式");
            AppSettings.applyTheme(requireContext());
        });
        view.findViewById(R.id.row_clean)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), AccountSettingsActivity.class)));
        view.findViewById(R.id.row_feedback)
                .setOnClickListener(v -> UiUtils.toast(requireContext(), "意见反馈开发中"));
        view.findViewById(R.id.row_about)
                .setOnClickListener(v -> UiUtils.toast(requireContext(), "必须背 v1.0.0"));
    }
}