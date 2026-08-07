package com.example.data.dao

import androidx.room.*
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY timeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND timeMillis >= :startTimeMillis AND timeMillis <= :endTimeMillis ORDER BY timeMillis ASC")
    fun getRemindersBetween(startTimeMillis: Long, endTimeMillis: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = :status ORDER BY timeMillis ASC")
    fun getRemindersByStatus(status: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY timeMillis DESC")
    fun searchReminders(query: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'PENDING'")
    fun getActiveCountFlow(): Flow<Int>
}
