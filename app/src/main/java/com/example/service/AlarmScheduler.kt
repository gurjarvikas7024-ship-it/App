package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ReminderEntity
import com.example.receiver.AlarmReceiver
import com.example.receiver.ReminderReceiver
import com.example.ui.alarm.FullScreenAlarmActivity

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (alarmManager == null) return
        if (reminder.timeMillis <= System.currentTimeMillis()) return

        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TRIGGER_REMINDER
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
                putExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT, reminder.customVoiceScript)
                putExtra(ReminderReceiver.EXTRA_REMINDER_PRESET, reminder.voicePreset)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
                putExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT, reminder.customVoiceScript)
            }

            val showPendingIntent = PendingIntent.getActivity(
                context,
                (reminder.id + 100000).toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Exact AlarmClockInfo ensures the Android OS hardware timer wakes up the CPU instantly
            // even in deep Doze mode, on table idle, or battery saver
            val alarmClockInfo = AlarmManager.AlarmClockInfo(reminder.timeMillis, showPendingIntent)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Log.d("AlarmScheduler", "Exact Hardware setAlarmClock scheduled for ID ${reminder.id} at ${reminder.timeMillis}")
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeMillis, pendingIntent)
                    Log.d("AlarmScheduler", "Exact setExactAndAllowWhileIdle scheduled for ID ${reminder.id} at ${reminder.timeMillis}")
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("AlarmScheduler", "Pre-Android S setAlarmClock scheduled for ID ${reminder.id} at ${reminder.timeMillis}")
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed primary scheduling, trying fallback RTC_WAKEUP", e)
            try {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_TRIGGER_REMINDER
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT, reminder.customVoiceScript)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_PRESET, reminder.voicePreset)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeMillis, pendingIntent)
            } catch (ex: Exception) {
                Log.e("AlarmScheduler", "Fallback scheduling failed completely", ex)
            }
        }
    }

    fun cancel(reminderId: Long) {
        if (alarmManager == null) return
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
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

