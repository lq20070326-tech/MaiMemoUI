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
import com.example.maimemo.widget.BarChartView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** 记忆持久度页面：所有数字与趋势均从本地学习数据库实时计算。 */
public class StatsMemoryFragment extends Fragment {
    public StatsMemoryFragment() { super(R.layout.fragment_stats_memory); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh(view);
    }

    @Override public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) refresh(view);
    }

    private void refresh(View view) {
        double average = WordRepository.getAverageStability(requireContext());
        TextView avg = view.findViewById(R.id.tv_avg_stability);
        avg.setText(average <= 0 ? "0" : (average >= 100 ? String.format(Locale.getDefault(), "%.0f", average) : String.format(Locale.getDefault(), "%.1f", average)));
        TextView hint = view.findViewById(R.id.tv_stability_hint);
        int[] dist = WordRepository.getStabilityDistribution(requireContext());
        int total = dist[0] + dist[1] + dist[2];
        hint.setText(total == 0 ? "完成学习后，这里会显示你的真实记忆数据" : "已分析 " + total + " 个单词的记忆稳定性");
        setPercent(view, R.id.tv_long_term_percent, dist[2], total);
        setPercent(view, R.id.tv_mid_term_percent, dist[1], total);
        setPercent(view, R.id.tv_short_term_percent, dist[0], total);

        float[] values = WordRepository.getRecentAverageStability(requireContext(), 7);
        String[] labels = new String[7];
        Calendar day = Calendar.getInstance(); day.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.getDefault());
        for (int i = 0; i < 7; i++) { labels[i] = format.format(day.getTime()); day.add(Calendar.DAY_OF_MONTH, 1); }
        ((BarChartView) view.findViewById(R.id.chart_trend)).setData(values, labels);
        bindForgottenWords(view);
        bindSuggestions(view);
    }

    private void bindSuggestions(View root) {
        java.util.List<String> suggestions = WordRepository.getImprovementSuggestions(requireContext());
        int[] ids = {R.id.tv_suggestion_1, R.id.tv_suggestion_2, R.id.tv_suggestion_3};
        for (int i=0;i<ids.length;i++) {
            TextView tv=root.findViewById(ids[i]);
            tv.setText(i<suggestions.size()?"· "+suggestions.get(i):"· 当前没有更多建议，继续保持规律学习。");
        }
    }

    private void bindForgottenWords(View root) {
        java.util.List<WordRepository.ForgottenWord> words = WordRepository.getMostForgottenWords(requireContext(), 3);
        int[] wordIds = {R.id.tv_forget_word_1, R.id.tv_forget_word_2, R.id.tv_forget_word_3};
        int[] countIds = {R.id.tv_forget_count_1, R.id.tv_forget_count_2, R.id.tv_forget_count_3};
        for (int i = 0; i < wordIds.length; i++) {
            TextView wordView = root.findViewById(wordIds[i]);
            TextView countView = root.findViewById(countIds[i]);
            if (i < words.size()) {
                WordRepository.ForgottenWord item = words.get(i);
                wordView.setText(item.word.headWord + " · " + WordRepository.displayBookName(item.word.bookName));
                countView.setText("遗忘 " + item.lapses + " 次");
            } else {
                wordView.setText("暂无数据");
                countView.setText("遗忘 0 次");
            }
        }
    }

    private void setPercent(View root, int textId, int count, int total) {
        TextView label = root.findViewById(textId);
        if (label != null) label.setText((total == 0 ? 0 : Math.round(count * 100f / total)) + "%");
        if (label == null || !(label.getParent() instanceof ViewGroup)) return;
        ViewGroup row = (ViewGroup) label.getParent();
        // 该行结构为名称、条形容器、百分比；将条形比例同步为数据库计算结果。
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout bars = (LinearLayout) child;
                if (bars.getChildCount() >= 2) {
                    float percent = total == 0 ? 0 : Math.max(0, Math.min(100, count * 100f / total));
                    View first = bars.getChildAt(0), second = bars.getChildAt(1);
                    LinearLayout.LayoutParams p1 = (LinearLayout.LayoutParams) first.getLayoutParams();
                    LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) second.getLayoutParams();
                    p1.weight = percent; p2.weight = Math.max(0.01f, 100f - percent);
                    first.setLayoutParams(p1); second.setLayoutParams(p2);
                }
            }
        }
    }
}
