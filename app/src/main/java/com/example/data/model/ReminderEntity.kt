package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatType(val labelEnglish: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

enum class ReminderStatus {
    PENDING,
    COMPLETED,
    MISSED
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeMillis: Long,
    val repeatType: String = RepeatType.ONCE.name,
    val customRepeatDays: String = "", // e.g. "1,3,5" for Mon,Wed,Fri
    val isVoiceEnabled: Boolean = true,
    val customVoiceScript: String = "",
    val voicePreset: String = "Studio Female",
    val status: String = ReminderStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
)
