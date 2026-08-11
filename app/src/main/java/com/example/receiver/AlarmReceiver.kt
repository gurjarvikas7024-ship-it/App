package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        try {
            val id = intent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L)
            val title = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
            val script = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT) ?: ""
            val preset = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_PRESET) ?: ""

            Log.d("AlarmReceiver", "AlarmReceiver triggered for ID: $id title: $title")

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                putExtra(AlarmService.EXTRA_REMINDER_ID, id)
                putExtra(AlarmService.EXTRA_REMINDER_TITLE, title)
                putExtra(AlarmService.EXTRA_REMINDER_SCRIPT, script)
                putExtra(AlarmService.EXTRA_REMINDER_PRESET, preset)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Throwable) {
            Log.e("AlarmReceiver", "Error during AlarmReceiver execution", e)
        }
    }
}
