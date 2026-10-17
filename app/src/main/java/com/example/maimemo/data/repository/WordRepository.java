package com.example.maimemo.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.maimemo.data.database.LocalDatabase;
import com.example.maimemo.business.fsrs6.Fsrs6Scheduler;
import com.example.maimemo.business.fsrs6.FsrsParameterStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 所有词库、学习进度、生词本和收藏的本地数据访问入口。 */
public final class WordRepository {
    public static final String NOTEBOOK = "notebook";
    public static final String FAVORITE = "favorite";

    private WordRepository() { }

    public static final class Book {
        public final String name;
        public final int total;
        public final int learned;
        public final boolean selected;
        public Book(String name, int total, int learned, boolean selected) {
            this.name = name; this.total = total; this.learned = learned; this.selected = selected;
        }
    }

    public static final class Word {
        public final String bookName;
        public final int rank;
        public final String headWord;
        public final String meaning;
        public final String phonetic;
        public final String pos;
        public final String sentence;
        public final String sentenceCn;
        public Word(String bookName, int rank, String headWord, String meaning,
                    String phonetic, String pos, String sentence, String sentenceCn) {
            this.bookName = bookName; this.rank = rank; this.headWord = headWord;
            this.meaning = meaning; this.phonetic = phonetic; this.pos = pos;
            this.sentence = sentence; this.sentenceCn = sentenceCn;
        }
    }

    /** 统计页“最容易遗忘的单词”数据模型。 */
    public static final class ForgottenWord {
        public final Word word;
        public final int lapses;
        public ForgottenWord(Word word, int lapses) { this.word = word; this.lapses = lapses; }
    }

    public static List<Book> getBooks(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        List<Book> result = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT bookName, number, \"current\" FROM Count ORDER BY rowid", null)) {
            while (c.moveToNext()) {
                String name = c.getString(0);
                int total = c.getInt(1);
                int learned = countLearned(db, name);
                result.add(new Book(name, total, learned, c.getInt(2) != 0));
            }
        }
        return result;
    }

    public static String getCurrentBook(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT currentBookName FROM Global LIMIT 1", null)) {
            if (c.moveToFirst()) {
                String name = c.getString(0);
                if (name != null && isBook(db, name)) return name;
            }
        }
        List<Book> books = getBooks(context);
        return books.isEmpty() ? "" : books.get(0).name;
    }

    public static void selectBook(Context context, String bookName) {
        SQLiteDatabase db = LocalDatabase.get(context);
        if (!isBook(db, bookName)) return;
        db.beginTransaction();
        try {
            db.execSQL("UPDATE Count SET \"current\" = CASE WHEN bookName=? THEN 1 ELSE 0 END", new Object[]{bookName});
            db.execSQL("UPDATE Global SET currentBookName=?, currentWordNumber=0", new Object[]{bookName});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public static List<Word> getWords(Context context, String bookName, int limit, boolean onlyUnlearned) {
        SQLiteDatabase db = LocalDatabase.get(context);
        List<Word> result = new ArrayList<>();
        if (!isBook(db, bookName)) return result;
        String columns;
        if ("Goin".equals(bookName)) columns = "w.wordRank,w.hiragana,w.romaji,w.katakana,'日语','', ''";
        else if ("StdJp_Mid".equals(bookName)) columns = "w.wordRank,w.headWord,w.tranCN,CAST(w.phone AS TEXT),w.pos,'',''";
        else columns = "w.wordRank,w.headWord,w.tranCN,w.usphone,w.pos,w.sentence,w.sentenceCN";
        String sql = "SELECT " + columns + " FROM \"" + bookName + "\" w " +
                (onlyUnlearned ? "WHERE NOT EXISTS (SELECT 1 FROM app_word_state s WHERE s.book_name=? AND s.word_rank=w.wordRank) " : "") +
                "ORDER BY w.wordRank LIMIT ?";
        String[] args = onlyUnlearned ? new String[]{bookName, String.valueOf(limit)} : new String[]{String.valueOf(limit)};
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) result.add(wordFromCursor(bookName, c));
        }
        return result;
    }

    /** Stable public entry point; selection policy is replaceable through WordPushService. */
    public static Word getNextWord(Context context, String bookName) {
        return com.example.maimemo.business.wordpush.WordPushService.nextWord(context, bookName);
    }

    /** Default FSRS provider implementation: due reviews first, then new words within daily limit. */
    public static Word getNextWordFromDatabase(Context context, String bookName) {
        SQLiteDatabase db = LocalDatabase.get(context);
        String now = now();
        int reviewLimit = com.example.maimemo.storage.AppSettings.getInt(context,
                com.example.maimemo.storage.AppSettings.DAILY_REVIEW_LIMIT, 200);
        if (getTodayReviewCount(context) < reviewLimit) {
            try (Cursor c = db.rawQuery("SELECT word_rank FROM app_word_state WHERE book_name=? AND due_at IS NOT NULL AND due_at<=? ORDER BY due_at ASC LIMIT 1", new String[]{bookName, now})) {
                if (c.moveToFirst()) {
                    Word due = getWordByRank(context, bookName, c.getInt(0));
                    if (due != null) return due;
                }
            }
        }
        int newLimit = com.example.maimemo.storage.AppSettings.getInt(context,
                com.example.maimemo.storage.AppSettings.DAILY_NEW_WORDS, 50);
        if (getTodayLearnedCount(context) >= newLimit) return null;
        // New-word pushes are sampled randomly from the currently selected vocabulary book,
        // while FSRS due reviews above still keep their normal priority.
        String columns;
        if ("Goin".equals(bookName)) columns = "w.wordRank,w.hiragana,w.romaji,w.katakana,'日语','', ''";
        else if ("StdJp_Mid".equals(bookName)) columns = "w.wordRank,w.headWord,w.tranCN,CAST(w.phone AS TEXT),w.pos,'',''";
        else columns = "w.wordRank,w.headWord,w.tranCN,w.usphone,w.pos,w.sentence,w.sentenceCN";
        String sql = "SELECT " + columns + " FROM \"" + bookName + "\" w " +
                "WHERE NOT EXISTS (SELECT 1 FROM app_word_state s WHERE s.book_name=? AND s.word_rank=w.wordRank) " +
                "ORDER BY RANDOM() LIMIT 1";
        try (Cursor c = db.rawQuery(sql, new String[]{bookName})) {
            return c.moveToFirst() ? wordFromCursor(bookName, c) : null;
        }
    }

    /** Build the public delivery payload for a selected word; used by pluggable push strategies. */
    public static com.example.maimemo.business.wordpush.WordDeliveryItem buildDeliveryItem(
            Context context, Word word, String providerId) {
        SQLiteDatabase db = LocalDatabase.get(context);
        String dueAt = null;
        double stability = 0.0, difficulty = 0.0, retrievability = 0.0;
        int reviews = 0, lapses = 0;
        boolean found = false;
        try (Cursor c = db.rawQuery("SELECT due_at,stability,difficulty,review_count,lapses,last_reviewed FROM app_word_state WHERE book_name=? AND word_rank=?",
                new String[]{word.bookName, String.valueOf(word.rank)})) {
            if (c.moveToFirst()) {
                found = true; dueAt = c.isNull(0) ? null : c.getString(0); stability = c.getDouble(1);
                difficulty = c.getDouble(2); reviews = c.getInt(3); lapses = c.getInt(4);
                String last = c.isNull(5) ? null : c.getString(5);
                if (last != null && stability > 0) {
                    try { SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Date lastDay = dayFmt.parse(last.substring(0, 10));
                        Date todayDay = dayFmt.parse(dayFmt.format(new Date()));
                        double elapsed = Math.max(0, Math.round((todayDay.getTime() - lastDay.getTime()) / 86400000.0));
                        retrievability = Fsrs6Scheduler.retrievability(stability, elapsed, FsrsParameterStore.getWeights(context));
                    } catch (Exception ignored) { }
                }
            }
        }
        com.example.maimemo.business.wordpush.WordDeliveryItem.Source source = found
                ? com.example.maimemo.business.wordpush.WordDeliveryItem.Source.DUE_REVIEW
                : com.example.maimemo.business.wordpush.WordDeliveryItem.Source.NEW_WORD;
        return new com.example.maimemo.business.wordpush.WordDeliveryItem(word, source, dueAt,
                stability, difficulty, retrievability, reviews, lapses, new int[]{1, 2, 3, 4}, providerId);
    }

    public static Word getWordByRank(Context context, String bookName, int rank) {
        SQLiteDatabase db = LocalDatabase.get(context);
        if (!isBook(db, bookName)) return null;
        String columns;
        if ("Goin".equals(bookName)) columns = "wordRank,hiragana,romaji,katakana,'日语','',''";
        else if ("StdJp_Mid".equals(bookName)) columns = "wordRank,headWord,tranCN,CAST(phone AS TEXT),pos,'',''";
        else columns = "wordRank,headWord,tranCN,usphone,pos,sentence,sentenceCN";
        try (Cursor c = db.rawQuery("SELECT " + columns + " FROM \"" + bookName + "\" WHERE wordRank=? LIMIT 1", new String[]{String.valueOf(rank)})) {
            return c.moveToFirst() ? wordFromCursor(bookName, c) : null;
        }
    }

    /** Apply FSRS-6 scheduling and persist a full review event for later analytics. Rating: 1 Again, 2 Hard, 3 Good, 4 Easy. */
    public static void markLearned(Context context, Word word, int resultCode) {
        SQLiteDatabase db = LocalDatabase.get(context);
        int rating = resultCode;
        if (rating < Fsrs6Scheduler.AGAIN || rating > Fsrs6Scheduler.EASY)
            throw new IllegalArgumentException("评分必须为 FSRS 1..4");
        String timestamp = now();
        int previousReviews = 0, lapses = 0;
        double stability = 0.0, difficulty = 0.0;
        String learnedAt = timestamp, lastReviewed = null, previousDueAt = null;
        try (Cursor c = db.rawQuery("SELECT review_count,stability,difficulty,lapses,learned_at,last_reviewed,due_at FROM app_word_state WHERE book_name=? AND word_rank=?", new String[]{word.bookName, String.valueOf(word.rank)})) {
            if (c.moveToFirst()) {
                previousReviews = c.getInt(0); stability = c.getDouble(1); difficulty = c.getDouble(2);
                lapses = c.getInt(3); learnedAt = c.isNull(4) ? timestamp : c.getString(4);
                lastReviewed = c.isNull(5) ? null : c.getString(5);
                previousDueAt = c.isNull(6) ? null : c.getString(6);
            }
        }
        double elapsedDays = 0.0;
        if (lastReviewed != null) {
            try {
                // FSRS receives elapsed calendar days; same-calendar-day reviews use elapsed_days=0.
                SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date lastDay = dayFmt.parse(lastReviewed.substring(0, 10));
                Date todayDay = dayFmt.parse(dayFmt.format(new Date()));
                elapsedDays = Math.max(0.0, Math.round((todayDay.getTime() - lastDay.getTime()) / 86400000.0));
            } catch (Exception ignored) { }
        }
        double beforeStability = stability, beforeDifficulty = difficulty;
        double[] activeWeights = FsrsParameterStore.getWeights(context);
        Fsrs6Scheduler.Result scheduled = Fsrs6Scheduler.review(stability, difficulty, previousReviews,
                lapses, rating, elapsedDays, Fsrs6Scheduler.DEFAULT_RETENTION, activeWeights);
        long delayMillis = Math.max(86400000L, Math.round(scheduled.intervalDays * 86400000.0));
        String dueAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(System.currentTimeMillis() + delayMillis));
        boolean isReview = previousReviews > 0;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("book_name", word.bookName); values.put("word_rank", word.rank);
            values.put("learned_at", learnedAt); values.put("last_reviewed", timestamp);
            values.put("review_count", previousReviews + 1); values.put("last_result", rating);
            values.put("due_at", dueAt); values.put("stability", scheduled.stability);
            values.put("difficulty", scheduled.difficulty); values.put("lapses", scheduled.lapses);
            values.put("last_rating", rating); values.put("last_elapsed_days", elapsedDays);
            values.put("last_scheduled_days", scheduled.intervalDays); values.put("last_retrievability", scheduled.retrievability);
            values.put("created_at", learnedAt); values.put("updated_at", timestamp);
            db.insertWithOnConflict("app_word_state", null, values, SQLiteDatabase.CONFLICT_REPLACE);

            ContentValues log = new ContentValues();
            log.put("book_name", word.bookName); log.put("word_rank", word.rank);
            log.put("result", rating); log.put("rating", rating); log.put("learned_at", timestamp);
            log.put("is_review", isReview ? 1 : 0); log.put("event_type", isReview ? "review" : "learn");
            log.put("stability_before", beforeStability); log.put("difficulty_before", beforeDifficulty);
            log.put("stability_after", scheduled.stability); log.put("difficulty_after", scheduled.difficulty);
            log.put("elapsed_days", elapsedDays); log.put("scheduled_days", scheduled.intervalDays);
            log.put("retrievability_before", scheduled.retrievability); log.put("due_at_after", dueAt);
            db.insert("app_learning_log", null, log);
            db.execSQL("UPDATE Global SET currentWordNumber=?", new Object[]{word.rank});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    /** 当前已学习卡片的平均稳定性（天）。 */
    public static double getAverageStability(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT AVG(stability) FROM app_word_state", null)) {
            return c.moveToFirst() && !c.isNull(0) ? c.getDouble(0) : 0.0;
        }
    }

    /** 记忆稳定性分布：短期(<3天)、中期(3-21天)、长期(>21天)。 */
    public static int[] getStabilityDistribution(Context context) {
        int[] values = new int[3];
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT CASE WHEN stability<3 THEN 0 WHEN stability<=21 THEN 1 ELSE 2 END bucket, COUNT(*) FROM app_word_state GROUP BY bucket", null)) {
            while (c.moveToNext()) values[c.getInt(0)] = c.getInt(1);
        }
        return values;
    }

    /** 按天统计最近一段时间内首次学习的词数，用于记忆持久度趋势图。 */
    public static int[] getRecentNewWordCounts(Context context, int days) {
        int[] result = new int[days];
        SQLiteDatabase db = LocalDatabase.get(context);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            java.util.Calendar date = (java.util.Calendar) calendar.clone(); date.add(java.util.Calendar.DAY_OF_MONTH, -i);
            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()) + "%";
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE is_review=0 AND learned_at LIKE ?", new String[]{key})) {
                result[days - 1 - i] = c.moveToFirst() ? c.getInt(0) : 0;
            }
        }
        return result;
    }

    /** 最近 N 天每天复习/学习后记录的平均稳定性（天）；没有事件的日期为 0。 */
    public static float[] getRecentAverageStability(Context context, int days) {
        float[] result = new float[Math.max(0, days)];
        SQLiteDatabase db = LocalDatabase.get(context);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            java.util.Calendar date = (java.util.Calendar) calendar.clone();
            date.add(java.util.Calendar.DAY_OF_MONTH, -i);
            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()) + "%";
            try (Cursor c = db.rawQuery("SELECT AVG(stability_after) FROM app_learning_log WHERE learned_at LIKE ? AND stability_after>0", new String[]{key})) {
                result[days - 1 - i] = c.moveToFirst() && !c.isNull(0) ? (float) c.getDouble(0) : 0f;
            }
        }
        return result;
    }

    /** 返回遗忘次数最多的前三个词，按数据库 FSRS lapse 计数排序，不使用演示词。 */
    public static List<ForgottenWord> getMostForgottenWords(Context context, int limit) {
        List<ForgottenWord> result = new ArrayList<>();
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT book_name,word_rank,lapses FROM app_word_state WHERE lapses>0 ORDER BY lapses DESC, last_reviewed ASC LIMIT ?", new String[]{String.valueOf(Math.max(1, limit))})) {
            while (c.moveToNext()) {
                String book = c.getString(0); int rank = c.getInt(1); int lapses = c.getInt(2);
                Word word = getWordByRank(context, book, rank);
                if (word != null) result.add(new ForgottenWord(word, lapses));
            }
        }
        return result;
    }

    /** 未来 24 小时内到期的复习卡片数，用于遗忘临界点提醒。 */
    public static int getDueInNext24HoursCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        String current = now();
        String end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000));
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_word_state WHERE due_at>? AND due_at<=?", new String[]{current, end})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public static int getTotalReviewCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE is_review=1", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    /** 根据 FSRS 稳定性与最后复习时间估算当前总体记忆保持率。 */
    public static int getCurrentRetentionPercent(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        double total = 0; int count = 0; long nowMillis = System.currentTimeMillis();
        try (Cursor c = db.rawQuery("SELECT stability,last_reviewed FROM app_word_state", null)) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            while (c.moveToNext()) {
                double stability = Math.max(0.1, c.getDouble(0));
                double elapsed = 0;
                try { String last = c.getString(1); if (last != null) elapsed = Math.max(0, (nowMillis - fmt.parse(last).getTime()) / 86400000.0); } catch (Exception ignored) { }
                total += Fsrs6Scheduler.retrievability(stability, elapsed, FsrsParameterStore.getWeights(context)); count++;
            }
        }
        return count == 0 ? 0 : (int) Math.round(total * 100.0 / count);
    }

    public static double getAverageReviewInterval(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT AVG(julianday(due_at)-julianday(last_reviewed)) FROM app_word_state WHERE due_at IS NOT NULL AND last_reviewed IS NOT NULL", null)) {
            return c.moveToFirst() && !c.isNull(0) ? Math.max(0, c.getDouble(0)) : 0;
        }
    }

    /** 指定经过时间下，按当前所有已学习卡片的 FSRS-6 模型估算平均记忆保持率。 */
    public static int getRetentionAtElapsedDays(Context context, double elapsedDays) {
        SQLiteDatabase db = LocalDatabase.get(context);
        double total = 0; int count = 0; long nowMillis = System.currentTimeMillis();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try (Cursor c = db.rawQuery("SELECT stability,last_reviewed FROM app_word_state WHERE stability>0", null)) {
            while (c.moveToNext()) {
                double elapsed = elapsedDays;
                try { String last = c.getString(1); if (last != null) elapsed += Math.max(0, (nowMillis - fmt.parse(last).getTime()) / 86400000.0); } catch (Exception ignored) { }
                total += Fsrs6Scheduler.retrievability(c.getDouble(0), elapsed, FsrsParameterStore.getWeights(context));
                count++;
            }
        }
        return count == 0 ? 0 : (int) Math.round(total * 100.0 / count);
    }

    public static float[] getRetentionCurve(Context context) {
        float[] values = new float[]{0, 0, 0, 0, 0, 0, 0};
        SQLiteDatabase db = LocalDatabase.get(context);
        double[] days = new double[]{0, 0.25, 1, 2, 3, 5, 7};
        double[] totals = new double[days.length]; int count = 0;
        long nowMillis = System.currentTimeMillis();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try (Cursor c = db.rawQuery("SELECT stability,last_reviewed FROM app_word_state", null)) {
            while (c.moveToNext()) {
                double stability = Math.max(0.1, c.getDouble(0));
                double elapsed = 0;
                try { String last = c.getString(1); if (last != null) elapsed = Math.max(0, (nowMillis - fmt.parse(last).getTime()) / 86400000.0); } catch (Exception ignored) { }
                for (int i = 0; i < days.length; i++) totals[i] += Fsrs6Scheduler.retrievability(stability, elapsed + days[i], FsrsParameterStore.getWeights(context));
                count++;
            }
        }
        if (count > 0) for (int i = 0; i < days.length; i++) values[i] = (float) (totals[i] * 100.0 / count);
        return values;
    }

    public static int getLearnedCount(Context context, String bookName) {
        return countLearned(LocalDatabase.get(context), bookName);
    }

    public static int[] getTodayResultCounts(Context context) {
        int[] result = new int[3];
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT result, COUNT(*) FROM app_learning_log WHERE learned_at LIKE ? GROUP BY result", new String[]{today() + "%"})) {
            while (c.moveToNext()) {
                int code = c.getInt(0);
                int bucket = code >= 3 ? 0 : (code == 2 ? 1 : 2);
                result[bucket] = c.getInt(1);
            }
        }
        return result;
    }

    public static int getTodayReviewCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ? AND is_review=1", new String[]{today() + "%"})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public static int getTodayLearnedCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ? AND is_review=0", new String[]{today() + "%"})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }


    /** Number of unique cards first learned today (not every answer event). */
    public static int getTodayNewCardCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ? AND is_review=0", new String[]{today() + "%"})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Number of review answers today, independent of new-card quota. */
    public static int getTodayReviewEventCount(Context context) { return getTodayReviewCount(context); }

    /** Recall success rate from actual review ratings (Hard/Good/Easy count as recall). */
    public static int getTodayRecallRatePercent(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN result>=2 THEN 1 ELSE 0 END) FROM app_learning_log WHERE learned_at LIKE ?", new String[]{today() + "%"})) {
            if (!c.moveToFirst() || c.getInt(0) == 0) return 0;
            return Math.round(c.getInt(1) * 100.0f / c.getInt(0));
        }
    }

    /** Total number of persisted rating events, useful for consistency checks. */
    public static int getTotalLearningEventCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    public static boolean hasDueReview(Context context, String bookName) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT 1 FROM app_word_state WHERE book_name=? AND due_at IS NOT NULL AND due_at<=? LIMIT 1", new String[]{bookName, now()})) {
            return c.moveToFirst();
        }
    }

    public static int getDueReviewCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_word_state WHERE due_at IS NOT NULL AND due_at<=?", new String[]{now()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** 返回指定年月每天的统计数组，顺序为 [认识, 模糊/需复习, 不认识]。 */
    public static int[][] getMonthStats(Context context, int year, int month, int daysInMonth) {
        int[][] result = new int[daysInMonth + 1][3];
        SQLiteDatabase db = LocalDatabase.get(context);
        String prefix = String.format(Locale.US, "%04d-%02d-", year, month);
        try (Cursor c = db.rawQuery("SELECT substr(learned_at,9,2), result, COUNT(*) FROM app_learning_log WHERE learned_at LIKE ? GROUP BY substr(learned_at,9,2), result", new String[]{prefix + "%"})) {
            while (c.moveToNext()) {
                int day;
                try { day = Integer.parseInt(c.getString(0)); } catch (Exception e) { continue; }
                int resultCode = c.getInt(1);
                if (day >= 1 && day <= daysInMonth) {
                    int bucket = resultCode >= 3 ? 0 : (resultCode == 2 ? 1 : 2);
                    result[day][bucket] = c.getInt(2);
                }
            }
        }
        return result;
    }

    public static int[] getRecentDailyCounts(Context context, int days) {
        int[] result = new int[days];
        SQLiteDatabase db = LocalDatabase.get(context);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            java.util.Calendar date = (java.util.Calendar) calendar.clone();
            date.add(java.util.Calendar.DAY_OF_MONTH, -i);
            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime()) + "%";
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ?", new String[]{key})) {
                result[days - 1 - i] = c.moveToFirst() ? c.getInt(0) : 0;
            }
        }
        return result;
    }

    public static int getMonthTotal(Context context, int year, int month) {
        SQLiteDatabase db = LocalDatabase.get(context);
        String prefix = String.format(Locale.US, "%04d-%02d-", year, month);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ?", new String[]{prefix + "%"})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public static int getStreakDays(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        int streak = 0;
        java.util.Calendar day = java.util.Calendar.getInstance();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime());
        boolean studiedToday;
        try (Cursor c = db.rawQuery("SELECT 1 FROM app_learning_log WHERE learned_at LIKE ? LIMIT 1", new String[]{todayDate + "%"})) { studiedToday = c.moveToFirst(); }
        if (!studiedToday) day.add(java.util.Calendar.DAY_OF_MONTH, -1);
        for (int i = 0; i < 366; i++) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime());
            boolean has;
            try (Cursor c = db.rawQuery("SELECT 1 FROM app_learning_log WHERE learned_at LIKE ? LIMIT 1", new String[]{date + "%"})) { has = c.moveToFirst(); }
            if (!has) break;
            streak++;
            day.add(java.util.Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }

    public static boolean isInCategory(Context context, String category, Word word) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT 1 FROM app_user_words WHERE category=? AND book_name=? AND word_rank=? LIMIT 1",
                new String[]{category, word.bookName, String.valueOf(word.rank)})) { return c.moveToFirst(); }
    }

    public static boolean toggleCategory(Context context, String category, Word word) {
        SQLiteDatabase db = LocalDatabase.get(context);
        if (isInCategory(context, category, word)) {
            db.delete("app_user_words", "category=? AND book_name=? AND word_rank=?",
                    new String[]{category, word.bookName, String.valueOf(word.rank)});
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("category", category); values.put("book_name", word.bookName);
        values.put("word_rank", word.rank); values.put("head_word", word.headWord);
        values.put("meaning", word.meaning); values.put("phonetic", word.phonetic); values.put("created_at", now());
        db.insertWithOnConflict("app_user_words", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return true;
    }

    public static List<Word> getCategoryWords(Context context, String category) {
        SQLiteDatabase db = LocalDatabase.get(context);
        List<Word> result = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT book_name,word_rank,head_word,meaning,phonetic FROM app_user_words WHERE category=? ORDER BY created_at DESC", new String[]{category})) {
            while (c.moveToNext()) result.add(new Word(c.getString(0), c.getInt(1), c.getString(2), c.getString(3), c.getString(4), "", "", ""));
        }
        return result;
    }

    public static List<Word> search(Context context, String query, int limit) {
        SQLiteDatabase db = LocalDatabase.get(context);
        List<Word> result = new ArrayList<>();
        String term = "%" + query.trim() + "%";
        try (Cursor books = db.rawQuery("SELECT bookName FROM Count ORDER BY rowid", null)) {
            while (books.moveToNext() && result.size() < limit) {
                String book = books.getString(0);
                String sql;
                if ("Goin".equals(book)) {
                    sql = "SELECT wordRank,hiragana,romaji,katakana,'日语','','' FROM Goin WHERE hiragana LIKE ? OR romaji LIKE ? OR katakana LIKE ? LIMIT ?";
                } else if ("StdJp_Mid".equals(book)) {
                    sql = "SELECT wordRank,headWord,tranCN,CAST(phone AS TEXT),pos,'','' FROM StdJp_Mid WHERE headWord LIKE ? OR tranCN LIKE ? LIMIT ?";
                } else {
                    sql = "SELECT wordRank,headWord,tranCN,usphone,pos,sentence,sentenceCN FROM \"" + book + "\" WHERE headWord LIKE ? OR tranCN LIKE ? LIMIT ?";
                }
                String[] args = "Goin".equals(book) ? new String[]{term, term, term, String.valueOf(limit - result.size())} : new String[]{term, term, String.valueOf(limit - result.size())};
                try (Cursor c = db.rawQuery(sql, args)) {
                    while (c.moveToNext() && result.size() < limit) result.add(wordFromCursor(book, c));
                }
            }
        }
        return result;
    }

    public static String displayBookName(String name) {
        switch (name) {
            case "CET4_1": return "四级核心词汇";
            case "CET4_3": return "四级高频词汇";
            case "CET6_1": return "六级核心词汇";
            case "CET6_3": return "六级高频词汇";
            case "KaoYan_1": return "考研核心词汇";
            case "KaoYan_2": return "考研高频词汇";
            case "TOEFL_2": return "托福核心词汇";
            case "IELTS_3": return "雅思核心词汇";
            case "GRE_2": return "GRE 核心词汇";
            case "GMAT_3": return "GMAT 核心词汇";
            case "SAT_2": return "SAT 核心词汇";
            case "Level4_1": return "初中四级词汇";
            case "Level4luan_2": return "四级乱序词汇";
            case "Level8_1": return "高中核心词汇";
            case "Level8luan_2": return "高中乱序词汇";
            case "StdJp_Mid": return "日语中级词汇";
            case "Goin": return "日语五十音";
            default: return name.startsWith("Import_") ? name.substring("Import_".length()).replace('_', ' ') : name.replace('_', ' ');
        }
    }

    /** 词书学习进度：从 Count 词书总量与 app_word_state 学习卡片数计算。 */
    public static List<Book> getBookProgress(Context context) {
        List<Book> books = getBooks(context);
        java.util.Collections.sort(books, (a, b) -> {
            int cmp = Integer.compare(b.learned, a.learned);
            return cmp != 0 ? cmp : Integer.compare(b.total, a.total);
        });
        return books;
    }

    /** 当前数据库中真实的词书数量（含导入词书）。 */
    public static int getBookCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM Count", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** 从 CSV/TSV 导入一册词书。字段：word,meaning,phonetic,pos,sentence,sentence_cn；支持表头。 */
    public static int importBook(Context context, String requestedName, List<String[]> rows) {
        if (requestedName == null || requestedName.trim().isEmpty()) throw new IllegalArgumentException("词书名称不能为空");
        if (rows == null || rows.isEmpty()) throw new IllegalArgumentException("文件中没有可导入的单词行");
        SQLiteDatabase db = LocalDatabase.get(context);
        String base = requestedName.trim().replaceAll("[^\\p{L}\\p{N}_-]", "_");
        if (base.length() > 32) base = base.substring(0, 32);
        if (base.isEmpty()) base = "ImportedBook";
        String table = "Import_" + base;
        int suffix = 2;
        while (tableExists(db, table) || isBook(db, table)) table = "Import_" + base + "_" + suffix++;
        int imported = 0;
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE \"" + table + "\" (wordRank INTEGER PRIMARY KEY, headWord TEXT NOT NULL, tranCN TEXT, usphone TEXT, pos TEXT, sentence TEXT, sentenceCN TEXT, questionType TEXT)");
            int rank = 1;
            for (String[] row : rows) {
                if (row == null || row.length == 0) continue;
                String word = field(row, 0).trim();
                if (word.isEmpty() || word.equalsIgnoreCase("word") || word.equalsIgnoreCase("headWord")) continue;
                ContentValues values = new ContentValues();
                values.put("wordRank", rank++); values.put("headWord", word);
                values.put("tranCN", field(row, 1)); values.put("usphone", field(row, 2));
                values.put("pos", field(row, 3)); values.put("sentence", field(row, 4)); values.put("sentenceCN", field(row, 5)); values.put("questionType", field(row, 6));
                db.insertOrThrow(table, null, values); imported++;
            }
            if (imported == 0) throw new IllegalArgumentException("没有识别到有效单词，请检查 CSV/TSV 的第一列是否为单词");
            ContentValues count = new ContentValues(); count.put("bookName", table); count.put("number", imported); count.put("current", 0);
            db.insertOrThrow("Count", null, count);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        return imported;
    }


    /** 获取同一本词书中的选择题干扰项，确保答案来自当前词书。 */
    public static List<Word> getQuizOptions(Context context, Word correct, int optionCount) {
        List<Word> pool = getWords(context, correct.bookName, 5000, false);
        List<Word> candidates = new ArrayList<>();
        for (Word w : pool) {
            if (!w.headWord.equalsIgnoreCase(correct.headWord) && w.meaning != null && !w.meaning.trim().isEmpty()) candidates.add(w);
        }
        java.util.Collections.shuffle(candidates);
        List<Word> result = new ArrayList<>(); result.add(correct);
        for (Word w : candidates) { if (result.size() >= optionCount) break; result.add(w); }
        java.util.Collections.shuffle(result);
        return result;
    }

    /** 导入词书的可选题型列；内置词书没有该列时返回空字符串。 */
    public static String getQuestionType(Context context, Word word) {
        SQLiteDatabase db = LocalDatabase.get(context);
        if (word == null || !isBook(db, word.bookName)) return "";
        try (Cursor c = db.rawQuery("PRAGMA table_info(\"" + word.bookName.replace("\"", "") + "\")", null)) {
            boolean has = false; while (c.moveToNext()) if ("questionType".equalsIgnoreCase(c.getString(1))) { has = true; break; }
            if (!has) return "";
        } catch (Throwable ignored) { return ""; }
        try (Cursor c = db.rawQuery("SELECT questionType FROM \"" + word.bookName + "\" WHERE wordRank=?", new String[]{String.valueOf(word.rank)})) {
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "";
        } catch (Throwable ignored) { return ""; }
    }

    private static String field(String[] row, int index) { return index < row.length && row[index] != null ? row[index] : ""; }
    private static boolean tableExists(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1", new String[]{name})) { return c.moveToFirst(); }
    }


    /** 学习时段分布：凌晨(0-5)、上午(6-11)、下午(12-17)、晚间(18-23)。 */
    public static int[] getStudyTimeDistribution(Context context) {
        int[] result = new int[4];
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT CAST(substr(learned_at,12,2) AS INTEGER) hour, COUNT(*) FROM app_learning_log GROUP BY hour", null)) {
            while (c.moveToNext()) {
                int hour = c.getInt(0), bucket = hour < 12 ? 0 : (hour < 18 ? 1 : 2);
                result[bucket] += c.getInt(1);
            }
        }
        return result;
    }

    /** 本周/上周学习事件数，按自然周周一到周日。 */
    public static int[] getThisAndLastWeekTotals(Context context) {
        int[] result = new int[2];
        SQLiteDatabase db = LocalDatabase.get(context);
        java.util.Calendar start = java.util.Calendar.getInstance();
        start.set(java.util.Calendar.HOUR_OF_DAY, 0); start.set(java.util.Calendar.MINUTE, 0);
        start.set(java.util.Calendar.SECOND, 0); start.set(java.util.Calendar.MILLISECOND, 0);
        int dow = start.get(java.util.Calendar.DAY_OF_WEEK);
        int backToMonday = (dow + 5) % 7;
        start.add(java.util.Calendar.DAY_OF_MONTH, -backToMonday);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
        String thisStart = fmt.format(start.getTime());
        java.util.Calendar next = (java.util.Calendar) start.clone(); next.add(java.util.Calendar.DAY_OF_MONTH, 7);
        String nextStart = fmt.format(next.getTime());
        java.util.Calendar previous = (java.util.Calendar) start.clone(); previous.add(java.util.Calendar.DAY_OF_MONTH, -7);
        String prevStart = fmt.format(previous.getTime());
        try (Cursor c = db.rawQuery("SELECT SUM(CASE WHEN learned_at>=? AND learned_at<? THEN 1 ELSE 0 END), SUM(CASE WHEN learned_at>=? AND learned_at<? THEN 1 ELSE 0 END) FROM app_learning_log", new String[]{thisStart, nextStart, prevStart, thisStart})) {
            if (c.moveToFirst()) { result[0] = c.isNull(0) ? 0 : c.getInt(0); result[1] = c.isNull(1) ? 0 : c.getInt(1); }
        }
        return result;
    }

    /** 本周每日学习事件数，数组从周一到今天/周日（未来日期自然为 0）。 */
    public static int[] getCurrentWeekDailyCounts(Context context) {
        int[] result = new int[7];
        SQLiteDatabase db = LocalDatabase.get(context);
        java.util.Calendar day = java.util.Calendar.getInstance();
        int dow = day.get(java.util.Calendar.DAY_OF_WEEK); day.add(java.util.Calendar.DAY_OF_MONTH, -((dow + 5) % 7));
        for (int i=0;i<7;i++) {
            String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime()) + "%";
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_learning_log WHERE learned_at LIKE ?", new String[]{key})) { result[i] = c.moveToFirst() ? c.getInt(0) : 0; }
            day.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        return result;
    }

    /** 数据驱动的提升建议：根据学习日志和 FSRS 状态生成最多三条建议。 */
    public static List<String> getImprovementSuggestions(Context context) {
        List<String> out = new ArrayList<>();
        int today = getTodayLearnedCount(context), due = getDueReviewCount(context), accuracy = getTodayRecallRatePercent(context);
        int[] week = getThisAndLastWeekTotals(context);
        if (due > 0) out.add("当前有 " + due + " 个单词已到复习时间，建议先完成到期复习。");
        if (today == 0) out.add("今天还没有学习记录，可以先完成一小组新词建立连续学习习惯。");
        if (accuracy > 0 && accuracy < 75) out.add("今日回忆成功率为 " + accuracy + "% ，建议放慢新词速度并重点复习不熟悉的词。");
        if (week[0] < week[1]) out.add("本周学习量比上周少 " + (week[1] - week[0]) + " 次，可以安排固定的每日学习时段。");
        if (out.isEmpty()) out.add("学习状态稳定，继续按到期时间复习，并保持规律学习。");
        while (out.size() > 3) out.remove(out.size()-1);
        return out;
    }

    /** 我的页累计学习卡片数（去重）。 */
    public static int getTotalLearnedCardCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_word_state", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    /** 我的页已掌握数：稳定性至少 21 天且最近一次评分为 Good/Easy。 */
    public static int getMasteredCardCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_word_state WHERE stability>=21 AND last_result>=3", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    /** 实际有学习日志的自然日数量。 */
    public static int getActiveStudyDays(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(DISTINCT substr(learned_at,1,10)) FROM app_learning_log", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    /** 今日新学卡片去重计数，避免同一新词重复事件被重复计数。 */
    public static int getTodayUniqueNewCardCount(Context context) {
        SQLiteDatabase db = LocalDatabase.get(context);
        try (Cursor c = db.rawQuery("SELECT COUNT(DISTINCT book_name || ':' || word_rank) FROM app_learning_log WHERE learned_at LIKE ? AND is_review=0", new String[]{today()+"%"})) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    private static Word wordFromCursor(String book, Cursor c) {
        return new Word(book, c.getInt(0), safe(c, 1), safe(c, 2), safe(c, 3), safe(c, 4), safe(c, 5), safe(c, 6));
    }
    private static String safe(Cursor c, int index) { return c.isNull(index) ? "" : c.getString(index); }
    private static int countLearned(SQLiteDatabase db, String bookName) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM app_word_state WHERE book_name=?", new String[]{bookName})) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }
    private static boolean isBook(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM Count WHERE bookName=? LIMIT 1", new String[]{name})) { return c.moveToFirst(); }
    }
    private static String now() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()); }
    private static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
}
