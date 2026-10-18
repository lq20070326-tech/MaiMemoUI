package com.example.maimemo.ui.viewmodel;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.maimemo.business.fsrs6.Fsrs6Scheduler;
import com.example.maimemo.business.fsrs6.FsrsParameterStore;
import com.example.maimemo.data.database.LocalDatabase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Export local review history for FSRS parameter fitting and import a fitted FSRS-6 w vector. */
public class FsrsDataActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private TextView status;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getSupportActionBarSafe();
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        TextView back = new TextView(this);
        back.setText("‹"); back.setTextSize(32); back.setTextColor(getColor(com.example.maimemo.R.color.brand_green));
        back.setPadding(dp(4), dp(8), dp(4), dp(16)); back.setOnClickListener(v -> finish());
        root.addView(back, matchWrap());

        TextView title = new TextView(this);
        title.setText("FSRS 数据与参数"); title.setTextSize(24); title.setTextColor(getColor(com.example.maimemo.R.color.text_primary)); title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());
        TextView subtitle = new TextView(this);
        subtitle.setText("把真实复习记录转化为更适合自己的记忆模型"); subtitle.setTextSize(13); subtitle.setTextColor(getColor(com.example.maimemo.R.color.text_secondary));
        LinearLayout.LayoutParams subLp = matchWrap(); subLp.topMargin = dp(4); subLp.bottomMargin = dp(16); root.addView(subtitle, subLp);

        LinearLayout infoCard = new LinearLayout(this); infoCard.setOrientation(LinearLayout.VERTICAL); infoCard.setPadding(dp(16), dp(16), dp(16), dp(16)); infoCard.setBackgroundResource(com.example.maimemo.R.drawable.bg_card);
        TextView infoTitle = new TextView(this); infoTitle.setText("使用说明"); infoTitle.setTextSize(16); infoTitle.setTypeface(null, android.graphics.Typeface.BOLD); infoTitle.setTextColor(getColor(com.example.maimemo.R.color.text_primary)); infoCard.addView(infoTitle, matchWrap());
        TextView intro = new TextView(this);
        intro.setText("① 导出本机复习历史 CSV，交给支持 FSRS-6 的拟合工具。\n② 拟合得到 21 个 w 参数后，导入参数 CSV。\n③ 新参数将用于之后的复习排程。\n\n仅处理本地数据，不会上传学习记录。旧记录缺失的历史状态无法自动还原；导入参数不会自动重算现有卡片的到期时间。");
        intro.setTextSize(14); intro.setLineSpacing(dp(3), 1f); intro.setTextColor(getColor(com.example.maimemo.R.color.text_secondary));
        LinearLayout.LayoutParams introLp = matchWrap(); introLp.topMargin = dp(10); infoCard.addView(intro, introLp);
        LinearLayout.LayoutParams cardLp = matchWrap(); cardLp.bottomMargin = dp(16); root.addView(infoCard, cardLp);

        TextView exportHeading = sectionHeading("数据集"); root.addView(exportHeading, matchWrap());
        Button export = button("导出 FSRS 训练数据 CSV"); styleAction(export, true); root.addView(export, matchWrap());
        TextView exportHint = hint("包含评分、复习间隔、记忆状态及排程信息，可用于外部参数拟合。"); LinearLayout.LayoutParams hintLp = matchWrap(); hintLp.topMargin = dp(6); hintLp.bottomMargin = dp(16); root.addView(exportHint, hintLp);
        TextView parameterHeading = sectionHeading("参数管理"); root.addView(parameterHeading, matchWrap());
        Button importWeights = button("导入拟合后的 w 参数 CSV"); styleAction(importWeights, true);
        LinearLayout.LayoutParams bp = matchWrap(); root.addView(importWeights, bp);
        Button reset = button("恢复默认 FSRS-6 参数"); styleAction(reset, false);
        bp = matchWrap(); bp.topMargin = dp(8); root.addView(reset, bp);
        status = new TextView(this); status.setTextSize(13); status.setTextColor(getColor(com.example.maimemo.R.color.text_secondary)); status.setPadding(dp(12), dp(12), dp(12), dp(12)); status.setBackgroundResource(com.example.maimemo.R.drawable.bg_card); bp = matchWrap(); bp.topMargin = dp(16); root.addView(status, bp);
        setContentView(scroll);
        com.example.maimemo.util.SystemBarUtils.apply(this, scroll);

        exportLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
            if (uri == null) return;
            try { int rows = exportCsv(uri); message("导出成功：" + rows + " 条学习记录"); }
            catch (Exception e) { message("导出失败：" + safeMessage(e)); }
        });
        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try { double[] weights = parseWeights(uri); FsrsParameterStore.setWeights(this, weights); message("导入成功：已启用 21 个 FSRS-6 w 参数。后续评分将使用新参数。"); }
            catch (Exception e) { message("导入失败：" + safeMessage(e) + "。请检查文件格式和 21 个参数是否有效。"); }
        });
        export.setOnClickListener(v -> exportLauncher.launch("maimemo_fsrs_training_" + System.currentTimeMillis() + ".csv"));
        importWeights.setOnClickListener(v -> importLauncher.launch(new String[]{"text/*", "text/csv", "application/csv", "application/octet-stream"}));
        reset.setOnClickListener(v -> { FsrsParameterStore.reset(this); message("已恢复内置默认参数。后续排程将使用默认 FSRS-6 参数。"); });
        status.setText("当前参数：" + FsrsParameterStore.getWeights(this).length + " 个 w 值\n参数文件格式见下方说明。\n\nw 参数导入格式支持：\n1. 单行 CSV：21 个数字，依次对应 w0 到 w20；\n2. 多行 CSV：每行一个数字，或每行“参数名,数值”（如 w0,0.212）。可带表头。\n数值须为有限正数且不超过 100。");
    }

    private int exportCsv(Uri uri) throws Exception {
        SQLiteDatabase db = LocalDatabase.get(this);
        String sql = "SELECT id,book_name,word_rank,learned_at,event_type,is_review,rating,result,elapsed_days,scheduled_days," +
                "stability_before,difficulty_before,retrievability_before,stability_after,difficulty_after,due_at_after " +
                "FROM app_learning_log ORDER BY learned_at ASC,id ASC";
        int count = 0;
        try (Cursor c = db.rawQuery(sql, null);
             OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IllegalStateException("无法打开导出文件");
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            w.write("id,book_name,word_rank,reviewed_at,event_type,is_review,rating,result,elapsed_days,scheduled_days,stability_before,difficulty_before,retrievability_before,stability_after,difficulty_after,due_at_after\r\n");
            while (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    if (i > 0) w.write(',');
                    w.write(csv(c.isNull(i) ? "" : c.getString(i)));
                }
                w.write("\r\n"); count++;
            }
            w.flush();
        }
        return count;
    }

    private double[] parseWeights(Uri uri) throws Exception {
        List<Double> nums = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IllegalArgumentException("无法读取所选文件");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] cells = line.split(",", -1);
                List<Double> lineNums = new ArrayList<>();
                for (String cell : cells) {
                    String v = cell.trim();
                    if (v.isEmpty()) continue;
                    try { lineNums.add(Double.parseDouble(v)); } catch (NumberFormatException ignored) { }
                }
                if (lineNums.size() == 21 && nums.isEmpty()) nums.addAll(lineNums);
                else if (lineNums.size() == 1) nums.add(lineNums.get(0));
                else if (lineNums.size() >= 2) nums.add(lineNums.get(lineNums.size() - 1));
            }
        }
        if (nums.size() != 21) throw new IllegalArgumentException("解析到 " + nums.size() + " 个数字，需要恰好 21 个（w0 至 w20）");
        double[] values = new double[21];
        for (int i = 0; i < values.length; i++) {
            values[i] = nums.get(i);
            if (!Double.isFinite(values[i]) || values[i] <= 0 || values[i] > 100)
                throw new IllegalArgumentException("w" + i + " 数值无效：" + values[i]);
        }
        return values;
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
    private TextView sectionHeading(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(17); v.setTypeface(null, android.graphics.Typeface.BOLD); v.setTextColor(getColor(com.example.maimemo.R.color.text_primary)); LinearLayout.LayoutParams lp = matchWrap(); lp.bottomMargin = dp(8); v.setLayoutParams(lp); return v; }
    private TextView hint(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(12); v.setTextColor(getColor(com.example.maimemo.R.color.text_secondary)); return v; }
    private void styleAction(Button b, boolean primary) { b.setAllCaps(false); b.setTextSize(15); b.setPadding(dp(8), dp(8), dp(8), dp(8)); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(primary ? com.example.maimemo.R.color.brand_green : com.example.maimemo.R.color.card_bg))); b.setTextColor(getColor(primary ? android.R.color.white : com.example.maimemo.R.color.text_primary)); }
    private Button button(String text) { Button b = new Button(this); b.setText(text); return b; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private void message(String text) { status.setText(text); Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    private String safeMessage(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private void getSupportActionBarSafe() { if (getSupportActionBar() != null) getSupportActionBar().setTitle("FSRS 数据与参数"); }
}
