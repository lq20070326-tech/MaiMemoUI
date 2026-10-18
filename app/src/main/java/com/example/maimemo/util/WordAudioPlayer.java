package com.example.maimemo.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads and caches pronunciation audio from Youdao dictvoice, then plays it locally. */
public final class WordAudioPlayer {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static MediaPlayer player;
    private WordAudioPlayer() { }

    public static void play(Context context, String word, Runnable onFailure) {
        if (word == null || word.trim().isEmpty()) { if (onFailure != null) onFailure.run(); return; }
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File cacheDir = new File(app.getCacheDir(), "Mp3Cache");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) { fail(onFailure); return; }
            File audio = new File(cacheDir, hash(word.trim().toLowerCase(java.util.Locale.ROOT)) + ".mp3");
            // 清除旧版本可能留下的损坏/非音频缓存，避免把错误响应当成 MP3 播放。
            if (!looksLikeMp3(audio)) {
                if (audio.exists()) audio.delete();
                HttpURLConnection connection = null;
                try {
                    String encoded = URLEncoder.encode(word.trim(), StandardCharsets.UTF_8.name());
                    boolean downloaded = false;
                    // 有道接口偶尔会返回错误文本或异常流；尝试英美两种音源，并验证 MP3 内容后再缓存。
                    for (String voiceType : new String[]{"2", "1"}) {
                        File temp = new File(cacheDir, audio.getName() + ".tmp");
                        HttpURLConnection attempt = null;
                        try {
                            URL url = new URL("https://dict.youdao.com/dictvoice?audio=" + encoded + "&type=" + voiceType);
                            attempt = (HttpURLConnection) url.openConnection();
                            attempt.setConnectTimeout(8000); attempt.setReadTimeout(12000);
                            attempt.setRequestProperty("User-Agent", "Mozilla/5.0 MaiMemoUI/1.0");
                            attempt.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.5");
                            attempt.connect();
                            if (attempt.getResponseCode() != HttpURLConnection.HTTP_OK) throw new java.io.IOException("HTTP " + attempt.getResponseCode());
                            try (InputStream in = attempt.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                                byte[] buffer = new byte[8192]; int n;
                                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                            }
                            if (!looksLikeMp3(temp)) throw new java.io.IOException("服务返回的不是有效 MP3 音频");
                            if (audio.exists()) audio.delete();
                            if (!temp.renameTo(audio)) throw new java.io.IOException("无法保存音频缓存");
                            downloaded = true; break;
                        } catch (Exception badAudio) { temp.delete(); }
                        finally { if (attempt != null) attempt.disconnect(); }
                    }
                    if (!downloaded) throw new java.io.IOException("无法获取有效音频");
                } catch (Exception e) { fail(onFailure); return; }
                finally { if (connection != null) connection.disconnect(); }
            }
            File finalAudio = audio;
            MAIN.post(() -> startPlayback(app, finalAudio, onFailure));
        });
    }

    private static synchronized void startPlayback(Context context, File audio, Runnable onFailure) {
        try {
            if (player != null) { try { player.stop(); } catch (Exception ignored) { } player.release(); player = null; }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            player.setDataSource(context, Uri.fromFile(audio));
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnErrorListener((mp, what, extra) -> { mp.release(); if (player == mp) player = null; fail(onFailure); return true; });
            player.setOnCompletionListener(mp -> { mp.release(); if (player == mp) player = null; });
            player.prepareAsync();
        } catch (Exception e) { fail(onFailure); }
    }

    private static boolean looksLikeMp3(File file) {
        if (file == null || file.length() < 256) return false;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            byte[] head = new byte[(int) Math.min(4096, raf.length())];
            raf.readFully(head);
            if (head.length >= 3 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') return true;
            for (int i = 0; i + 1 < head.length; i++) {
                int a = head[i] & 0xff, b = head[i + 1] & 0xff;
                if (a == 0xff && (b & 0xe0) == 0xe0 && (b & 0x06) != 0) return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private static void fail(Runnable callback) { if (callback != null) MAIN.post(callback); }
    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b));
            return out.toString();
        } catch (Exception e) { return Integer.toHexString(value.hashCode()); }
    }
}
