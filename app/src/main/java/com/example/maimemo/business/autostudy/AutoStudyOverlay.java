package com.example.maimemo.business.autostudy;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.util.MosaicBackgroundDrawable;

/** Compact, touch-friendly fallback presenter used only after overlay permission has been granted. */
public final class AutoStudyOverlay {
    private static WindowManager windowManager;
    private static View currentView;
    private AutoStudyOverlay() { }

    public static boolean show(Context context, WordRepository.Word word, String source) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !Settings.canDrawOverlays(context)) return false;
        if (source != null && source.contains("auto_group_new_word")) {
            return showGroup(context, word);
        }
        dismiss(context);
        Context app = context.getApplicationContext();
        windowManager = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return false;
        LinearLayout card = new LinearLayout(app); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(app, 28), dp(app, 30), dp(app, 28), dp(app, 28));
        card.setBackground(new MosaicBackgroundDrawable(dp(app, 22))); card.setElevation(dp(app, 12));
        TextView badge = text(app, "必须背 · 主动回忆训练", 13, 0xFF16865B, true); badge.setGravity(Gravity.CENTER); card.addView(badge);
        WordRepository.Word ignored = word;
        com.example.maimemo.business.wordpush.WordDeliveryItem memory = WordRepository.buildDeliveryItem(app, word, "autostudy-overlay");
        TextView memoryState = text(app, memory.reviewCount > 0 && memory.stabilityDays > 0
                ? "🧠 当前回忆概率估算：" + Math.round(memory.retrievability * 100) + "% · 已复习 " + memory.reviewCount + " 次"
                : "🧠 新词：尝试先回忆，再揭晓答案", 12, 0xFF40564A, true); memoryState.setGravity(Gravity.CENTER); card.addView(memoryState);
        if (memory.reviewCount > 0 && memory.stabilityDays > 0) {
            ProgressBar recallBar = new ProgressBar(app, null, android.R.attr.progressBarStyleHorizontal); recallBar.setMax(100); recallBar.setProgress((int)Math.max(0, Math.min(100, Math.round(memory.retrievability * 100)))); card.addView(recallBar, new LinearLayout.LayoutParams(-1, dp(app, 7)));
        }
        TextView curve = text(app, "记忆保持示意：随时间会减弱，及时回忆可再次强化。", 11, 0xFF68766E, false); curve.setGravity(Gravity.CENTER); card.addView(curve);
        TextView prompt = text(app, "先在心里想出中文意思，再查看答案", 13, 0xFFB06A22, true); prompt.setGravity(Gravity.CENTER); card.addView(prompt);
        TextView wordView = text(app, word.headWord, 34, 0xFF17231D, true); wordView.setGravity(Gravity.CENTER); wordView.setPadding(0, dp(app, 10), 0, dp(app, 3)); card.addView(wordView);
        if (word.pos != null && !word.pos.trim().isEmpty()) { TextView pos = text(app, "词性：" + word.pos, 14, 0xFF367DC2, true); pos.setGravity(Gravity.CENTER); card.addView(pos); }
        if (word.phonetic != null && !word.phonetic.trim().isEmpty()) { TextView ph = text(app, word.phonetic, 15, 0xFF738078, false); ph.setGravity(Gravity.CENTER); card.addView(ph); }
        TextView meaning = text(app, "先尝试回忆，再揭晓释义。", 16, 0xFF26342C, false); meaning.setGravity(Gravity.CENTER); meaning.setPadding(0, dp(app, 12), 0, dp(app, 12)); card.addView(meaning);
        if (word.sentence != null && !word.sentence.trim().isEmpty()) {
            TextView sentence = text(app, word.sentence, 12, 0xFF65746B, false); card.addView(sentence);
        }
        LinearLayout row = new LinearLayout(app); row.setGravity(Gravity.CENTER); row.setPadding(0, dp(app, 14), 0, 0); row.setVisibility(View.GONE);
        Button reveal = new Button(app); reveal.setText("揭晓释义 · 检查记忆"); reveal.setAllCaps(false); reveal.setTextColor(Color.WHITE); reveal.setBackground(tint(0xFF218B63)); card.addView(reveal, new LinearLayout.LayoutParams(-1, dp(app, 44)));
        reveal.setOnClickListener(v -> { meaning.setText(word.meaning == null ? "暂无释义" : word.meaning); prompt.setText("根据刚才的回忆情况，诚实选择："); reveal.setVisibility(View.GONE); row.setVisibility(View.VISIBLE); });
        String[] labels = {"重来", "困难", "记住", "轻松"}; int[] ratings = {1,2,3,4}; int[] colors = {0xFFCE5B55,0xFFDA9B32,0xFF21966B,0xFF367DC2};
        for (int j=0;j<labels.length;j++) {
            final int rating = ratings[j]; Button b = new Button(app); b.setText(labels[j]); b.setTextSize(11); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setPadding(0,0,0,0); b.setBackground(tint(colors[j]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(app, 42), 1); lp.setMargins(dp(app, 3),0,dp(app,3),0); row.addView(b, lp);
            b.setOnClickListener(v -> submit(app, word, rating, card));
        }
        card.addView(row);
        Button close = new Button(app); close.setText("暂时关闭"); close.setAllCaps(false); close.setTextSize(12); card.addView(close, new LinearLayout.LayoutParams(-1, dp(app, 38)));
        close.setOnClickListener(v -> { dismiss(app); app.startService(new android.content.Intent(app, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_DISMISS)); });
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams((int)(app.getResources().getDisplayMetrics().widthPixels * 0.98f), WindowManager.LayoutParams.MATCH_PARENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            windowManager.addView(card, params);
            currentView = card;
            // Acknowledge visibility so the service does not launch a duplicate Activity
            // or notification. The same service remains the source of queue progression.
            app.startService(new android.content.Intent(app, AutoStudyForegroundService.class)
                    .setAction(AutoStudyContract.ACTION_OVERLAY_PRESENTED));
            return true;
        } catch (Throwable e) { currentView = null; return false; }
    }

    /** App-outside group learning mirrors the Activity state machine while sharing its queue. */
    private static boolean showGroup(Context context, WordRepository.Word word) {
        dismiss(context);
        Context app = context.getApplicationContext();
        windowManager = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return false;
        final String phase = AutoStudyGroupQueue.phase(app);
        final int groupNo = Math.max(1, AutoStudyGroupQueue.getGroupNumber(app));
        final int pos = AutoStudyGroupQueue.getPosition(app) + 1;
        final int size = AutoStudyGroupQueue.getSize(app);
        LinearLayout card = new LinearLayout(app); card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(app, 24), dp(app, 26), dp(app, 24), dp(app, 22));
        card.setBackground(new MosaicBackgroundDrawable(dp(app, 22))); card.setElevation(dp(app, 12));
        TextView badge = text(app, "必须背 · 分组学习 · 第 " + groupNo + " 组", 15, 0xFF16865B, true); badge.setGravity(Gravity.CENTER); card.addView(badge);
        if (AutoStudyGroupQueue.FAMILIARITY.equals(phase)) {
            int rounds = Math.max(1, Math.min(2, com.example.maimemo.storage.AppSettings.getInt(app, com.example.maimemo.storage.AppSettings.AUTO_FAMILIARITY_ROUNDS, 1)));
            TextView stage = text(app, "熟悉阶段 · 第 " + AutoStudyGroupQueue.round(app) + "/" + rounds + " 轮 · 本轮 " + pos + "/" + size, 14, 0xFFB06A22, true); stage.setGravity(Gravity.CENTER); card.addView(stage);
            TextView head = text(app, word.headWord, 34, 0xFF17231D, true); head.setGravity(Gravity.CENTER); head.setPadding(0, dp(app, 16), 0, dp(app, 5)); card.addView(head);
            if (word.phonetic != null && !word.phonetic.trim().isEmpty()) { TextView ph=text(app, word.phonetic, 15, 0xFF7A877F, false); ph.setGravity(Gravity.CENTER); card.addView(ph); }
            if (word.pos != null && !word.pos.trim().isEmpty()) { TextView ps=text(app, "词性：" + word.pos, 15, 0xFF367DC2, true); ps.setGravity(Gravity.CENTER); card.addView(ps); }
            TextView meaning=text(app, word.meaning == null ? "暂无释义" : word.meaning, 21, 0xFF2C3931, true); meaning.setGravity(Gravity.CENTER); meaning.setPadding(0,dp(app,12),0,dp(app,12)); card.addView(meaning);
            if (word.sentence != null && !word.sentence.trim().isEmpty()) { TextView sen=text(app, word.sentence, 14, 0xFF68766E, false); card.addView(sen); }
            if (word.sentenceCn != null && !word.sentenceCn.trim().isEmpty()) { TextView senCn=text(app, word.sentenceCn, 13, 0xFF68766E, false); card.addView(senCn); }
            Button audio=new Button(app); audio.setText("🔊 播放发音"); audio.setAllCaps(false); card.addView(audio,new LinearLayout.LayoutParams(-1,dp(app,44))); audio.setOnClickListener(v->{try{com.example.maimemo.util.WordAudioPlayer.play(app,word.headWord,null);}catch(Throwable ignored){}});
            Button next=new Button(app); next.setText("已熟悉，继续下一个"); next.setAllCaps(false); next.setTextColor(Color.WHITE); next.setBackground(tint(0xFF218B63)); LinearLayout.LayoutParams nl=new LinearLayout.LayoutParams(-1,dp(app,48));nl.topMargin=dp(app,12);card.addView(next,nl);
            next.setOnClickListener(v->{AutoStudyGroupQueue.familiarityDone(app,word.bookName,word.rank); app.startService(new android.content.Intent(app,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_GROUP_PROGRESS)); dismiss(app);});
        } else if (AutoStudyGroupQueue.RECALL.equals(phase)) {
            TextView stage=text(app,"背诵阶段 · 本组第 "+pos+"/"+size+" 个",14,0xFFB06A22,true);stage.setGravity(Gravity.CENTER);card.addView(stage);
            TextView head=text(app,word.headWord,34,0xFF17231D,true);head.setGravity(Gravity.CENTER);head.setPadding(0,dp(app,16),0,dp(app,5));card.addView(head);
            if(word.phonetic!=null&&!word.phonetic.trim().isEmpty()){TextView ph=text(app,word.phonetic,15,0xFF7A877F,false);ph.setGravity(Gravity.CENTER);card.addView(ph);}
            TextView meaning=text(app,"先回忆中文意思，再揭晓答案。",18,0xFF2C3931,false);meaning.setGravity(Gravity.CENTER);meaning.setPadding(0,dp(app,14),0,dp(app,14));card.addView(meaning);
            LinearLayout ratings=new LinearLayout(app);ratings.setOrientation(LinearLayout.VERTICAL);ratings.setVisibility(View.GONE);card.addView(ratings,new LinearLayout.LayoutParams(-1,-2));
            Button reveal=new Button(app);reveal.setText("揭晓释义");reveal.setAllCaps(false);reveal.setTextColor(Color.WHITE);reveal.setBackground(tint(0xFF218B63));card.addView(reveal,new LinearLayout.LayoutParams(-1,dp(app,46)));
            reveal.setOnClickListener(v->{meaning.setText(word.meaning==null?"暂无释义":word.meaning);reveal.setVisibility(View.GONE);ratings.setVisibility(View.VISIBLE);});
            String[] labels={"重来 · 完全没记住","困难 · 回忆费力","记住 · 正常回忆","轻松 · 非常熟悉"};int[] values={1,2,3,4};int[] colors={0xFFCE5B55,0xFFDA9B32,0xFF21966B,0xFF367DC2};
            for(int i=0;i<labels.length;i++){final int rating=values[i];Button b=new Button(app);b.setText(labels[i]);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(tint(colors[i]));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(app,44));lp.topMargin=dp(app,5);ratings.addView(b,lp);b.setOnClickListener(v->{try{WordRepository.markLearned(app,word,rating);}catch(Throwable ignored){}AutoStudyGroupQueue.recallDone(app,word.bookName,word.rank);app.startService(new android.content.Intent(app,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_GROUP_PROGRESS));dismiss(app);});}
        } else {
            TextView stage=text(app,"本组统一测试 · 待测 "+AutoStudyGroupQueue.quizRemaining(app),14,0xFFB06A22,true);stage.setGravity(Gravity.CENTER);card.addView(stage);
            String type=AutoStudyGroupQueue.quizType(app); boolean sentence=type!=null&&type.contains("句子");
            TextView prompt;
            if(sentence){String raw=word.sentence==null?"":word.sentence.trim();String masked=raw.isEmpty()?"暂无例句，请根据中文释义选择单词":raw.replaceAll("(?i)\\b"+java.util.regex.Pattern.quote(word.headWord)+"\\b","_____ ");if(masked.equals(raw)&&!raw.isEmpty())masked=raw+"\n目标词义："+word.meaning;prompt=text(app,masked,18,0xFF17231D,true);}
            else prompt=text(app,word.meaning==null?"暂无中文释义":word.meaning,22,0xFF17231D,true);
            prompt.setGravity(Gravity.CENTER);prompt.setPadding(0,dp(app,14),0,dp(app,14));card.addView(prompt);
            java.util.List<WordRepository.Word> options=WordRepository.getQuizOptions(app,word,4);if(options.isEmpty())options=java.util.Collections.singletonList(word);final boolean[] done={false};
            for(WordRepository.Word option:options){Button b=new Button(app);b.setText(option.headWord+(sentence&&option.pos!=null?" · "+option.pos:""));b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(tint(0xFF367DC2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(app,44));lp.topMargin=dp(app,5);card.addView(b,lp);b.setOnClickListener(v->{if(done[0])return;done[0]=true;boolean correct=option.headWord.equalsIgnoreCase(word.headWord);AutoStudyGroupQueue.quizAnswer(app,word.bookName,word.rank,correct);prompt.setText(correct?"✓ 回答正确":"✗ 正确答案："+word.headWord+(word.meaning==null?"":"\n"+word.meaning));new Handler(Looper.getMainLooper()).postDelayed(()->{app.startService(new android.content.Intent(app,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_GROUP_PROGRESS));dismiss(app);},900L);});}
        }
        Button close=new Button(app);close.setText("暂时关闭本张卡片");close.setAllCaps(false);card.addView(close,new LinearLayout.LayoutParams(-1,dp(app,38)));close.setOnClickListener(v->{dismiss(app);app.startService(new android.content.Intent(app,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_DISMISS));});
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params=new WindowManager.LayoutParams((int)(app.getResources().getDisplayMetrics().widthPixels*0.98f),WindowManager.LayoutParams.MATCH_PARENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);params.gravity=Gravity.CENTER;
        try{windowManager.addView(card,params);currentView=card;app.startService(new android.content.Intent(app,AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_OVERLAY_PRESENTED));return true;}catch(Throwable e){currentView=null;return false;}
    }

    private static void submit(Context c, WordRepository.Word word, int rating, LinearLayout card) {
        try { WordRepository.markLearned(c, word, rating); } catch (Throwable ignored) { }
        String[] feedback = {"这次没能回忆起来 · 已记录，后续会安排巩固。", "回忆有些费力 · 已记录，继续强化。", "记得不错 · FSRS 已安排后续复习。", "回忆很轻松 · FSRS 已安排后续复习。"};
        card.removeAllViews();
        TextView title = text(c, "记忆反馈", 18, 0xFF16865B, true); title.setGravity(Gravity.CENTER); card.addView(title);
        TextView result = text(c, feedback[Math.max(0, Math.min(3, rating - 1))], 16, 0xFF26342C, true); result.setGravity(Gravity.CENTER); result.setPadding(0, dp(c, 22), 0, dp(c, 16)); card.addView(result);
        try {
            com.example.maimemo.business.wordpush.WordDeliveryItem updated = WordRepository.buildDeliveryItem(c, word, "autostudy-overlay");
            if (updated.dueAt != null) { TextView due = text(c, "下次复习：" + updated.dueAt, 13, 0xFF40564A, false); due.setGravity(Gravity.CENTER); card.addView(due); }
            if (updated.stabilityDays > 0) { TextView stable = text(c, String.format(java.util.Locale.getDefault(), "记忆稳定性约 %.1f 天", updated.stabilityDays), 13, 0xFF40564A, false); stable.setGravity(Gravity.CENTER); card.addView(stable); }
        } catch (Throwable ignored) { }
        java.util.List<WordRepository.Word> options = WordRepository.getQuizOptions(c, word, 4);
        if (options.size() > 1) {
            TextView quizTitle = text(c, "🧩 小检测：看中文，选出英文", 16, 0xFF16865B, true); quizTitle.setGravity(Gravity.CENTER); quizTitle.setPadding(0, dp(c, 18), 0, dp(c, 8)); card.addView(quizTitle);
            String type = WordRepository.getQuestionType(c, word);
            if (type != null && !type.trim().isEmpty()) { TextView typeView = text(c, "题型：" + type, 12, 0xFF367DC2, true); typeView.setGravity(Gravity.CENTER); card.addView(typeView); }
            TextView cn = text(c, word.meaning == null ? "暂无中文释义" : word.meaning, 22, 0xFF17231D, true); cn.setGravity(Gravity.CENTER); cn.setPadding(0, dp(c, 10), 0, dp(c, 12)); card.addView(cn);
            final boolean[] answeredQuiz = {false};
            for (WordRepository.Word option : options) {
                Button choice = new Button(c); choice.setText(option.headWord); choice.setAllCaps(false); choice.setTextSize(15); choice.setTextColor(Color.WHITE); choice.setBackground(tint(0xFF367DC2));
                LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(c, 44)); op.setMargins(0, dp(c, 3), 0, dp(c, 3)); card.addView(choice, op);
                choice.setOnClickListener(v -> {
                    if (answeredQuiz[0]) return; answeredQuiz[0] = true;
                    boolean correct = option.headWord.equalsIgnoreCase(word.headWord);
                    cn.setText((correct ? "✓ 回答正确！" : "✗ 正确答案：" + word.headWord) + "\n" + (word.meaning == null ? "" : word.meaning));
                    for (int i = 0; i < card.getChildCount(); i++) { View child = card.getChildAt(i); if (child instanceof Button && child != choice) child.setEnabled(false); }
                    choice.setText(correct ? "✓ " + option.headWord : "你的选择：" + option.headWord);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> { c.startService(new android.content.Intent(c, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_ANSWERED)); dismiss(c); }, 2200L);
                });
            }
        } else {
            c.startService(new android.content.Intent(c, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_ANSWERED));
            new Handler(Looper.getMainLooper()).postDelayed(() -> dismiss(c), 3000L);
        }
    }
    public static void dismiss(Context context) {
        if (currentView != null && windowManager != null) {
            try { windowManager.removeView(currentView); } catch (Throwable ignored) { }
        }
        currentView = null;
    }
    private static TextView text(Context c, String value, int size, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(value == null ? "" : value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private static GradientDrawable tint(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(100); return d; }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
