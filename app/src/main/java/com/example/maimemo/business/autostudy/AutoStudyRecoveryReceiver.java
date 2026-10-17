package com.example.maimemo.business.autostudy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.maimemo.storage.AppSettings;

/** Best-effort recovery after reboot or app update; Android does not allow recovery after force-stop. */
public final class AutoStudyRecoveryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;
        String mode = AppSettings.getString(context, AppSettings.STUDY_MODE, "");
        if (!"全自动".equals(mode)) return;
        try {
            Intent start = new Intent(context, AutoStudyForegroundService.class).setAction(AutoStudyContract.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(start);
            else context.startService(start);
        } catch (Throwable error) {
            Log.w("AutoStudyRecovery", "Unable to restore auto-study session; open the app to resume.", error);
        }
    }
}
