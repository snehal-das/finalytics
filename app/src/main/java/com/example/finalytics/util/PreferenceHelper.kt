package com.example.finalytics.util

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {
    private const val PREFS_NAME = "finalytics_prefs"
    private const val KEY_NEW_TX_NOTIF = "new_tx_notifications"
    private const val KEY_AUDIT_NOTIF = "audit_notifications"
    private const val KEY_AUDIT_TIME = "audit_notification_time" // e.g. "21:00"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isNewTxNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NEW_TX_NOTIF, true)
    }

    fun setNewTxNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NEW_TX_NOTIF, enabled).apply()
    }

    fun isAuditNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUDIT_NOTIF, false) // Default false as per standard UX
    }

    fun setAuditNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUDIT_NOTIF, enabled).apply()
    }

    fun getAuditNotificationTime(context: Context): String {
        return getPrefs(context).getString(KEY_AUDIT_TIME, "21:00") ?: "21:00"
    }

    fun setAuditNotificationTime(context: Context, time: String) {
        getPrefs(context).edit().putString(KEY_AUDIT_TIME, time).apply()
    }
}
