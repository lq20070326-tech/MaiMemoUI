package com.example.maimemo.business.autostudy;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;

import com.example.maimemo.R;
import com.example.maimemo.business.wordpush.WordDeliveryItem;
import com.example.maimemo.business.wordpush.WordPushService;
import com.example.maimemo.data.repository.WordRepository;
import com.example.maimemo.ui.autostudy.AutoStudyActivity;

/** Maintains a user-started automatic study session and cascades presentation: Activity -> full-screen/high-priority notification fallback. */
public final class AutoStudyForegroundService extends Service {
    private static final String TAG = "AutoStudyService";
    private static final long PRESENTATION_WATCHDOG_MS = 2200L;
    private static final long RATING_NEXT_WORD_MS = 5000L;
    private static final long DISMISS_NEXT_WORD_MS = 180L;
    private static final long DEFAULT_FREE_WINDOW_MS = 5000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String bookName;
    private WordRepository.Word pendingWord;
    private boolean pendingGroupNewWord;
    private String pendingSource = "";
    private boolean presentationConfirmed;
    private boolean overlayShown;
    private boolean popupRetryAttempted;
    private boolean stopped;
    private final Runnable fallbackRunnable = this::tryOverlayThenNotify;
    private final Runnable nextWordRunnable = this::deliverNext;

    public static void start(Context context) {
        Intent i = new Intent(context, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_START);
        ContextCompat.startForegroundService(context, i);
    }
    public static void stop(Context context) {
        Intent i = new Intent(context, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_STOP);
        context.startService(i);
    }

    /** Always prefer the app's live current wordbook. auto_study_book is only a legacy fallback;
     * it may be stale after the user switches books from the home/library screen. */
    private String resolveSelectedBook() {
        String current = WordRepository.getCurrentBook(this);
        if (current != null && !current.trim().isEmpty()) return current;
        String configured = com.example.maimemo.storage.AppSettings.getString(this, "auto_study_book", "");
        return configured == null ? "" : configured;
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(AutoStudyContract.NOTIFICATION_ID, buildOngoingNotification("自动学习已启动"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? AutoStudyContract.ACTION_START : intent.getAction();
        if (AutoStudyContract.ACTION_STOP.equals(action)) {
            stopped = true; handler.removeCallbacksAndMessages(null); removeOverlay(); stopForeground(true); stopSelf(); return START_NOT_STICKY;
        }
        if (AutoStudyContract.ACTION_PRESENTED.equals(action)) {
            // The in-app study UI owns the current word. Keep the external presenter in sync.
            presentationConfirmed = true;
            overlayShown = false;
            handler.removeCallbacks(fallbackRunnable);
            removeOverlay();
            cancelWordNotification();
            return START_STICKY;
        }
        if (AutoStudyContract.ACTION_OVERLAY_PRESENTED.equals(action)) {
            // External UI is visible; the service and Activity share the same pending word
            // and WordRepository/AutoStudyGroupQueue state. Do not open a second presenter.
            presentationConfirmed = true;
            overlayShown = true;
            handler.removeCallbacks(fallbackRunnable);
            cancelWordNotification();
            updateOngoing("正在弹窗学习：" + (pendingWord == null ? "当前单词" : pendingWord.headWord));
            return START_STICKY;
        }
        if (AutoStudyContract.ACTION_OVERLAY_DISMISSED.equals(action)) {
            overlayShown = false;
            presentationConfirmed = false;
            handler.removeCallbacks(fallbackRunnable);
            handler.postDelayed(fallbackRunnable, 500L);
            return START_STICKY;
        }
        if (AutoStudyContract.ACTION_NOT_VISIBLE.equals(action)) {
            // Activity left the foreground. Use a short grace period to avoid racing
            // with lifecycle transitions, then fall back only if it is still not visible.
            presentationConfirmed = false;
            handler.removeCallbacks(fallbackRunnable);
            handler.postDelayed(fallbackRunnable, 700L);
            return START_STICKY;
        }
        if (AutoStudyContract.ACTION_ANSWERED.equals(action)) {
            if (pendingWord == null) return START_STICKY;
            String selected = resolveSelectedBook();
            boolean bookChanged = selected != null && !selected.isEmpty()
                    && pendingWord.bookName != null && !selected.equals(pendingWord.bookName);
            if (bookChanged) {
                AutoStudyGroupQueue.resetForBookSwitch(this, selected);
                bookName = selected;
            } else if (pendingGroupNewWord) {
                AutoStudyGroupQueue.advance(this, pendingWord.bookName, pendingWord.rank);
            }
            pendingGroupNewWord = false;
            pendingWord = null;
            pendingSource = null;
            handler.removeCallbacks(fallbackRunnable); handler.removeCallbacks(nextWordRunnable); removeOverlay(); cancelWordNotification();
            handler.postDelayed(nextWordRunnable, RATING_NEXT_WORD_MS); return START_STICKY;
        }
        if (AutoStudyContract.ACTION_DISMISS.equals(action)) {
            pendingWord = null;
            pendingSource = null;
            pendingGroupNewWord = false;
            handler.removeCallbacks(fallbackRunnable); handler.removeCallbacks(nextWordRunnable); removeOverlay(); cancelWordNotification();
            handler.postDelayed(nextWordRunnable, DISMISS_NEXT_WORD_MS); return START_STICKY;
        }
        if (AutoStudyContract.ACTION_GROUP_PROGRESS.equals(action)) {
            // A card/overlay may send progress twice (for example during Activity
            // recreation). Consume progress only while one delivery is outstanding.
            if (pendingWord == null) return START_STICKY;
            String selected = resolveSelectedBook();
            boolean bookChanged = selected != null && !selected.isEmpty()
                    && pendingWord.bookName != null && !selected.equals(pendingWord.bookName);
            if (bookChanged) {
                // Do not advance the old book's queue with an event from its stale card.
                AutoStudyGroupQueue.resetForBookSwitch(this, selected);
                bookName = selected;
            }
            pendingGroupNewWord = false;
            pendingWord = null;
            pendingSource = null;
            handler.removeCallbacks(fallbackRunnable);
            handler.removeCallbacks(nextWordRunnable);
            removeOverlay(); cancelWordNotification();
            handler.postDelayed(nextWordRunnable, 700L); return START_STICKY;
        }
        stopped = false;
        bookName = resolveSelectedBook();
        handler.removeCallbacks(nextWordRunnable);
        handler.post(this::deliverNext);
        return START_STICKY;
    }

    private void deliverNext() {
        if (stopped) return;
        try {
            // Re-read the selected book every delivery: settings may change while
            // the foreground service is alive, so do not keep using a stale bookName.
            String selectedBook = resolveSelectedBook();
            if (selectedBook != null && !selectedBook.isEmpty() && !selectedBook.equals(bookName)) {
                AutoStudyGroupQueue.resetForBookSwitch(this, selectedBook);
                bookName = selectedBook;
            }
            WordDeliveryItem item;
            WordRepository.Word due = WordRepository.getNextDueReviewAcrossBooks(this);
            if (due != null) {
                // 全局到期复习永远先于新词分组。
                item = WordRepository.buildDeliveryItem(this, due, "global_due_review_priority");
                pendingGroupNewWord = false;
            } else if ("全自动".equals(com.example.maimemo.storage.AppSettings.getString(this,
                    com.example.maimemo.storage.AppSettings.STUDY_MODE, "手动"))) {
                WordRepository.Word grouped = AutoStudyGroupQueue.current(this, bookName);
                item = grouped == null ? null : WordRepository.buildDeliveryItem(this, grouped, "auto_group_new_word");
                pendingGroupNewWord = grouped != null;
            } else {
                item = WordPushService.nextItem(this, bookName);
                pendingGroupNewWord = false;
            }
            if (item == null) {
                updateOngoing("当前没有到期复习词或可学习新词");
                handler.postDelayed(nextWordRunnable, 60_000L);
                return;
            }
            pendingWord = item.word;
            pendingSource = item.providerId + ":" + item.source.name();
            popupRetryAttempted = false;
            presentationConfirmed = false;
            overlayShown = false;
            cancelWordNotification();
            removeOverlay();
            Intent screen = new Intent(this, AutoStudyActivity.class);
            putWordExtras(screen, item.word, item.providerId + ":" + item.source.name());
            screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try { startActivity(screen); }
            catch (Throwable error) { Log.w(TAG, "Full-screen activity launch failed", error); }
            handler.removeCallbacks(fallbackRunnable);
            handler.postDelayed(fallbackRunnable, PRESENTATION_WATCHDOG_MS);
        } catch (Throwable error) {
            Log.e(TAG, "Cannot select next word", error);
            updateOngoing("读取单词失败，将稍后重试");
            handler.postDelayed(nextWordRunnable, 30_000L);
        }
    }

    private void tryOverlayThenNotify() {
        if (stopped || presentationConfirmed || pendingWord == null) return;

        // Two presenters with mutual exclusion: use the v15-style overlay when Android
        // grants overlay permission; otherwise fall back to the v19 Activity notification.
        // Both presenters read the same WordRepository data and report answers to this
        // service, which advances the single persistent AutoStudyGroupQueue.
        if (!isAppInForeground()) {
            boolean shown = AutoStudyOverlay.show(this, pendingWord,
                    pendingSource.isEmpty() ? "FALLBACK" : pendingSource);
            if (shown) {
                overlayShown = true;
                // Overlay posts ACTION_OVERLAY_PRESENTED; this guard avoids duplicate UI
                // during the short time before that acknowledgement reaches the service.
                presentationConfirmed = true;
                cancelWordNotification();
                updateOngoing("正在弹窗学习：" + pendingWord.headWord);
                return;
            }
        }
        overlayShown = false;
        showWordNotification(pendingWord);
    }

    private boolean isAppInForeground() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) return false;
        try {
            for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                if (getPackageName().equals(process.processName)) {
                    return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            || process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Unable to determine app foreground state", error);
        }
        return false;
    }

    private void showWordNotification(WordRepository.Word word) {
        Intent open = new Intent(this, AutoStudyActivity.class);
        putWordExtras(open, word, pendingSource.isEmpty() ? "FALLBACK" : pendingSource);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // PendingIntent identity must include the actual word, not only rank:
        // different books can have the same rank and Android otherwise may reuse
        // a stale notification intent. The data URI also forces a fresh identity.
        String notificationKey = String.valueOf(word.bookName) + "/" + word.rank + "/" + String.valueOf(word.headWord);
        open.setData(android.net.Uri.parse("maimemo://autostudy/word/" + android.net.Uri.encode(notificationKey)));
        int requestCode = notificationKey.hashCode();
        PendingIntent pi = PendingIntent.getActivity(this, requestCode, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Use Android's native heads-up message style. The banner appearance is
        // drawn by System UI (like a messaging app), not by a custom app window.
        Person sender = new Person.Builder().setName("必须背 · 单词提醒").build();
        NotificationCompat.MessagingStyle messageStyle = new NotificationCompat.MessagingStyle("必须背")
                .addMessage(word.headWord + (word.meaning == null || word.meaning.trim().isEmpty() ? "" : "\n" + word.meaning),
                        System.currentTimeMillis(), sender);
        Notification n = new NotificationCompat.Builder(this, AutoStudyContract.WORD_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle("必须背")
                .setContentText(word.headWord + (word.meaning == null || word.meaning.trim().isEmpty() ? "" : " · " + word.meaning))
                .setStyle(messageStyle)
                .setContentIntent(pi)
                // Ask Android to present the same AutoStudyActivity as a full-screen
                // popup when policy/permissions allow; otherwise show a heads-up banner
                // with the same tap target instead of falsely claiming an overlay appeared.
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true).setOnlyAlertOnce(false)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setTicker("必须背：" + word.headWord)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSilent(false)
                .setVibrate(new long[]{0, 250, 120, 250})
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean canPost = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (nm != null && canPost) {
            // Reuse one ID: update the current pending-word notification instead of
            // accumulating stale notifications or appearing to notify only once.
            nm.notify(AutoStudyContract.WORD_NOTIFICATION_ID, n);
            updateOngoing("等待处理：" + word.headWord);
        } else {
            // Android 13+ suppresses ordinary notifications without runtime permission.
            // Keep the pending word in the service and update the FGS status where allowed.
            Log.w(TAG, "POST_NOTIFICATIONS denied; fallback notification cannot be shown. Pending word=" + word.headWord);
            updateOngoing("通知权限未开启，待处理：" + word.headWord);
        }
    }

    static void putWordExtras(Intent i, WordRepository.Word w, String source) {
        i.putExtra(AutoStudyContract.EXTRA_BOOK, w.bookName);
        i.putExtra(AutoStudyContract.EXTRA_RANK, w.rank);
        i.putExtra(AutoStudyContract.EXTRA_WORD, w.headWord);
        i.putExtra(AutoStudyContract.EXTRA_MEANING, w.meaning);
        i.putExtra(AutoStudyContract.EXTRA_PHONETIC, w.phonetic);
        i.putExtra(AutoStudyContract.EXTRA_POS, w.pos);
        i.putExtra(AutoStudyContract.EXTRA_SENTENCE, w.sentence);
        i.putExtra(AutoStudyContract.EXTRA_SENTENCE_CN, w.sentenceCn);
        i.putExtra(AutoStudyContract.EXTRA_SOURCE, source);
    }

    private Notification buildOngoingNotification(String text) {
        Intent open = new Intent(this, AutoStudyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent content = PendingIntent.getActivity(this, 9001, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 9002, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, AutoStudyContract.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo).setContentTitle("必须背 · 自动学习")
                .setContentText(text).setContentIntent(content).addAction(0, "停止自动学习", stopPi)
                .setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).build();
    }
    private void updateOngoing(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(AutoStudyContract.NOTIFICATION_ID, buildOngoingNotification(text));
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;
            if (nm.getNotificationChannel(AutoStudyContract.CHANNEL_ID) == null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        AutoStudyContract.CHANNEL_ID, "自动学习服务状态", NotificationManager.IMPORTANCE_LOW);
                serviceChannel.setDescription("显示自动学习服务是否正在运行");
                nm.createNotificationChannel(serviceChannel);
            }
            // New dedicated high-importance channel enables heads-up word reminders
            // even if an older installation already created the previous channel.
            if (nm.getNotificationChannel(AutoStudyContract.WORD_CHANNEL_ID) == null) {
                NotificationChannel wordChannel = new NotificationChannel(
                        AutoStudyContract.WORD_CHANNEL_ID, "单词消息横幅", NotificationManager.IMPORTANCE_HIGH);
                wordChannel.setDescription("高优先级单词消息，尝试显示系统顶部横幅");
                wordChannel.enableVibration(true);
                wordChannel.setShowBadge(true);
                wordChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(wordChannel);
            }
        }
    }
    private void cancelWordNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(AutoStudyContract.WORD_NOTIFICATION_ID);
    }
    private void removeOverlay() { AutoStudyOverlay.dismiss(this); overlayShown = false; }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); removeOverlay(); cancelWordNotification(); super.onDestroy(); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
