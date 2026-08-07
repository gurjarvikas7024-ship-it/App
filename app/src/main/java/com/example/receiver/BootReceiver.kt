package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val scheduler = AlarmScheduler(context)
                    val pendingReminders = db.reminderDao().getRemindersByStatus(ReminderStatus.PENDING.name).first()
                    val now = System.currentTimeMillis()

                    for (reminder in pendingReminders) {
                        if (reminder.timeMillis > now) {
                            scheduler.schedule(reminder)
                        } else {
                            db.reminderDao().updateStatus(reminder.id, ReminderStatus.MISSED.name)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
