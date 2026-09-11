package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.data.model.RepeatType
import com.example.service.AlarmScheduler
import com.example.service.AlarmService
import com.example.service.NotificationHelper
import com.example.service.ReminderScheduleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_10 = "com.example.ACTION_SNOOZE_10"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_SCRIPT = "extra_reminder_script"
        const val EXTRA_REMINDER_PRESET = "extra_reminder_preset"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: ACTION_TRIGGER_REMINDER
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
        val script = intent.getStringExtra(EXTRA_REMINDER_SCRIPT) ?: ""
        val preset = intent.getStringExtra(EXTRA_REMINDER_PRESET) ?: ""

        when (action) {
            ACTION_MARK_DONE -> {
                Log.d("AlarmReceiver", "Mark as Done for ID: $reminderId")
                NotificationHelper.cancelNotification(context, reminderId)

                // Stop AlarmService if running
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_DISMISS_ALARM
                    putExtra(AlarmService.EXTRA_REMINDER_ID, reminderId)
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error stopping service on mark done", e)
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (reminderId != -1L) {
                            val db = AppDatabase.getInstance(context)
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                if (ReminderScheduleHelper.isRecurring(reminder.repeatType)) {
                                    // 1. Record completed occurrence in history
                                    val completedRecord = reminder.copy(
                                        id = 0,
                                        repeatType = RepeatType.ONCE.name,
                                        status = ReminderStatus.COMPLETED.name
                                    )
                                    db.reminderDao().insertReminder(completedRecord)

                                    // 2. Schedule next cycle
                                    val nextTrigger = ReminderScheduleHelper.getNextTriggerTime(
                                        reminder.timeMillis,
                                        reminder.repeatType,
                                        System.currentTimeMillis()
                                    )
                                    val updated = reminder.copy(
                                        timeMillis = nextTrigger,
                                        status = ReminderStatus.PENDING.name
                                    )
                                    db.reminderDao().updateReminder(updated)
                                    AlarmScheduler(context).schedule(updated)
                                    Log.d("AlarmReceiver", "Recurring reminder $reminderId (${reminder.repeatType}) marked done and scheduled for: $nextTrigger")
                                } else {
                                    db.reminderDao().updateStatus(reminderId, ReminderStatus.COMPLETED.name)
                                    AlarmScheduler(context).cancel(reminderId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Error marking reminder completed", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                Toast.makeText(context, "Reminder marked as done", Toast.LENGTH_SHORT).show()
            }

            ACTION_SNOOZE_10 -> {
                Log.d("AlarmReceiver", "Snooze 10 Mins for ID: $reminderId")
                NotificationHelper.cancelNotification(context, reminderId)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_SNOOZE_ALARM
                    putExtra(AlarmService.EXTRA_REMINDER_ID, reminderId)
                    putExtra(AlarmService.EXTRA_SNOOZE_MINUTES, 10)
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error sending snooze to service", e)
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (reminderId != -1L) {
                            val db = AppDatabase.getInstance(context)
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                val snoozedCal = Calendar.getInstance().apply {
                                    timeInMillis = System.currentTimeMillis() + (10 * 60 * 1000L)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val updated = reminder.copy(
                                    timeMillis = snoozedCal.timeInMillis,
                                    status = ReminderStatus.PENDING.name
                                )
                                db.reminderDao().updateReminder(updated)
                                AlarmScheduler(context).schedule(updated)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Error snoozing reminder", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                Toast.makeText(context, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
            }

            else -> {
                // ACTION_TRIGGER_REMINDER
                Log.d("AlarmReceiver", "Triggering alarm: $reminderId - $title")
                NotificationHelper.showAlarmNotification(context, reminderId, title, script)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_START_ALARM
                    putExtra(AlarmService.EXTRA_REMINDER_ID, reminderId)
                    putExtra(AlarmService.EXTRA_REMINDER_TITLE, title)
                    putExtra(AlarmService.EXTRA_REMINDER_SCRIPT, script)
                    putExtra(AlarmService.EXTRA_REMINDER_PRESET, preset)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error starting AlarmService", e)
                }
            }
        }
    }
}
