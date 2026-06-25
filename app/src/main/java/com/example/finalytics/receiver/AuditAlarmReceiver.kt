package com.example.finalytics.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.finalytics.util.NotificationHelper
import com.example.finalytics.util.PreferenceHelper

class AuditAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AuditAlarmReceiver", "Received intent action: ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (PreferenceHelper.isAuditNotificationsEnabled(context)) {
                NotificationHelper.scheduleAuditAlarm(context)
            }
        } else {
            // Alarm trigger
            if (PreferenceHelper.isAuditNotificationsEnabled(context)) {
                NotificationHelper.sendAuditNotification(context)
                // Schedule for the next day
                NotificationHelper.scheduleAuditAlarm(context)
            }
        }
    }
}
