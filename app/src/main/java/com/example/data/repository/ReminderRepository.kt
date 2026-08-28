package com.example.data.repository

import android.content.Context
import android.widget.Toast
import com.example.data.dao.ReminderDao
import com.example.data.model.ReminderEntity
import com.example.data.model.ReminderStatus
import com.example.data.preferences.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReminderRepository(private val reminderDao: ReminderDao) {

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    fun getPendingReminders(): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersByStatus(ReminderStatus.PENDING.name)

    fun getCompletedReminders(): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersByStatus(ReminderStatus.COMPLETED.name)

    fun getMissedReminders(): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersByStatus(ReminderStatus.MISSED.name)

    fun searchReminders(query: String): Flow<List<ReminderEntity>> =
        reminderDao.searchReminders(query)

    fun getRemindersForDateRange(startMillis: Long, endMillis: Long): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersBetween(startMillis, endMillis)

    val activeCount: Flow<Int> = reminderDao.getActiveCountFlow()

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun insertReminder(reminder: ReminderEntity): Long =
        reminderDao.insertReminder(reminder)

    /**
     * Strict verification save method: blocks saving if free action limit reached
     */
    suspend fun saveReminder(context: Context, reminder: ReminderEntity): Long {
        val prefs = context.getSharedPreferences(PreferenceManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isPro = prefs.getBoolean(PreferenceManager.KEY_IS_PRO_UNLOCKED, false)
        val actionCount = PreferenceManager.getLifetimeActionsCount(context)

        if (!isPro && actionCount >= PreferenceManager.MAX_FREE_ACTIONS) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Free trial limit reached (2/2). Upgrade to Pro to edit or create unlimited reminders.", Toast.LENGTH_SHORT).show()
            }
            throw IllegalStateException("Free trial limit reached! Upgrade to Pro.")
        }

        val id = reminderDao.insertReminder(reminder)
        if (!isPro) {
            PreferenceManager.incrementLifetimeActions(context)
        }
        return id
    }

    /**
     * Strict verification update method: blocks updating if free action limit reached
     */
    suspend fun updateReminderWithLimit(context: Context, reminder: ReminderEntity) {
        val prefs = context.getSharedPreferences(PreferenceManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isPro = prefs.getBoolean(PreferenceManager.KEY_IS_PRO_UNLOCKED, false)
        val actionCount = PreferenceManager.getLifetimeActionsCount(context)

        if (!isPro && actionCount >= PreferenceManager.MAX_FREE_ACTIONS) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Free trial limit reached (2/2). Upgrade to Pro to edit or create unlimited reminders.", Toast.LENGTH_SHORT).show()
            }
            throw IllegalStateException("Free trial limit reached! Upgrade to Pro.")
        }

        reminderDao.updateReminder(reminder)
        if (!isPro) {
            PreferenceManager.incrementLifetimeActions(context)
        }
    }

    suspend fun updateReminder(reminder: ReminderEntity) =
        reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(id: Long) =
        reminderDao.deleteReminderById(id)

    suspend fun markCompleted(id: Long) =
        reminderDao.updateStatus(id, ReminderStatus.COMPLETED.name)

    suspend fun markMissed(id: Long) =
        reminderDao.updateStatus(id, ReminderStatus.MISSED.name)
}
