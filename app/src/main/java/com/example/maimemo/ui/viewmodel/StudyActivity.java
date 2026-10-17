package com.example.maimemo.ui.viewmodel;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.maimemo.R;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.business.wordpush.WordPushService;
import com.example.maimemo.business.wordpush.WordDeliveryItem;
import com.example.maimemo.business.fsrs6.Fsrs6Scheduler;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.util.UiUtils;
import com.example.maimemo.util.WordAudioPlayer;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.maimemo.data.database.LocalDatabase;

/** 本地背词流程：从当前词书读取未学习单词，回答后写入学习记录。 */
public class StudyActivity extends AppCompatActivity {
    private WordRepository.Word current;
    private WordDeliveryItem currentDeliveryItem;
    private TextView title, phonetic, meaning, sentence, progress;
    private Button showAnswer, again, hard, know, easy, notebook, favorite, audioButton;
    private boolean answerShown;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoRevealRunnable = this::revealAnswer;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.page_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        loadNext();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(autoRevealRunnable);
        super.onDestroy();
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(8), pad, dp(10));
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.page_bg));

        TextView back = new TextView(this);
        back.setText("‹   背诵练习"); back.setTextSize(16); back.setTypeface(null, android.graphics.Typeface.BOLD);
        back.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        back.setGravity(Gravity.CENTER_VERTICAL); back.setPadding(dp(4), 0, 0, dp(4));
        back.setOnClickListener(v -> finish()); root.addView(back, new LinearLayout.LayoutParams(-1, dp(38)));

        progress = new TextView(this); progress.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        progress.setTextSize(12); progress.setPadding(dp(10), dp(6), dp(10), dp(6));
        progress.setBackgroundResource(R.drawable.bg_pill_green_light);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-2, -2);
        progressLp.topMargin = dp(3); root.addView(progress, progressLp);
        ScrollView scroll = new ScrollView(this);
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL); card.setPadding(dp(20), dp(24), dp(20), dp(22));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setElevation(dp(2));
        title = new TextView(this); title.setTextSize(34); title.setTypeface(null, android.graphics.Typeface.BOLD); title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.015f);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); card.addView(title);
        phonetic = new TextView(this); phonetic.setTextSize(15); phonetic.setGravity(Gravity.CENTER);
        phonetic.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout audioRow = new LinearLayout(this); audioRow.setGravity(Gravity.CENTER); audioRow.setOrientation(LinearLayout.HORIZONTAL);
        audioButton = button("🔊 播放发音"); audioButton.setOnClickListener(v -> playCurrentAudio(true));
        audioRow.addView(phonetic, new LinearLayout.LayoutParams(0, -2, 1));
        audioRow.addView(audioButton, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams phoneticLp = new LinearLayout.LayoutParams(-1, -2); phoneticLp.topMargin = dp(8); card.addView(audioRow, phoneticLp);
        meaning = new TextView(this); meaning.setTextSize(18); meaning.setGravity(Gravity.CENTER);
        meaning.setLineSpacing(dp(3), 1f);
        meaning.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); meaning.setPadding(dp(8), dp(22), dp(8), dp(12)); card.addView(meaning);
        sentence = new TextView(this); sentence.setTextSize(14); sentence.setGravity(Gravity.CENTER);
        sentence.setLineSpacing(dp(3), 1f); sentence.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        sentence.setPadding(dp(8), dp(4), dp(8), dp(4)); card.addView(sentence);
        scroll.addView(card); LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1); scrollLp.topMargin = dp(16); root.addView(scroll, scrollLp);

        showAnswer = button("显示释义"); showAnswer.setTextSize(15);
        showAnswer.setTextColor(ContextCompat.getColor(this, R.color.white));
        showAnswer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_green)));
        showAnswer.setTextSize(16); showAnswer.setTypeface(null, android.graphics.Typeface.BOLD);
        showAnswer.setElevation(dp(2));
        root.addView(showAnswer, new LinearLayout.LayoutParams(-1, dp(54)));
        showAnswer.setOnClickListener(v -> revealAnswer());
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        again = button("没有印象"); hard = button("记忆模糊"); know = button("暂时记住"); easy = button("已经记牢");
        Button[] ratings = {again, hard, know, easy};
        int[] ratingColors = {0xFFFFEFEC, 0xFFFFF5DF, 0xFFEAF7EF, 0xFFEAF3FF};
        int[] ratingTextColors = {0xFFB94336, 0xFF94620B, 0xFF237B4B, 0xFF2867A8};
        String[] ratingMarks = {"↻", "≈", "✓", "✦"};
        for (int i = 0; i < ratings.length; i++) {
            ratings[i].setText(ratingMarks[i] + "\n" + new String[]{"没有印象", "记忆模糊", "暂时记住", "已经记牢"}[i]);
            ratings[i].setTextSize(13); ratings[i].setTypeface(null, android.graphics.Typeface.BOLD);
            ratings[i].setGravity(Gravity.CENTER); ratings[i].setMinWidth(0); ratings[i].setMinHeight(0);
            ratings[i].setPadding(dp(2), dp(7), dp(2), dp(7));
            ratings[i].setIncludeFontPadding(false); ratings[i].setLineSpacing(dp(3), 1f);
            android.graphics.drawable.GradientDrawable ratingBg = new android.graphics.drawable.GradientDrawable();
            ratingBg.setColor(ratingColors[i]); ratingBg.setCornerRadius(dp(16));
            ratingBg.setStroke(dp(1), android.graphics.Color.argb(30, ratingTextColors[i] >> 16 & 255, ratingTextColors[i] >> 8 & 255, ratingTextColors[i] & 255));
            ratings[i].setBackground(ratingBg); ratings[i].setTextColor(ratingTextColors[i]);
            ratings[i].setElevation(dp(1)); ratings[i].setStateListAnimator(null);
            LinearLayout.LayoutParams ratingLp = new LinearLayout.LayoutParams(0, dp(76), 1);
            if (i > 0) ratingLp.leftMargin = dp(7);
            row.addView(ratings[i], ratingLp);
        }
        LinearLayout.LayoutParams ratingRowLp = new LinearLayout.LayoutParams(-1, -2);
        ratingRowLp.topMargin = dp(12); root.addView(row, ratingRowLp);
        again.setOnClickListener(v -> recordAnswer(Fsrs6Scheduler.AGAIN));
        hard.setOnClickListener(v -> recordAnswer(Fsrs6Scheduler.HARD));
        know.setOnClickListener(v -> recordAnswer(Fsrs6Scheduler.GOOD));
        easy.setOnClickListener(v -> recordAnswer(Fsrs6Scheduler.EASY));

        LinearLayout saveRow = new LinearLayout(this); saveRow.setOrientation(LinearLayout.HORIZONTAL);
        notebook = button("加入生词本"); favorite = button("收藏");
        notebook.setTextSize(12); favorite.setTextSize(12);
        notebook.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEAF5EF));
        favorite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEAF5EF));
        notebook.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        favorite.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
        saveRow.addView(notebook, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(0, dp(44), 1); favLp.leftMargin = dp(10); saveRow.addView(favorite, favLp);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, -2); saveLp.topMargin = dp(8); root.addView(saveRow, saveLp);
        notebook.setOnClickListener(v -> toggleSaved(WordRepository.NOTEBOOK));
        favorite.setOnClickListener(v -> toggleSaved(WordRepository.FAVORITE));
        setContentView(root);
        com.example.maimemo.util.SystemBarUtils.apply(this, root);
    }

    private Button button(String text) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); return button;
    }

    private void loadNext() {
        String book = WordRepository.getCurrentBook(this);
        int dailyLimit = AppSettings.getInt(this, AppSettings.DAILY_NEW_WORDS, 50);
        int todayCount = WordRepository.getTodayLearnedCount(this);
        if (todayCount >= dailyLimit && !WordRepository.hasDueReview(this, book)) {
            current = null;
            title.setText("今日学习目标已完成");
            phonetic.setText("");
            meaning.setText("今日已完成 " + todayCount + " 次单词学习，达到每日目标 " + dailyLimit + "。明天再继续吧。");
            sentence.setText("");
            progress.setText(WordRepository.displayBookName(book));
            showAnswer.setVisibility(View.GONE);
            again.setEnabled(false); hard.setEnabled(false); know.setEnabled(false); easy.setEnabled(false); notebook.setEnabled(false); favorite.setEnabled(false);
            return;
        }
        currentDeliveryItem = WordPushService.nextItem(this, book);
        current = currentDeliveryItem == null ? null : currentDeliveryItem.word;
        answerShown = false;
        if (current == null) {
            title.setText("本词书已全部学习"); phonetic.setText(""); meaning.setText("你已经完成当前词书中的全部新词。"); sentence.setText("");
            progress.setText(WordRepository.displayBookName(book)); showAnswer.setVisibility(View.GONE); again.setEnabled(false); hard.setEnabled(false); know.setEnabled(false); easy.setEnabled(false); notebook.setEnabled(false); favorite.setEnabled(false); return;
        }
        title.setText(current.headWord);
        phonetic.setText(AppSettings.getBoolean(this, AppSettings.SHOW_PHONETIC, true) && !current.phonetic.isEmpty() ? "/" + current.phonetic + "/" : "");
        meaning.setText("点击“显示释义”查看答案"); sentence.setText("");
        String sourceLabel = currentDeliveryItem != null && currentDeliveryItem.source == WordDeliveryItem.Source.DUE_REVIEW ? "到期复习" : "新词";
        progress.setText(WordRepository.displayBookName(book) + " · " + sourceLabel + " · 已学 " + WordRepository.getLearnedCount(this, book));
        showAnswer.setVisibility(View.VISIBLE); showAnswer.setText("显示释义"); again.setEnabled(true); hard.setEnabled(true); know.setEnabled(true); easy.setEnabled(true); notebook.setEnabled(true); favorite.setEnabled(true);
        refreshSaveButtons();
        audioButton.setEnabled(true);
        playCurrentAudio(false);
        handler.removeCallbacks(autoRevealRunnable);
        // “全自动”目前仅作为后期开发选项保留，不启用自动揭示、自动评分或自动跳词。
        // 未来接口与数据字段说明见 business/wordpush/AutoStudyModeDevelopment.java。
    }

    private boolean isAutoPlayEnabled() {
        try (Cursor c = LocalDatabase.get(this).rawQuery("SELECT autoPlay FROM Global LIMIT 1", null)) {
            return !c.moveToFirst() || c.getInt(0) != 0;
        } catch (Exception e) { return true; }
    }

    private void playCurrentAudio(boolean manual) {
        if (current == null) return;
        if (!manual && !isAutoPlayEnabled()) return;
        audioButton.setText("正在加载音频…");
        WordAudioPlayer.play(this, current.headWord, () -> {
            if (isFinishing() || isDestroyed()) return;
            audioButton.setText("🔊 播放发音");
            if (manual) UiUtils.toast(this, "发音下载失败，请检查网络或稍后重试");
        });
        // Keep the replay control available while the background download/playback starts.
        audioButton.postDelayed(() -> { if (!isFinishing()) audioButton.setText("🔊 播放发音"); }, 1800);
    }

    private void revealAnswer() {
        if (current == null || answerShown) return;
        handler.removeCallbacks(autoRevealRunnable);
        answerShown = true;
        meaning.setText(current.meaning == null || current.meaning.isEmpty() ? "暂无释义" : current.meaning);
        if (AppSettings.getBoolean(this, AppSettings.SHOW_EXAMPLE, true) && current.sentence != null && !current.sentence.isEmpty()) {
            sentence.setText(current.sentence + (current.sentenceCn == null || current.sentenceCn.isEmpty() ? "" : "\n" + current.sentenceCn));
        }
        showAnswer.setText("已显示释义");
    }

    private void recordAnswer(int result) {
        if (current == null) return;
        if (!answerShown) { revealAnswer(); UiUtils.toast(this, "先查看释义，再选择记忆情况"); return; }
        WordPushService.recordAnswer(this, current, result);
        loadNext();
    }

    private void toggleSaved(String category) {
        if (current == null) return;
        boolean added = WordRepository.toggleCategory(this, category, current);
        UiUtils.toast(this, added ? (category.equals(WordRepository.NOTEBOOK) ? "已加入生词本" : "已收藏") : "已移出列表");
        refreshSaveButtons();
    }

    private void refreshSaveButtons() {
        if (current == null) return;
        notebook.setText(WordRepository.isInCategory(this, WordRepository.NOTEBOOK, current) ? "移出生词本" : "加入生词本");
        favorite.setText(WordRepository.isInCategory(this, WordRepository.FAVORITE, current) ? "取消收藏" : "收藏");
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
