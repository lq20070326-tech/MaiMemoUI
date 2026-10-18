package com.example.maimemo.ui.viewmodel;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.maimemo.R;
import com.example.maimemo.widget.BarChartView;
import com.example.maimemo.data.repository.WordRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

/**
 * 统计板块二：学习情况。
 */
public class StatsStudyFragment extends Fragment {

    public StatsStudyFragment() {
        super(R.layout.fragment_stats_study);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh(view);
    }

    @Override public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) refresh(view);
    }

    private void refresh(View view) {
        int[] counts = WordRepository.getRecentDailyCounts(requireContext(), 7);
        float[] values = new float[7];
        String[] labels = new String[7];
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            values[i] = counts[i];
            labels[i] = format.format(day.getTime());
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        ((BarChartView) view.findViewById(R.id.chart_week)).setData(values, labels);
        int[] today = WordRepository.getTodayResultCounts(requireContext());
        int totalToday = today[0] + today[1] + today[2];
        ((android.widget.TextView) view.findViewById(R.id.tv_today_new)).setText(String.valueOf(WordRepository.getTodayLearnedCount(requireContext())));
        ((android.widget.TextView) view.findViewById(R.id.tv_today_review)).setText(String.valueOf(WordRepository.getTodayReviewCount(requireContext())));
        // FSRS 评分 2/3/4 均表示成功回忆；只有 Again(1) 计为未回忆成功。
        int accuracy = WordRepository.getTodayRecallRatePercent(requireContext());
        ((android.widget.TextView) view.findViewById(R.id.tv_accuracy)).setText(accuracy + "%");
        int weekTotal = 0;
        for (int count : counts) weekTotal += count;
        ((android.widget.TextView) view.findViewById(R.id.tv_week_total)).setText(String.valueOf(weekTotal));
        bindStudyPeriods(view);
        bindBookProgress(view);
        bindWeeklySummary(view);
    }

    private void bindStudyPeriods(View root) {
        int[] counts = WordRepository.getStudyTimeDistribution(requireContext());
        int total = counts[0] + counts[1] + counts[2];
        int[] textIds = {R.id.tv_period_morning, R.id.tv_period_noon, R.id.tv_period_evening};
        int[] weights = {0,0,0};
        for (int i=0;i<3;i++) {
            weights[i] = total == 0 ? 0 : Math.round(counts[i] * 100f / total);
            TextView label = root.findViewById(textIds[i]);
            label.setText(weights[i] + "%");
            View row = (View) label.getParent();
            for (int j=0;j<((ViewGroup)row).getChildCount();j++) {
                View child=((ViewGroup)row).getChildAt(j);
                if (child instanceof LinearLayout && child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout bar=(LinearLayout)child;
                    if (bar.getChildCount()>=2) {
                        LinearLayout.LayoutParams a=(LinearLayout.LayoutParams)bar.getChildAt(0).getLayoutParams();
                        LinearLayout.LayoutParams b=(LinearLayout.LayoutParams)bar.getChildAt(1).getLayoutParams();
                        a.weight=Math.max(0.01f,weights[i]); b.weight=Math.max(0.01f,100-weights[i]);
                        bar.getChildAt(0).setLayoutParams(a); bar.getChildAt(1).setLayoutParams(b);
                    }
                }
            }
        }
    }

    private void bindBookProgress(View root) {
        List<WordRepository.Book> books = WordRepository.getBookProgress(requireContext());
        int[] valueIds = {R.id.tv_book_progress_1, R.id.tv_book_progress_2, R.id.tv_book_progress_3};
        int[] slots = {0,1,2};
        // Update visible slots and their labels/progress bars without hard-coding any sample book names.
        for (int i=0;i<3;i++) {
            TextView value = root.findViewById(valueIds[i]);
            View row = (View) value.getParent();
            TextView name = null;
            for (int j=0;j<((ViewGroup)row).getChildCount();j++) if (((ViewGroup)row).getChildAt(j) instanceof TextView && ((ViewGroup)row).getChildAt(j)!=value) name=(TextView)((ViewGroup)row).getChildAt(j);
            if (i < books.size()) {
                WordRepository.Book book=books.get(i); value.setText(book.learned + " / " + book.total);
                if (name!=null) name.setText(WordRepository.displayBookName(book.name));
                View card = (View) row.getParent();
                int rowIndex=-1;
                for (int j=0;j<((ViewGroup)card).getChildCount();j++) if (((ViewGroup)card).getChildAt(j)==row) rowIndex=j;
                View bar = rowIndex>=0 && rowIndex+1<((ViewGroup)card).getChildCount()?((ViewGroup)card).getChildAt(rowIndex+1):null;
                if (bar instanceof LinearLayout && ((LinearLayout)bar).getChildCount()>=2) {
                    float pct=book.total<=0?0:Math.max(0,Math.min(100,book.learned*100f/book.total));
                    LinearLayout ll=(LinearLayout)bar; LinearLayout.LayoutParams a=(LinearLayout.LayoutParams)ll.getChildAt(0).getLayoutParams(); LinearLayout.LayoutParams b=(LinearLayout.LayoutParams)ll.getChildAt(1).getLayoutParams();
                    a.weight=Math.max(0.01f,pct); b.weight=Math.max(0.01f,100-pct); ll.getChildAt(0).setLayoutParams(a); ll.getChildAt(1).setLayoutParams(b);
                }
            } else { value.setText("0 / 0"); if(name!=null) name.setText("暂无词书数据"); }
        }
    }

    private void bindWeeklySummary(View root) {
        int[] totals=WordRepository.getThisAndLastWeekTotals(requireContext());
        int[] daily=WordRepository.getCurrentWeekDailyCounts(requireContext());
        int peak=0; for(int i=1;i<daily.length;i++) if(daily[i]>daily[peak]) peak=i;
        String[] dayNames={"周一","周二","周三","周四","周五","周六","周日"};
        TextView a=root.findViewById(R.id.tv_week_summary_1);
        if (totals[0]==0 && totals[1]==0) a.setText("· 本周尚无学习记录");
        else if (totals[1]==0) a.setText("· 本周共记录 " + totals[0] + " 次学习；上周暂无可比较数据");
        else { int delta=Math.round((totals[0]-totals[1])*100f/totals[1]); a.setText("· 本周共记录 " + totals[0] + " 次学习，较上周" + (delta>=0?"增加 ":"减少 ") + Math.abs(delta) + "%"); }
        root.<TextView>findViewById(R.id.tv_week_summary_2).setText(totals[0]==0?"· 每日学习峰值将在产生记录后显示":"· 本周学习最多的是"+dayNames[peak]+"，共"+daily[peak]+"次");
        int todayIndex = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        String advice = totals[0]==0 ? "· 完成第一次学习后，这里会提供个性化建议" : (daily[todayIndex]==0 ? "· 今天还没有学习记录，可以完成今日学习目标" : "· 已记录今天的学习，保持规律复习节奏");
        root.<TextView>findViewById(R.id.tv_week_summary_3).setText(advice);
    }
}
