package com.example.maimemo.ui.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.database.Cursor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** 词书、生词本、收藏全部读取本地 SQLite，不再使用固定演示数组。 */
public class WordbookActivity extends AppCompatActivity {
    private static final String EXTRA_TAB = "extra_tab";
    private final List<TextView> tabs = new ArrayList<>();
    private LinearLayout container;
    private LayoutInflater inflater;
    private int selectedTab;
    private static final int REQUEST_IMPORT_BOOK = 9201;

    public static Intent newIntent(Context context, int tab) {
        Intent intent = new Intent(context, WordbookActivity.class);
        intent.putExtra(EXTRA_TAB, tab);
        return intent;
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_wordbook);
        com.example.maimemo.util.SystemBarUtils.apply(this, findViewById(android.R.id.content));
        UiUtils.setupTopBar(this, "词书与单词本");
        inflater = LayoutInflater.from(this);
        container = findViewById(R.id.container);
        tabs.add(findViewById(R.id.tab_wordbook));
        tabs.add(findViewById(R.id.tab_notebook));
        tabs.add(findViewById(R.id.tab_favorite));
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            tabs.get(i).setOnClickListener(v -> select(index));
        }
        select(Math.max(0, Math.min(2, getIntent().getIntExtra(EXTRA_TAB, 0))));
    }

    @Override protected void onResume() {
        super.onResume();
        if (container != null) render();
    }

    private void select(int index) {
        selectedTab = index;
        for (int i = 0; i < tabs.size(); i++) {
            boolean selected = i == index;
            TextView tab = tabs.get(i);
            tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : 0);
            tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.brand_green : R.color.text_secondary));
            tab.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        render();
    }

    private void render() {
        container.removeAllViews();
        try {
            if (selectedTab == 0) renderBooks();
            else renderSavedWords(selectedTab == 1 ? WordRepository.NOTEBOOK : WordRepository.FAVORITE);
        } catch (RuntimeException e) {
            TextView error = new TextView(this);
            error.setPadding(18, 24, 18, 24);
            error.setText("读取本地词库失败：" + e.getMessage());
            container.addView(error);
        }
    }

    private void renderBooks() {
        Button importButton = new Button(this);
        importButton.setText("＋ 导入词书（CSV / TSV）");
        importButton.setAllCaps(false);
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(-1, -2);
        importLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        container.addView(importButton, importLp);
        importButton.setOnClickListener(v -> showImportInstructions());
        List<WordRepository.Book> books = WordRepository.getBooks(this);
        if (books.isEmpty()) { showEmpty("暂无词书，可点击上方导入词书"); return; }
        for (WordRepository.Book book : books) {
            View row = inflater.inflate(R.layout.item_wordbook, container, false);
            ((ImageView) row.findViewById(R.id.iv_book_icon)).setImageResource(R.drawable.ic_book);
            ((TextView) row.findViewById(R.id.tv_book_title)).setText(WordRepository.displayBookName(book.name));
            ((TextView) row.findViewById(R.id.tv_book_subtitle)).setText("已学 " + book.learned + " / " + book.total + " 词 · " + book.name);
            int percent = book.total <= 0 ? 0 : Math.min(100, book.learned * 100 / book.total);
            ((TextView) row.findViewById(R.id.tv_book_progress)).setText(percent + "%");
            row.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle(WordRepository.displayBookName(book.name))
                    .setMessage("总词数：" + book.total + "\n已学习：" + book.learned + "\n选择后将作为当前学习词书。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("开始学习", (dialog, which) -> {
                        WordRepository.selectBook(this, book.name);
                        startActivity(new Intent(this, StudyActivity.class));
                    }).show());
            container.addView(row);
            addDivider();
        }
    }


    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void showImportInstructions() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(8), dp(20), dp(8));
        TextView badge = new TextView(this); badge.setText("CSV  /  TSV  ·  UTF-8"); badge.setTextSize(12);
        badge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_green));
        badge.setPadding(dp(10), dp(7), dp(10), dp(7)); badge.setBackgroundResource(R.drawable.bg_pill_green_light);
        panel.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        TextView intro = new TextView(this);
        intro.setText("每行一个词条。第一列必须是单词，其余信息可留空；导入会创建新词书，不会覆盖已有词书。");
        intro.setTextSize(14); intro.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary)); intro.setPadding(0, dp(12), 0, dp(10)); panel.addView(intro);
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL); fields.setPadding(dp(12), dp(10), dp(12), dp(10)); fields.setBackgroundResource(R.drawable.bg_card); panel.addView(fields);
        String[][] rows = {{"word", "单词（必填）"}, {"meaning", "中文释义"}, {"phonetic", "音标"}, {"pos", "词性"}, {"sentence", "英文例句"}, {"sentence_cn", "例句中文翻译"}};
        for (String[] item : rows) {
            LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setPadding(0, dp(5), 0, dp(5));
            TextView key = new TextView(this); key.setText(item[0]); key.setTypeface(null, android.graphics.Typeface.BOLD); key.setTextSize(13); key.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_green));
            r.addView(key, new LinearLayout.LayoutParams(dp(105), -2));
            TextView desc = new TextView(this); desc.setText(item[1]); desc.setTextSize(13); desc.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary)); r.addView(desc);
            fields.addView(r);
        }
        TextView example = new TextView(this);
        example.setText("示例：\nword,meaning,phonetic,pos,sentence,sentence_cn\naccept,接受,əkˈsept,v.,I accept it.,我接受它。\n\nCSV 用逗号分隔；TSV 用制表符分隔。字段中包含分隔符时请用双引号包裹。暂不支持字段内容跨行。");
        example.setTextSize(12); example.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)); example.setPadding(0, dp(12), 0, dp(4)); panel.addView(example);
        ScrollView scroller = new ScrollView(this); scroller.setFillViewport(false); scroller.addView(panel);
        new AlertDialog.Builder(this).setTitle("导入词书 · 格式指南").setView(scroller)
                .setNegativeButton("稍后再说", null)
                .setPositiveButton("选择 CSV / TSV 文件", (dialog, which) -> {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("text/*");
                    pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/tab-separated-values", "text/plain", "application/vnd.ms-excel"});
                    startActivityForResult(pick, REQUEST_IMPORT_BOOK);
                }).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_BOOK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            String fileName = getFileName(uri);
            String bookName = fileName.replaceFirst("(?i)\\.(csv|tsv|txt)$", "").trim();
            if (bookName.isEmpty()) bookName = "导入词书";
            boolean tsv = fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".tsv");
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("\uFEFF")) line = line.substring(1);
                    if (line.trim().isEmpty()) continue;
                    rows.add(parseDelimitedLine(line, tsv ? '\t' : ','));
                }
            }
            int count = WordRepository.importBook(this, bookName, rows);
            UiUtils.toast(this, "导入成功：" + count + " 个单词");
            render();
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("导入失败")
                    .setMessage("无法导入该词书：" + (e.getMessage() == null ? "文件格式不正确" : e.getMessage()) + "\n请确认文件为 UTF-8 编码的 CSV/TSV，且第一列为单词。")
                    .setPositiveButton("知道了", null).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = "import.csv";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int index = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = c.getString(index);
            }
        } catch (Exception ignored) { }
        return name == null ? "import.csv" : name;
    }

    private String[] parseDelimitedLine(String line, char delimiter) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == delimiter && !quoted) { fields.add(field.toString()); field.setLength(0); }
            else field.append(ch);
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private void renderSavedWords(String category) {
        List<WordRepository.Word> words = WordRepository.getCategoryWords(this, category);
        if (words.isEmpty()) {
            showEmpty(category.equals(WordRepository.NOTEBOOK) ? "还没有生词。学习时可以把单词加入生词本。" : "还没有收藏。学习时可以收藏喜欢的单词。");
            return;
        }
        for (WordRepository.Word word : words) {
            View row = inflater.inflate(R.layout.item_word, container, false);
            ((TextView) row.findViewById(R.id.tv_word)).setText(word.headWord);
            ((TextView) row.findViewById(R.id.tv_meaning)).setText(word.meaning == null ? "" : word.meaning);
            ((TextView) row.findViewById(R.id.tv_word_tag)).setText(WordRepository.displayBookName(word.bookName));
            row.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle(word.headWord)
                    .setMessage((word.meaning == null ? "" : word.meaning) + "\n\n词书：" + WordRepository.displayBookName(word.bookName))
                    .setNegativeButton("关闭", null)
                    .setPositiveButton("移出列表", (dialog, which) -> {
                        WordRepository.toggleCategory(this, category, word);
                        render();
                    }).show());
            container.addView(row);
            addDivider();
        }
    }

    private void showEmpty(String message) {
        TextView empty = new TextView(this);
        empty.setPadding(20, 28, 20, 28);
        empty.setText(message);
        empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        container.addView(empty);
    }

    private void addDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, (int) getResources().getDisplayMetrics().density)));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        container.addView(divider);
    }
}
