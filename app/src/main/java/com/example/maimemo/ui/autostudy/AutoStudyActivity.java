package com.example.maimemo.ui.autostudy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maimemo.business.autostudy.AutoStudyContract;
import com.example.maimemo.business.autostudy.AutoStudyForegroundService;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.util.WordAudioPlayer;
import com.example.maimemo.util.MosaicBackgroundDrawable;

/** Translucent full-screen first-level study card. */
public final class AutoStudyActivity extends AppCompatActivity {
    private WordRepository.Word word;
    private boolean answered;
    private boolean meaningShown;
    private boolean dismissedByUser;
    private LinearLayout card;
    private TextView meaningView;
    private TextView memoryStatusView;
    private ProgressBar recallBar;
    private Button revealButton;
    private final Handler lifecycleHandler = new Handler(Looper.getMainLooper());
    private final Runnable reportNotVisible = () -> {
        if (!answered && !dismissedByUser && word != null && !isChangingConfigurations()) {
            startService(new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_NOT_VISIBLE));
        }
    };

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.52f;
        getWindow().setAttributes(lp);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        readWord(getIntent());
        buildUi();
    }
    @Override protected void onStart() {
        super.onStart();
        lifecycleHandler.removeCallbacks(reportNotVisible);
        startService(new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_PRESENTED));
    }
    @Override protected void onStop() {
        super.onStop();
        if (!answered && !dismissedByUser && word != null && !isChangingConfigurations()) {
            // Debounce lifecycle changes. A delayed launch/resume should not race the overlay.
            lifecycleHandler.removeCallbacks(reportNotVisible);
            lifecycleHandler.postDelayed(reportNotVisible, 500L);
        }
    }
    @Override protected void onDestroy() {
        lifecycleHandler.removeCallbacks(reportNotVisible);
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); lifecycleHandler.removeCallbacks(reportNotVisible); setIntent(intent); answered = false; meaningShown = false; dismissedByUser = false; readWord(intent); buildUi(); }
    private void readWord(Intent i) {
        String book = i.getStringExtra(AutoStudyContract.EXTRA_BOOK); int rank = i.getIntExtra(AutoStudyContract.EXTRA_RANK, -1);
        if (book == null || rank < 0) { word = null; return; }
        word = new WordRepository.Word(book, rank, i.getStringExtra(AutoStudyContract.EXTRA_WORD),
                i.getStringExtra(AutoStudyContract.EXTRA_MEANING), i.getStringExtra(AutoStudyContract.EXTRA_PHONETIC),
                i.getStringExtra(AutoStudyContract.EXTRA_POS), i.getStringExtra(AutoStudyContract.EXTRA_SENTENCE),
                i.getStringExtra(AutoStudyContract.EXTRA_SENTENCE_CN));
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        // Explicit full-window scrim: some ROMs do not visibly apply FLAG_DIM_BEHIND
        // to translucent activities, so make the background dim part of the layout too.
        scroll.setBackgroundColor(0x22000000);
        LinearLayout outer = new LinearLayout(this); outer.setGravity(Gravity.CENTER); outer.setPadding(dp(8), dp(8), dp(8), dp(8)); outer.setOrientation(LinearLayout.VERTICAL); scroll.addView(outer);
        card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(24),dp(22),dp(24),dp(22));
        card.setBackground(new MosaicBackgroundDrawable(dp(28))); card.setElevation(dp(18));
        outer.addView(card, new LinearLayout.LayoutParams(-1,-1));
        addText("必须背 · 自动学习", 13, 0xFF16865B, false, Gravity.CENTER);
        if (word == null) {
            addText("自动学习已运行", 24, 0xFF17231D, true, Gravity.CENTER);
            addText("当前页面没有待展示的单词。返回应用后，学习服务会继续调度。", 15, 0xFF66746B, false, Gravity.CENTER);
            Button close = button("返回", 0xFF218B63); card.addView(close, fullWidth(dp(48))); close.setOnClickListener(v -> finish());
            setContentView(scroll); return;
        }
        // Memory dashboard: use the stored FSRS estimate when available; new words are not assigned a fake percentage.
        com.example.maimemo.business.wordpush.WordDeliveryItem memory = WordRepository.buildDeliveryItem(this, word, "autostudy-ui");
        boolean hasMemory = memory.reviewCount > 0 && memory.stabilityDays > 0;
        addText(hasMemory ? "🧠 本词记忆状态" : "🧠 首次建立记忆", 14, 0xFF16865B, true, Gravity.CENTER);
        if (hasMemory) {
            int recall = (int)Math.max(0, Math.min(100, Math.round(memory.retrievability * 100.0)));
            memoryStatusView = addText("当前回忆概率估算  " + recall + "%  ·  已复习 " + memory.reviewCount + " 次", 13, 0xFF40564A, true, Gravity.CENTER);
            recallBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            recallBar.setMax(100); recallBar.setProgress(recall);
            LinearLayout.LayoutParams barLp = fullWidth(dp(8)); barLp.bottomMargin = dp(8); card.addView(recallBar, barLp);
            addMemoryCurve();
        } else {
            memoryStatusView = addText("先尝试回忆，再查看答案；每次主动提取都有助于形成记忆。", 13, 0xFF40564A, false, Gravity.CENTER);
            addMemoryCurve();
        }
        addText("先回忆，不要急着看释义", 14, 0xFFB06A22, true, Gravity.CENTER);
        addText(word.headWord, 34, 0xFF17231D, true, Gravity.CENTER);
        if (com.example.maimemo.storage.AppSettings.getBoolean(this, com.example.maimemo.storage.AppSettings.SHOW_PHONETIC, true) && word.phonetic != null)
            addText(word.phonetic, 16, 0xFF7A877F, false, Gravity.CENTER);
        Button pronounce = button("🔊 播放发音", 0xFF367DC2); card.addView(pronounce, fullWidth(dp(44)));
        pronounce.setOnClickListener(v -> { try { WordAudioPlayer.play(this, word.headWord, null); } catch (Throwable ignored) { } });
        meaningView = addText("先在心里说出中文意思，再揭晓答案。", 17, 0xFF2C3931, false, Gravity.CENTER); meaningView.setPadding(0,dp(14),0,dp(14));
        if (word.sentence != null && !word.sentence.trim().isEmpty() && com.example.maimemo.storage.AppSettings.getBoolean(this, com.example.maimemo.storage.AppSettings.SHOW_EXAMPLE, true)) {
            addText(word.sentence, 13, 0xFF68766E, false, Gravity.CENTER);
            if (word.sentenceCn != null && !word.sentenceCn.trim().isEmpty()) addText(word.sentenceCn, 13, 0xFF68766E, false, Gravity.CENTER);
        }
        revealButton = button("揭晓释义 · 检查记忆", 0xFF218B63); card.addView(revealButton, fullWidth(dp(48)));
        revealButton.setOnClickListener(v -> { meaningShown = true; meaningView.setText(word.meaning == null ? "暂无释义" : word.meaning); memoryStatusView.setText("答案已揭晓：请诚实评估刚才是否想起来了。主动回忆比重复阅读更有助于巩固。 "); ratings.setVisibility(View.VISIBLE); revealButton.setVisibility(View.GONE); });
        ratings = new LinearLayout(this); ratings.setOrientation(LinearLayout.VERTICAL); ratings.setVisibility(android.view.View.GONE); ratings.setPadding(0,dp(10),0,0);
        String[] labels = {"重来 · 完全没记住", "困难 · 想起来很费力", "记住 · 正常回忆", "轻松 · 非常熟悉"}; int[] values={1,2,3,4}; int[] colors={0xFFCE5B55,0xFFDA9B32,0xFF21966B,0xFF367DC2};
        for (int i=0;i<labels.length;i++) { final int rating=values[i]; Button b=button(labels[i],colors[i]); LinearLayout.LayoutParams p=fullWidth(dp(46)); p.topMargin=dp(5); ratings.addView(b,p); b.setOnClickListener(v -> answer(rating)); }
        card.addView(ratings);
        Button pause = button("暂时关闭本张卡片",0xFF69776F); LinearLayout.LayoutParams pauseLp=fullWidth(dp(42)); pauseLp.topMargin=dp(8); card.addView(pause,pauseLp); pause.setOnClickListener(v -> { dismissedByUser = true; startService(new Intent(this,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_DISMISS)); finish(); });
        setContentView(scroll);
    }
    private LinearLayout ratings;
    @Override public void onBackPressed() {
        if (!answered) {
            dismissedByUser = true;
            startService(new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_DISMISS));
        }
        super.onBackPressed();
    }

    private void answer(int rating) {
        if (answered || word == null) return; answered=true;
        try { WordRepository.markLearned(this, word, rating); } catch (Throwable e) { answered=false; return; }
        String[] feedback = {"已记录：这次没能回忆起来。短间隔再次接触会更有帮助。", "已记录：回忆有些费力，系统会安排巩固。", "记得不错！这次回忆已交给 FSRS 安排后续复习。", "回忆很轻松！系统会根据记忆状态安排下一次复习。"};
        if (ratings != null) ratings.setVisibility(View.GONE);
        if (revealButton != null) revealButton.setVisibility(View.GONE);
        memoryStatusView.setText(feedback[Math.max(0, Math.min(3, rating - 1))]);
        memoryStatusView.setTextColor(rating <= 2 ? 0xFFB06A22 : 0xFF16865B);
        try {
            com.example.maimemo.business.wordpush.WordDeliveryItem updated = WordRepository.buildDeliveryItem(this, word, "autostudy-ui");
            if (updated.dueAt != null) addText("下次复习：" + updated.dueAt, 13, 0xFF40564A, true, Gravity.CENTER);
            if (updated.stabilityDays > 0) addText(String.format(java.util.Locale.getDefault(), "记忆稳定性：约 %.1f 天", updated.stabilityDays), 13, 0xFF40564A, false, Gravity.CENTER);
        } catch (Throwable ignored) { }
        showWordQuiz();
    }


    private void showWordQuiz() {
        java.util.List<WordRepository.Word> options = WordRepository.getQuizOptions(this, word, 4);
        if (options.size() <= 1) {
            startService(new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_ANSWERED));
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000L); return;
        }
        addText("🧩 小检测：看中文，选出正确英文", 16, 0xFF16865B, true, Gravity.CENTER);
        String type = WordRepository.getQuestionType(this, word);
        if (type != null && !type.trim().isEmpty()) addText("题型：" + type, 12, 0xFF367DC2, true, Gravity.CENTER);
        addText(word.meaning == null ? "暂无中文释义" : word.meaning, 22, 0xFF17231D, true, Gravity.CENTER);
        final boolean[] selected = {false};
        for (WordRepository.Word option : options) {
            Button choice = button(option.headWord, 0xFF367DC2);
            card.addView(choice, fullWidth(dp(44)));
            choice.setOnClickListener(v -> {
                if (selected[0]) return; selected[0] = true;
                boolean correct = option.headWord.equalsIgnoreCase(word.headWord);
                addText(correct ? "✓ 回答正确！" : "✗ 正确答案：" + word.headWord, 16, correct ? 0xFF16865B : 0xFFB06A22, true, Gravity.CENTER);
                addText(word.meaning, 13, 0xFF40564A, false, Gravity.CENTER);
                for (int i=0;i<card.getChildCount();i++) if (card.getChildAt(i) instanceof Button) ((Button)card.getChildAt(i)).setEnabled(false);
                startService(new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_ANSWERED));
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2200L);
            });
        }
    }

    private void addMemoryCurve() {
        TextView curve = addText("记忆保持示意：刚复习后较牢固，随后会逐渐减弱；在遗忘前回忆可再次强化。", 11, 0xFF66746B, false, Gravity.CENTER);
        curve.setPadding(dp(6), dp(2), dp(6), dp(8));
        curve.setBackground(tintBackground(0x18FFFFFF));
    }
    private GradientDrawable tintBackground(int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); d.setStroke(dp(1), 0x227A9A86); return d;
    }
    private TextView addText(String text,int size,int color,boolean bold,int gravity) {
        TextView v=new TextView(this); v.setText(text==null?"":text); v.setTextSize(size); v.setTextColor(color); v.setGravity(gravity); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); card.addView(v,p); return v;
    }
    private Button button(String text,int color) { Button b=new Button(this); b.setText(text); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(Color.WHITE); GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(14)); b.setBackground(d); return b; }
    private LinearLayout.LayoutParams fullWidth(int height) { return new LinearLayout.LayoutParams(-1,height); }
    private int dp(int x) { return Math.round(x*getResources().getDisplayMetrics().density); }
}
