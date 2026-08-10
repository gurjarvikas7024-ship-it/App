package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ReminderDao
import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [ReminderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yaad_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate fallback mock data on initial creation
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    val dao = database.reminderDao()
                                    val now = System.currentTimeMillis()
                                    val calToday = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 20)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    val calTomorrow = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 8)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }

                                    val mock1 = ReminderEntity(
                                        title = "Drink Warm Water",
                                        description = "Stay hydrated and drink a glass of warm water",
                                        timeMillis = calToday.timeInMillis.coerceAtLeast(now + 3600000),
                                        repeatType = RepeatType.DAILY.name,
                                        customVoiceScript = "Hello! It's time to drink your evening glass of warm water."
                                    )
                                    val mock2 = ReminderEntity(
                                        title = "Morning Walk & Yoga",
                                        description = "30 minutes light exercise and stretching",
                                        timeMillis = calTomorrow.timeInMillis,
                                        repeatType = RepeatType.DAILY.name,
                                        customVoiceScript = "Good morning! Time for your morning walk and yoga session."
                                    )
                                    dao.insertReminder(mock1)
                                    dao.insertReminder(mock2)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
