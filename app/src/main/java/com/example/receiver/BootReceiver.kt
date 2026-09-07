package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.service.AlarmScheduler
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
                    for (reminder in pendingReminders) {
                        if (reminder.timeMillis > now) {
                            scheduler.schedule(reminder)
                            restoredCount++
                        } else if (ReminderScheduleHelper.isRecurring(reminder.repeatType)) {
                            // Missed while device was powered off: advance to next future occurrence
                            val nextTrigger = ReminderScheduleHelper.getNextTriggerTime(
                                reminder.timeMillis,
                                reminder.repeatType,
                                now
                            )
                            val updated = reminder.copy(timeMillis = nextTrigger)
                            db.reminderDao().updateReminder(updated)
                            scheduler.schedule(updated)
                            restoredCount++
                        } else {
                            db.reminderDao().updateStatus(reminder.id, ReminderStatus.MISSED.name)
                        }
                    }
                    Log.d("BootReceiver", "Successfully rescheduled $restoredCount pending alarms.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
