package com.example.maimemo.ui.viewmodel;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.maimemo.R;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.widget.ForgettingCurveView;

import java.util.Locale;

/** 遗忘曲线：理论线用于参考，个人线、阶段保持率和待到期数量均从本地 FSRS 状态计算。 */
public class StatsForgettingFragment extends Fragment {
    private static final double[] STAGE_DAYS = {20.0 / 1440.0, 1.0 / 24.0, 1.0};
    private static final int[] STAGE_TEXT_IDS = {R.id.tv_retention_20m, R.id.tv_retention_1h, R.id.tv_retention_1d};
    private static final int[] STAGE_BAR_IDS = {R.id.bar_retention_20m, R.id.bar_retention_1h, R.id.bar_retention_1d};

    public StatsForgettingFragment() { super(R.layout.fragment_stats_forgetting); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh(view);
        view.findViewById(R.id.btn_review_now).setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), StudyActivity.class)));
    }

    @Override public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) refresh(view);
    }

    private void refresh(View root) {
        float[] theory = {100f, 58f, 44f, 36f, 33f, 30f, 28f};
        ((ForgettingCurveView) root.findViewById(R.id.curve_view)).setData(theory, WordRepository.getRetentionCurve(requireContext()));
        ((TextView) root.findViewById(R.id.tv_current_retention)).setText(WordRepository.getCurrentRetentionPercent(requireContext()) + "%");
        ((TextView) root.findViewById(R.id.tv_avg_interval)).setText(String.format(Locale.getDefault(), "%.1f天", WordRepository.getAverageReviewInterval(requireContext())));
        ((TextView) root.findViewById(R.id.tv_total_reviews)).setText(String.valueOf(WordRepository.getTotalReviewCount(requireContext())));
        for (int i = 0; i < STAGE_DAYS.length; i++) {
            int percent = WordRepository.getRetentionAtElapsedDays(requireContext(), STAGE_DAYS[i]);
            ((TextView) root.findViewById(STAGE_TEXT_IDS[i])).setText(percent + "%");
            setBarPercent(root, STAGE_BAR_IDS[i], percent);
        }
        int due = WordRepository.getDueInNext24HoursCount(requireContext());
        TextView dueHint = root.findViewById(R.id.tv_due_24h_hint);
        if (dueHint != null) dueHint.setText("未来 24 小时内将有 " + due + " 个单词进入复习时间");
    }

    private void setBarPercent(View root, int fillId, int percent) {
        View fill = root.findViewById(fillId);
        if (fill == null || !(fill.getParent() instanceof LinearLayout)) return;
        LinearLayout bar = (LinearLayout) fill.getParent();
        if (bar.getChildCount() < 2) return;
        LinearLayout.LayoutParams first = (LinearLayout.LayoutParams) bar.getChildAt(0).getLayoutParams();
        LinearLayout.LayoutParams second = (LinearLayout.LayoutParams) bar.getChildAt(1).getLayoutParams();
        first.weight = Math.max(0.01f, percent);
        second.weight = Math.max(0.01f, 100 - percent);
        bar.getChildAt(0).setLayoutParams(first);
        bar.getChildAt(1).setLayoutParams(second);
    }
}
