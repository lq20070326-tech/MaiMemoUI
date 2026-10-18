package com.example.maimemo.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.maimemo.storage.AppSettings;
import com.example.maimemo.ui.viewmodel.ReminderReceiver;

import java.util.Calendar;

/** 本地每日提醒调度，不需要网络服务。 */
public final class ReminderScheduler {
    private ReminderScheduler() { }

    public static void schedule(Context context) {
        cancel(context);
        if (!AppSettings.getBoolean(context, AppSettings.REMINDER_ENABLED, true)) return;
        String value = AppSettings.getString(context, AppSettings.REMINDER_TIME, "20:00");
        int hour = 20, minute = 0;
        try {
            String[] parts = value.split(":");
            hour = Integer.parseInt(parts[0]); minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) { }
        Calendar when = Calendar.getInstance();
        when.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, hour)));
        when.set(Calendar.MINUTE, Math.max(0, Math.min(59, minute)));
        when.set(Calendar.SECOND, 0); when.set(Calendar.MILLISECOND, 0);
        if (!when.after(Calendar.getInstance())) when.add(Calendar.DAY_OF_YEAR, 1);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pendingIntent(context);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
            else manager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
        }
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pendingIntent(context);
        if (manager != null) manager.cancel(pending);
        pending.cancel();
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 9137, intent, flags);
    }
}
