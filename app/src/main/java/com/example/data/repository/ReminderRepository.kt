package com.example.data.repository

import com.example.data.dao.ReminderDao
import com.example.data.model.ReminderEntity
import com.example.data.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

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

    suspend fun updateReminder(reminder: ReminderEntity) =
        reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(id: Long) =
        reminderDao.deleteReminderById(id)

    suspend fun markCompleted(id: Long) =
        reminderDao.updateStatus(id, ReminderStatus.COMPLETED.name)

    suspend fun markMissed(id: Long) =
        reminderDao.updateStatus(id, ReminderStatus.MISSED.name)
}
