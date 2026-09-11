package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.data.model.RepeatType
import com.example.service.AlarmScheduler
import com.example.service.NotificationHelper
import com.example.service.ReminderScheduleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Device rebooted or package replaced: $action. Restoring pending alarms.")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val scheduler = AlarmScheduler(context)
                    val pendingReminders = db.reminderDao().getRemindersByStatus(ReminderStatus.PENDING.name).first()
                    val now = System.currentTimeMillis()

                    var restoredCount = 0
                    var missedCount = 0
                    for (reminder in pendingReminders) {
                        if (reminder.timeMillis > now) {
                            scheduler.schedule(reminder)
                            restoredCount++
                        } else if (ReminderScheduleHelper.isRecurring(reminder.repeatType)) {
                            // Missed while mobile was switched off:
                            // 1. Record missed occurrence so it appears in Missed Reminders
                            val missedRecord = reminder.copy(
                                id = 0,
                                repeatType = RepeatType.ONCE.name,
                                status = ReminderStatus.MISSED.name
                            )
                            db.reminderDao().insertReminder(missedRecord)

                            // 2. Advance recurring reminder to next future occurrence
                            val nextTrigger = ReminderScheduleHelper.getNextTriggerTime(
                                reminder.timeMillis,
                                reminder.repeatType,
                                now
                            )
                            val updated = reminder.copy(
                                timeMillis = nextTrigger,
                                status = ReminderStatus.PENDING.name
                            )
                            db.reminderDao().updateReminder(updated)
                            scheduler.schedule(updated)
                            restoredCount++
                            missedCount++

                            // 3. Notify user of missed reminder
                            NotificationHelper.showMissedReminderNotification(
                                context,
                                reminder.id,
                                reminder.title,
                                reminder.timeMillis
                            )
                        } else {
                            // One-time reminder missed while phone was off: mark as MISSED
                            db.reminderDao().updateStatus(reminder.id, ReminderStatus.MISSED.name)
                            missedCount++

                            // Notify user of missed reminder
                            NotificationHelper.showMissedReminderNotification(
                                context,
                                reminder.id,
                                reminder.title,
                                reminder.timeMillis
                            )
                        }
                    }
                    Log.d("BootReceiver", "Boot sync finished: $restoredCount pending alarms rescheduled, $missedCount marked as missed.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
