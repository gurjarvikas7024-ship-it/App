package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {

    const val PREFS_NAME = "MemoryPlusPrefs"
    const val KEY_REMINDER_COUNT = "reminder_count"
    const val KEY_LIFETIME_ACTIONS_COUNT = "lifetime_actions_count"
    const val KEY_LIFETIME_REMINDERS_CREATED = "lifetime_reminders_created"
    const val KEY_REMINDERS_COUNT = "reminders_count"
    const val KEY_IS_PRO_UNLOCKED = "is_pro_unlocked"
    const val SECRET_PRO_KEY = "MP2026PRO"
    const val MAX_FREE_REMINDERS = 2
    const val MAX_FREE_ACTIONS = 2

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getLifetimeActionsCount(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_REMINDER_COUNT, 0)
    }

    @JvmStatic
    fun incrementLifetimeActions(context: Context): Int {
        val prefs = getPrefs(context)
        val current = getLifetimeActionsCount(context)
        val next = current + 1
        prefs.edit()
            .putInt(KEY_REMINDER_COUNT, next)
            .putInt(KEY_LIFETIME_ACTIONS_COUNT, next)
            .putInt(KEY_LIFETIME_REMINDERS_CREATED, next)
            .putInt(KEY_REMINDERS_COUNT, next)
            .apply()
        return next
    }

    @JvmStatic
    fun getLifetimeRemindersCreated(context: Context): Int {
        return getLifetimeActionsCount(context)
    }

    @JvmStatic
    fun getRemindersCount(context: Context): Int {
        return getLifetimeActionsCount(context)
    }

    @JvmStatic
    fun incrementLifetimeRemindersCreated(context: Context): Int {
        return incrementLifetimeActions(context)
    }

    @JvmStatic
    fun incrementRemindersCount(context: Context): Int {
        return incrementLifetimeActions(context)
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
    fun canPerformAction(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isPro = prefs.getBoolean(KEY_IS_PRO_UNLOCKED, false)
        if (isPro) {
            return true
        }
        val actionCount = getLifetimeActionsCount(context)
        return actionCount < MAX_FREE_ACTIONS
    }

    @JvmStatic
    fun canCreateReminder(context: Context): Boolean {
        return canPerformAction(context)
    }

    @JvmStatic
    fun getRemainingFreeReminders(context: Context): Int {
        if (isProUnlocked(context)) return Int.MAX_VALUE
        val count = getLifetimeActionsCount(context)
        return (MAX_FREE_ACTIONS - count).coerceAtLeast(0)
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
