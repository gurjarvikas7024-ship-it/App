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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.repository.ReminderRepository
import com.example.ui.alarm.FullScreenAlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "memory_plus_alarm_channel_v2"
        const val NOTIFICATION_ID = 9991
        const val ACTION_START_ALARM = "com.example.service.ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.service.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.service.ACTION_SNOOZE_ALARM"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_SERVICE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_SCRIPT = "extra_reminder_script"
        const val EXTRA_REMINDER_PRESET = "extra_reminder_preset"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ttsManager: TTSManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentReminderId: Long = -1L
    private var currentReminderTitle: String = "Reminder Alert!"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MemoryPlus:AlarmServiceWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max safety timeout
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to acquire wake lock in onCreate", e)
        }

        try {
            ttsManager = TTSManager(applicationContext)
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to create TTSManager", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val id = intent?.getLongExtra(EXTRA_REMINDER_ID, currentReminderId) ?: currentReminderId
        val title = intent?.getStringExtra(EXTRA_REMINDER_TITLE) ?: currentReminderTitle
        val script = intent?.getStringExtra(EXTRA_REMINDER_SCRIPT) ?: ""
        val preset = intent?.getStringExtra(EXTRA_REMINDER_PRESET) ?: "Studio Female"
        val snoozeMinutes = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, 5) ?: 5

        currentReminderId = id
        currentReminderTitle = title

        when (action) {
            ACTION_DISMISS_ALARM -> {
                handleDismiss(id)
            }
            ACTION_SNOOZE_ALARM -> {
                handleSnooze(id, snoozeMinutes)
            }
            ACTION_STOP_SERVICE -> {
                stopAlarmMediaAndVibration()
                stopSelf()
            }
            ACTION_START_ALARM -> {
                startAlarmSequence(id, title, script, preset)
            }
            else -> {
                startAlarmSequence(id, title, script, preset)
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmSequence(id: Long, title: String, script: String, preset: String) {
        // 1. Create High-Priority Notification Channel & call startForeground immediately
        createNotificationChannel()
        promoteToForegroundImmediately(title, script, id)

        // 2. Start looping alarm sound and continuous vibration
        startAudioAndVibration()

        // 3. Play voice announcement using TTS
        serviceScope.launch {
            delay(1500) // slight delay to allow alarm ring to establish
            val voiceText = if (script.isNotBlank()) script else "Reminder: $title"
            ttsManager?.speak(voiceText, preset)
        }

        // 4. Try opening FullScreenAlarmActivity
        triggerFullScreenActivity(id, title, script)
    }

    private fun triggerFullScreenActivity(id: Long, title: String, script: String) {
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
            Log.e("AlarmService", "FullScreenActivity direct start skipped or restricted: ${e.message}")
        }
    }

    private fun startAudioAndVibration() {
        try {
            stopAlarmMediaAndVibration()

            // Initialize MediaPlayer with ALARM usage
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Start continuous vibration pattern
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error starting audio/vibration in service", e)
        }
    }

    private fun stopAlarmMediaAndVibration() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping MediaPlayer", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping Vibrator", e)
        }

        try {
            ttsManager?.stop()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping TTS", e)
        }
    }

    private fun handleDismiss(reminderId: Long) {
        stopAlarmMediaAndVibration()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        if (reminderId != -1L) notificationManager?.cancel(reminderId.toInt())

        serviceScope.launch {
            try {
                if (reminderId != -1L) {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = ReminderRepository(db.reminderDao())
                    repo.markCompleted(reminderId)
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error marking reminder completed", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun handleSnooze(reminderId: Long, snoozeMinutes: Int) {
        stopAlarmMediaAndVibration()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        if (reminderId != -1L) notificationManager?.cancel(reminderId.toInt())

        serviceScope.launch {
            try {
                if (reminderId != -1L) {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = ReminderRepository(db.reminderDao())
                    val scheduler = AlarmScheduler(applicationContext)
                    val reminder = repo.getReminderById(reminderId)
                    if (reminder != null) {
                        val snoozedTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                        val updated = reminder.copy(timeMillis = snoozedTime)
                        repo.updateReminder(updated)
                        scheduler.schedule(updated)
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error snoozing reminder", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun promoteToForegroundImmediately(title: String, script: String, id: Long) {
        try {
            // PendingIntent to launch FullScreenAlarmActivity on tap
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

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                if (id != -1L) id.toInt() else NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct Dismiss Action PendingIntent
            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(EXTRA_REMINDER_ID, id)
            }
            val dismissPendingIntent = PendingIntent.getService(
                this,
                (id + 200000).toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct Snooze Action PendingIntent (5 mins)
            val snoozeIntent = Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_SNOOZE_MINUTES, 5)
            }
            val snoozePendingIntent = PendingIntent.getService(
                this,
                (id + 300000).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle("🔔 MEMORY PLUS REMINDER")
                .bigText("$title\n\n${if (script.isNotBlank()) script else "Tap to open full screen or use quick actions below."}")

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Memory Plus: $title")
                .setContentText(title)
                .setStyle(bigTextStyle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSound(alarmSoundUri, AudioManager.STREAM_ALARM)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_sync, "Snooze 5m", snoozePendingIntent)
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
                val alarmSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Memory Plus Priority Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Triggers full screen alarm overlays, loud sounds, and heads-up banner notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    enableLights(true)
                    setSound(alarmSoundUri, audioAttributes)
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
        stopAlarmMediaAndVibration()
        ttsManager?.shutdown()
        ttsManager = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing WakeLock", e)
        }
        serviceScope.cancel()
    }
}

