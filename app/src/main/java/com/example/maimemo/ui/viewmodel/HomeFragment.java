package com.example.maimemo.ui.viewmodel;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.util.UiUtils;
import com.example.maimemo.widget.DayStat;
import com.example.maimemo.widget.RingCalendarView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 首页数据直接来自本地学习记录与内置词库。 */
public class HomeFragment extends Fragment {
    private RingCalendarView calendar;
    private TextView tvMonth, tvLegendMastered, tvLegendFuzzy, tvLegendUnknown, tvTodayCount;
    private TextView tvStatLearned, tvStatReview, tvStatStreak;
    private EditText searchInput;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        calendar = view.findViewById(R.id.calendar);
        tvMonth = view.findViewById(R.id.tv_month);
        tvLegendMastered = view.findViewById(R.id.tv_legend_mastered);
        tvLegendFuzzy = view.findViewById(R.id.tv_legend_fuzzy);
        tvLegendUnknown = view.findViewById(R.id.tv_legend_unknown);
        tvTodayCount = view.findViewById(R.id.tv_today_count);
        tvStatLearned = view.findViewById(R.id.tv_stat_learned);
        tvStatReview = view.findViewById(R.id.tv_stat_review);
        tvStatStreak = view.findViewById(R.id.tv_stat_streak);
        searchInput = view.findViewById(R.id.et_search);

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> { calendar.shiftMonth(-1); refresh(); });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> { calendar.shiftMonth(1); refresh(); });
        view.findViewById(R.id.btn_search).setOnClickListener(v -> doSearch());
        searchInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch(); return true;
            }
            return false;
        });
        tvTodayCount.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), StudyActivity.class)));
        refresh();
    }

    @Override public void onResume() { super.onResume(); if (getView() != null && calendar != null) refresh(); }

    private void refresh() {
        Calendar current = Calendar.getInstance();
        int year = calendar.currentYear();
        int month = calendar.currentMonth();
        int days = currentDays(year, month);
        int[][] counts = WordRepository.getMonthStats(requireContext(), year, month, days);
        Map<Integer, DayStat> stats = new HashMap<>();
        int mastered = 0, fuzzy = 0, unknown = 0;
        for (int day = 1; day <= days; day++) {
            stats.put(day, new DayStat(counts[day][0], counts[day][1], counts[day][2]));
            mastered += counts[day][0]; fuzzy += counts[day][1]; unknown += counts[day][2];
        }
        tvMonth.setText(year + "年" + month + "月");
        calendar.setDayStats(stats);
        tvLegendMastered.setText(String.valueOf(mastered));
        tvLegendFuzzy.setText(String.valueOf(fuzzy));
        tvLegendUnknown.setText(String.valueOf(unknown));
        int today = current.get(Calendar.DAY_OF_MONTH);
        int todayTotal = (year == current.get(Calendar.YEAR) && month == current.get(Calendar.MONTH) + 1) ? counts[today][0] + counts[today][1] + counts[today][2] : 0;
        tvTodayCount.setText("今日 " + WordRepository.getTodayLearnedCount(requireContext()) + " 词 · 开始学习");
        tvStatLearned.setText(String.valueOf(WordRepository.getTodayLearnedCount(requireContext())));
        tvStatReview.setText(String.valueOf(WordRepository.getDueReviewCount(requireContext())));
        tvStatStreak.setText(WordRepository.getStreakDays(requireContext()) + "天");
    }

    private int currentDays(int year, int month) {
        Calendar c = Calendar.getInstance(); c.set(Calendar.YEAR, year); c.set(Calendar.MONTH, month - 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void doSearch() {
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
        if (query.isEmpty()) { UiUtils.toast(requireContext(), "请输入要查询的单词或中文释义"); return; }
        List<WordRepository.Word> words;
        try { words = WordRepository.search(requireContext(), query, 30); }
        catch (RuntimeException e) { UiUtils.toast(requireContext(), "查询失败：" + e.getMessage()); return; }
        if (words.isEmpty()) { new AlertDialog.Builder(requireContext()).setTitle("查词").setMessage("没有找到“" + query + "”相关单词。").setPositiveButton("知道了", null).show(); return; }
        showSearchResults(query, words);
    }

    private void showSearchResults(String query, List<WordRepository.Word> words) {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(8), pad, dp(4));
        TextView summary = new TextView(requireContext());
        summary.setText("找到 " + words.size() + " 个相关单词 · 点击查看详情");
        summary.setTextColor(Color.rgb(105, 116, 126));
        summary.setTextSize(13);
        summary.setPadding(dp(4), dp(4), dp(4), dp(12));
        root.addView(summary);

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        for (WordRepository.Word word : words) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.rgb(246, 249, 247));
            background.setCornerRadius(dp(14));
            row.setBackground(background);
            row.setClickable(true);
            row.setFocusable(true);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);

            TextView title = new TextView(requireContext());
            title.setText(word.headWord + (word.pos == null || word.pos.trim().isEmpty() ? "" : "  " + word.pos));
            title.setTextColor(Color.rgb(36, 125, 87));
            title.setTextSize(17);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(title);

            if (word.phonetic != null && !word.phonetic.trim().isEmpty()) {
                TextView phonetic = new TextView(requireContext());
                phonetic.setText("/" + word.phonetic + "/");
                phonetic.setTextColor(Color.rgb(115, 125, 132));
                phonetic.setTextSize(12);
                phonetic.setPadding(0, dp(2), 0, dp(3));
                row.addView(phonetic);
            }
            TextView meaning = new TextView(requireContext());
            meaning.setText(word.meaning == null || word.meaning.trim().isEmpty() ? "暂无释义" : word.meaning);
            meaning.setTextColor(Color.rgb(47, 54, 59));
            meaning.setTextSize(14);
            meaning.setMaxLines(3);
            row.addView(meaning);

            TextView book = new TextView(requireContext());
            book.setText("词书 · " + WordRepository.displayBookName(word.bookName) + "     ›");
            book.setTextColor(Color.rgb(135, 145, 150));
            book.setTextSize(11);
            book.setPadding(0, dp(7), 0, 0);
            row.addView(book);
            row.setOnClickListener(v -> showWord(word));
            list.addView(row);
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(390)));
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("查词 · “" + query + "”")
                .setView(root)
                .setNegativeButton("关闭", null)
                .create();
        dialog.show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showWord(WordRepository.Word word) {
        StringBuilder detail = new StringBuilder();
        if (word.phonetic != null && !word.phonetic.isEmpty() && AppSettings.getBoolean(requireContext(), AppSettings.SHOW_PHONETIC, true)) detail.append('/').append(word.phonetic).append("/\n\n");
        detail.append(word.meaning == null ? "暂无释义" : word.meaning);
        if (word.sentence != null && !word.sentence.isEmpty() && AppSettings.getBoolean(requireContext(), AppSettings.SHOW_EXAMPLE, true)) {
            detail.append("\n\n").append(word.sentence);
            if (word.sentenceCn != null && !word.sentenceCn.isEmpty()) detail.append("\n").append(word.sentenceCn);
        }
        new AlertDialog.Builder(requireContext()).setTitle(word.headWord).setMessage(detail.toString())
                .setNeutralButton("加入生词本", (d, w) -> {
                    boolean added = WordRepository.toggleCategory(requireContext(), WordRepository.NOTEBOOK, word);
                    UiUtils.toast(requireContext(), added ? "已加入生词本" : "已移出生词本");
                }).setPositiveButton("收藏", (d, w) -> {
                    boolean added = WordRepository.toggleCategory(requireContext(), WordRepository.FAVORITE, word);
                    UiUtils.toast(requireContext(), added ? "已收藏" : "已取消收藏");
                }).setNegativeButton("关闭", null).show();
    }
}
