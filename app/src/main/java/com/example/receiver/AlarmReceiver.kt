package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        var wakeLock: PowerManager.WakeLock? = null
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryPlus:AlarmReceiverWakeLock"
            )
            wakeLock?.acquire(30 * 1000L) // 30s guarantee for service handoff

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
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                // Ignore wakeLock release exception
            }
        }
    }
}

