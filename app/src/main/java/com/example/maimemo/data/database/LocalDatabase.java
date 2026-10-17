package com.example.maimemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** App 私有数据库入口。首次启动时将内置词库复制到 databases/inami.db。 */
public final class LocalDatabase {
    private static final String DB_NAME = "inami.db";
    private static SQLiteDatabase database;

    private LocalDatabase() { }

    public static synchronized SQLiteDatabase get(Context context) {
        if (database != null && database.isOpen()) return database;
        Context app = context.getApplicationContext();
        File dbFile = app.getDatabasePath(DB_NAME);
        if (!dbFile.exists() || dbFile.length() == 0) {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new SQLiteException("无法创建数据库目录: " + parent);
            }
            File tempFile = new File(parent, DB_NAME + ".tmp");
            try (InputStream input = app.getAssets().open(DB_NAME);
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[64 * 1024];
                int length;
                while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
                output.getFD().sync();
            } catch (IOException e) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                throw new SQLiteException("复制内置词库失败: " + e.getMessage());
            }
            // 仅在完整复制成功后替换正式数据库，防止异常中断留下半个数据库。
            //noinspection ResultOfMethodCallIgnored
            dbFile.delete();
            if (!tempFile.renameTo(dbFile)) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                throw new SQLiteException("无法完成本地词库安装，请检查手机剩余空间");
            }
        }
        try {
            database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
            // 验证核心词表，防止损坏的文件导致后续页面黑屏退出。
            try (android.database.Cursor check = database.rawQuery("SELECT bookName FROM Count LIMIT 1", null)) {
                if (!check.moveToFirst()) throw new SQLiteException("内置词库没有可用词书");
            }
            createAppTables(database);
            return database;
        } catch (RuntimeException e) {
            if (database != null && database.isOpen()) database.close();
            database = null;
            throw e;
        }
    }

    private static void createAppTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS app_word_state (" +
                "book_name TEXT NOT NULL, word_rank INTEGER NOT NULL, learned_at TEXT, " +
                "last_reviewed TEXT, review_count INTEGER NOT NULL DEFAULT 0, " +
                "last_result INTEGER NOT NULL DEFAULT 0, due_at TEXT, " +
                "stability REAL NOT NULL DEFAULT 1.0, difficulty REAL NOT NULL DEFAULT 5.0, " +
                "PRIMARY KEY(book_name, word_rank))");
        ensureColumn(db, "app_word_state", "due_at", "TEXT");
        ensureColumn(db, "app_word_state", "stability", "REAL NOT NULL DEFAULT 1.0");
        ensureColumn(db, "app_word_state", "difficulty", "REAL NOT NULL DEFAULT 5.0");
        ensureColumn(db, "app_word_state", "lapses", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(db, "app_word_state", "last_rating", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(db, "app_word_state", "last_elapsed_days", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_word_state", "last_scheduled_days", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_word_state", "last_retrievability", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_word_state", "created_at", "TEXT");
        ensureColumn(db, "app_word_state", "updated_at", "TEXT");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_word_state_due ON app_word_state(due_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_user_words (" +
                "category TEXT NOT NULL, book_name TEXT NOT NULL, word_rank INTEGER NOT NULL, " +
                "head_word TEXT NOT NULL, meaning TEXT, phonetic TEXT, created_at TEXT NOT NULL, " +
                "PRIMARY KEY(category, book_name, word_rank))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_user_words_category ON app_user_words(category)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_learning_log (id INTEGER PRIMARY KEY AUTOINCREMENT, book_name TEXT NOT NULL, word_rank INTEGER NOT NULL, result INTEGER NOT NULL, learned_at TEXT NOT NULL, is_review INTEGER NOT NULL DEFAULT 0)");
        ensureColumn(db, "app_learning_log", "is_review", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "stability_after", "REAL NOT NULL DEFAULT 1.0");
        ensureColumn(db, "app_learning_log", "difficulty_after", "REAL NOT NULL DEFAULT 5.0");
        ensureColumn(db, "app_learning_log", "elapsed_days", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "rating", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "event_type", "TEXT");
        ensureColumn(db, "app_learning_log", "stability_before", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "difficulty_before", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "scheduled_days", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "retrievability_before", "REAL NOT NULL DEFAULT 0");
        ensureColumn(db, "app_learning_log", "due_at_after", "TEXT");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_learning_log_date ON app_learning_log(learned_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_app_learning_log_card ON app_learning_log(book_name, word_rank, learned_at)");
    }

    private static void ensureColumn(SQLiteDatabase db, String table, String column, String declaration) {
        boolean found = false;
        try (android.database.Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) if (column.equalsIgnoreCase(c.getString(1))) { found = true; break; }
        }
        if (!found) db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + declaration);
    }
}
