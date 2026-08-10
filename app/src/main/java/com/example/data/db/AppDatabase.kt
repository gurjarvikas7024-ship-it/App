package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ReminderDao
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderEntity
import com.example.data.model.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [ReminderEntity::class],
    version = 1,
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
                                        title = "रात को गरम पानी पीना है",
                                        description = "सेहत के लिए गरम पानी पीना याद रखें",
                                        timeMillis = calToday.timeInMillis.coerceAtLeast(now + 3600000),
                                        category = ReminderCategory.WATER.name,
                                        repeatType = RepeatType.DAILY.name,
                                        customVoiceScript = "जी, रात का गरम पानी पीने का समय हो गया है।"
                                    )
                                    val mock2 = ReminderEntity(
                                        title = "सुबह की सैर और एक्सरसाइज",
                                        description = "30 मिनट वॉक और योग",
                                        timeMillis = calTomorrow.timeInMillis,
                                        category = ReminderCategory.EXERCISE.name,
                                        repeatType = RepeatType.DAILY.name,
                                        customVoiceScript = "सुप्रभात! सुबह की वॉक और योग का समय हो गया है।"
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
