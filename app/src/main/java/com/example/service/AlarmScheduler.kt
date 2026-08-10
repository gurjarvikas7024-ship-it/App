package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ReminderEntity
import com.example.receiver.ReminderReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (alarmManager == null) return
        if (reminder.timeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT, reminder.customVoiceScript)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(reminder.timeMillis, pendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(reminder.timeMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.timeMillis, pendingIntent)
            }
            Log.d("AlarmScheduler", "Alarm scheduled for ID ${reminder.id} at ${reminder.timeMillis}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to schedule exact alarm, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeMillis, pendingIntent)
            } catch (ex: Exception) {
                Log.e("AlarmScheduler", "Fallback alarm scheduling also failed", ex)
            }
        }
    }

    fun cancel(reminderId: Long) {
        if (alarmManager == null) return
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to cancel alarm", e)
        }
    }
}
