package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Device restarted / package replaced with action: $action. Rescheduling all pending alarms.")

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

                    var rescheduledCount = 0
                    for (reminder in pendingReminders) {
                        if (reminder.timeMillis > now) {
                            scheduler.schedule(reminder)
                            rescheduledCount++
                        } else {
                            db.reminderDao().updateStatus(reminder.id, ReminderStatus.MISSED.name)
                        }
                    }
                    Log.d("BootReceiver", "Successfully restored $rescheduledCount pending alarms after boot.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error during boot alarm rescheduling", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
