package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.service.AlarmService

open class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_SCRIPT = "extra_reminder_script"
        const val EXTRA_REMINDER_PRESET = "extra_reminder_preset"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
        val script = intent.getStringExtra(EXTRA_REMINDER_SCRIPT) ?: ""
        val preset = intent.getStringExtra(EXTRA_REMINDER_PRESET) ?: ""

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmService.EXTRA_REMINDER_ID, id)
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
            e.printStackTrace()
        }
    }
}
