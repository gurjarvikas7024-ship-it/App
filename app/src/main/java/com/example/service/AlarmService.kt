package com.example.service

import android.app.ActivityOptions
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
import com.example.ui.alarm.AlarmActivity
import com.example.ui.alarm.FullScreenAlarmActivity

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "yaad_ai_alarm_channel_v4"
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
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "YaadAI:AlarmServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 mins timeout safety
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
        val script = intent?.getStringExtra(EXTRA_REMINDER_SCRIPT) ?: ""
        val preset = intent?.getStringExtra(EXTRA_REMINDER_PRESET) ?: ""

        createNotificationChannel()

        val fullScreenIntent = Intent(this, FullScreenAlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_SCRIPT, script)
            putExtra(EXTRA_REMINDER_PRESET, preset)
        }

        val activityOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        val pendingIntent = PendingIntent.getActivity(
            this,
            id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptions
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Yaad AI: $title")
            .setContentText(script.ifEmpty { "Important scheduled reminder" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Failed startForeground with type, fallback standard", e)
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Immediately launch full screen overlay over active apps (YouTube, Instagram, Facebook, etc.)
        try {
            if (activityOptions != null) {
                startActivity(fullScreenIntent, activityOptions)
            } else {
                startActivity(fullScreenIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to launch full screen intent from Service", e)
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yaad AI Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers full screen alarm overlays over all applications"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
