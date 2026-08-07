package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderCategory(val displayNameHindi: String, val displayNameEnglish: String, val iconName: String) {
    STUDY("पढ़ाई", "Study", "School"),
    MEDICINE("दवा", "Medicine", "MedicalServices"),
    OFFICE("ऑफिस", "Office", "Work"),
    MEETING("मीटिंग", "Meeting", "Groups"),
    SHOPPING("खरीदारी", "Shopping", "ShoppingCart"),
    BIRTHDAY("जन्मदिन", "Birthday", "Cake"),
    EXERCISE("व्यायाम", "Exercise", "FitnessCenter"),
    WATER("पानी", "Water", "WaterDrop"),
    PRAYER("पूजा / प्रार्थना", "Prayer", "SelfImprovement"),
    PERSONAL("व्यक्तिगत", "Personal", "Person")
}

enum class RepeatType(val labelHindi: String, val labelEnglish: String) {
    ONCE("एक बार", "Once"),
    DAILY("हर रोज", "Daily"),
    WEEKLY("हर सप्ताह", "Weekly"),
    MONTHLY("हर महीने", "Monthly"),
    YEARLY("हर साल", "Yearly"),
    CUSTOM("कस्टम", "Custom")
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
    val category: String = ReminderCategory.PERSONAL.name,
    val repeatType: String = RepeatType.ONCE.name,
    val customRepeatDays: String = "", // e.g. "1,3,5" for Mon,Wed,Fri
    val isVoiceEnabled: Boolean = true,
    val customVoiceScript: String = "",
    val status: String = ReminderStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis()
)
