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
     * Save reminder method: unlimited free access
     */
    suspend fun saveReminder(context: Context, reminder: ReminderEntity): Long {
        val id = reminderDao.insertReminder(reminder)
        PreferenceManager.incrementLifetimeActions(context)
        return id
    }

    /**
     * Update reminder method: unlimited free access
     */
    suspend fun updateReminderWithLimit(context: Context, reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
        PreferenceManager.incrementLifetimeActions(context)
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
