package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {

    const val PREFS_NAME = "MemoryPlusPrefs"
    const val KEY_LIFETIME_REMINDERS_CREATED = "lifetime_reminders_created"
    const val KEY_REMINDERS_COUNT = "reminders_count"
    const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"
    const val SECRET_PRO_KEY = "MP2026PRO"
    const val MAX_FREE_REMINDERS = 2

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getLifetimeRemindersCreated(context: Context): Int {
        val prefs = getPrefs(context)
        val lifetime = prefs.getInt(KEY_LIFETIME_REMINDERS_CREATED, -1)
        if (lifetime != -1) {
            return lifetime
        }
        // Fallback/migration from previous key if present
        val legacy = prefs.getInt(KEY_REMINDERS_COUNT, 0)
        prefs.edit().putInt(KEY_LIFETIME_REMINDERS_CREATED, legacy).apply()
        return legacy
    }

    @JvmStatic
    fun getRemindersCount(context: Context): Int {
        return getLifetimeRemindersCreated(context)
    }

    @JvmStatic
    fun incrementLifetimeRemindersCreated(context: Context): Int {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_LIFETIME_REMINDERS_CREATED, prefs.getInt(KEY_REMINDERS_COUNT, 0))
        val next = current + 1
        prefs.edit()
            .putInt(KEY_LIFETIME_REMINDERS_CREATED, next)
            .putInt(KEY_REMINDERS_COUNT, next)
            .apply()
        return next
    }

    @JvmStatic
    fun incrementRemindersCount(context: Context): Int {
        return incrementLifetimeRemindersCreated(context)
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
    fun canCreateReminder(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isPro = prefs.getBoolean(KEY_IS_PRO_UNLOCKED, false)
        if (isPro) {
            return true
        }
        val totalCreated = prefs.getInt(KEY_LIFETIME_REMINDERS_CREATED, prefs.getInt(KEY_REMINDERS_COUNT, 0))
        return totalCreated < MAX_FREE_REMINDERS
    }

    @JvmStatic
    fun getRemainingFreeReminders(context: Context): Int {
        if (isProUnlocked(context)) return Int.MAX_VALUE
        val count = getLifetimeRemindersCreated(context)
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
