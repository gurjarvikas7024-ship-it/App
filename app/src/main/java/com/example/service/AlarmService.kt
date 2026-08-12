package com.example.service

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ui.alarm.FullScreenAlarmActivity

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "memory_plus_alarm_channel_v1"
        const val NOTIFICATION_ID = 9991
        const val ACTION_START_ALARM = "com.example.service.ACTION_START_ALARM"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_SCRIPT = "extra_reminder_script"
        const val EXTRA_REMINDER_PRESET = "extra_reminder_preset"
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryPlus:AlarmServiceWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes safety timeout
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to acquire wake lock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acquire temporary Partial WakeLock for 60 seconds to prevent CPU sleep
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryPlus:AlarmServiceWakeLock"
            )
            wakeLock?.acquire(60 * 1000L) // 60s partial wake lock
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to acquire wake lock", e)
        }

        val id = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
        val script = intent?.getStringExtra(EXTRA_REMINDER_SCRIPT) ?: ""

        // 1. Create channel & execute startForeground IMMEDIATELY
        createNotificationChannel()
        promoteToForegroundImmediately(title, script, id)

        // 2. Launch full screen overlay activity
        try {
            val fullScreenIntent = Intent(this, FullScreenAlarmActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_SCRIPT, script)
            }

            val activityOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    }.toBundle()
                } catch (e: Exception) {
                    null
                }
            } else null

            if (activityOptions != null) {
                startActivity(fullScreenIntent, activityOptions)
            } else {
                startActivity(fullScreenIntent)
            }
        } catch (e: Throwable) {
            Log.e("AlarmService", "Error triggering full screen activity from AlarmService", e)
        }

        return START_NOT_STICKY
    }

    private fun promoteToForegroundImmediately(title: String, script: String, id: Long) {
        try {
            val fullScreenIntent = Intent(this, FullScreenAlarmActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_SCRIPT, script)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                if (id != -1L) id.toInt() else NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Massive BigTextStyle Notification with large text display
            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle("🔔 MEMORY PLUS REMINDER")
                .bigText("$title\n\n${script.ifEmpty { "Important scheduled task. Tap to open or snooze." }}")

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Memory Plus: $title")
                .setContentText(title)
                .setStyle(bigTextStyle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } catch (e: Throwable) {
                        Log.e("AlarmService", "startForeground specialUse failed", e)
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("AlarmService", "Fatal error in promoteToForegroundImmediately", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Memory Plus Priority Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Triggers full screen alarm overlays and loud vibrating notifications for Memory Plus"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    enableLights(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e("AlarmService", "Error creating NotificationChannel", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing WakeLock", e)
        }
    }
}
