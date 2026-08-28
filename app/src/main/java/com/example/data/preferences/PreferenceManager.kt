package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {

    private const val PREFS_NAME = "memory_plus_prefs"
    const val KEY_REMINDERS_CREATED_COUNT = "reminders_created_count"
    const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"
    const val SECRET_PRO_KEY = "MP2026PRO"
    const val MAX_FREE_REMINDERS = 2

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getRemindersCreatedCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDERS_CREATED_COUNT, 0)
    }

    @JvmStatic
    fun incrementRemindersCreatedCount(context: Context): Int {
        val prefs = getPrefs(context)
        val newCount = prefs.getInt(KEY_REMINDERS_CREATED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_REMINDERS_CREATED_COUNT, newCount).apply()
        return newCount
    }

    @JvmStatic
    fun isProUnlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PRO_UNLOCKED, false)
    }

    @JvmStatic
    fun setProUnlocked(context: Context, isUnlocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PRO_UNLOCKED, isUnlocked).apply()
    }

    @JvmStatic
    fun isLocked(context: Context): Boolean {
        if (isProUnlocked(context)) {
            return false
        }
        return getRemindersCreatedCount(context) >= MAX_FREE_REMINDERS
    }

    @JvmStatic
    fun getRemainingFreeReminders(context: Context): Int {
        if (isProUnlocked(context)) return Int.MAX_VALUE
        val count = getRemindersCreatedCount(context)
        return (MAX_FREE_REMINDERS - count).coerceAtLeast(0)
    }

    @JvmStatic
    fun verifyAndUnlockSecretKey(context: Context, key: String): Boolean {
        if (key.trim().equals(SECRET_PRO_KEY, ignoreCase = true)) {
            setProUnlocked(context, true)
            return true
        }
        return false
    }
}
