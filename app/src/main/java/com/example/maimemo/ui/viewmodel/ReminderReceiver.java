package com.example.maimemo.ui.viewmodel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.maimemo.R;
import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.util.ReminderScheduler;

/** 接收本地闹钟并显示学习提醒；显示后安排下一天。 */
public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "daily_study_reminder";

    @Override public void onReceive(Context context, Intent intent) {
        if (!AppSettings.getBoolean(context, AppSettings.REMINDER_ENABLED, true)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "每日学习提醒", NotificationManager.IMPORTANCE_DEFAULT));
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle("该复习单词啦")
                .setContentText("花几分钟复习今天的单词，保持记忆连续性。")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try { NotificationManagerCompat.from(context).notify(9137, notification.build()); }
        catch (SecurityException ignored) { /* Android 13+ 未授权通知时安全跳过 */ }
        ReminderScheduler.schedule(context);
    }
}
