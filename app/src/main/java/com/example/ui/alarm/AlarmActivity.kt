package com.example.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.receiver.ReminderReceiver
import com.example.service.AlarmScheduler
import com.example.service.TTSManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OceanBlueAccent
import com.example.ui.theme.SkyBlueContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var ttsManager: TTSManager? = null
    private var alarmScope: CoroutineScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enforceScreenOvertake()

        val reminderId = intent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L)
        val reminderTitle = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TITLE) ?: "Reminder Alert!"
        val customScript = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT) ?: ""
        val reminderPreset = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_PRESET) ?: ""

        // Sound & Vibration
        startAlarmEffects()

        // TTS Speech
        ttsManager = TTSManager(this)

        alarmScope = CoroutineScope(Dispatchers.IO)
        alarmScope?.launch {
            val userPrefs = UserPreferencesRepository(applicationContext)
            val name = userPrefs.userNameFlow.first()
            val globalPreset = userPrefs.voicePresetFlow.first()
            val activePreset = if (reminderPreset.isNotBlank()) reminderPreset else globalPreset

            val spokenText = if (customScript.isNotBlank()) customScript
            else reminderTitle

            // Short delay so initial ringtone alerts user, then TTS speaks AI script clearly
            kotlinx.coroutines.delay(800)

            while (isActive) {
                // Duck/pause ringtone while TTS speaks so voice is crystal clear
                try {
                    ringtone?.stop()
                } catch (e: Exception) { e.printStackTrace() }

                ttsManager?.speak(spokenText, activePreset)

                // Wait 6 seconds for speech, then repeat ringtone briefly
                kotlinx.coroutines.delay(6000)

                if (isActive) {
                    try {
                        ringtone?.play()
                    } catch (e: Exception) { e.printStackTrace() }
                    kotlinx.coroutines.delay(2000)
                }
            }
        }

        setContent {
            MyApplicationTheme {
                AlarmFullScreenContent(
                    title = reminderTitle,
                    script = customScript,
                    onSnooze = {
                        stopEffects()
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        if (reminderId != -1L) notificationManager?.cancel(reminderId.toInt())
                        if (reminderId != -1L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = AppDatabase.getInstance(applicationContext)
                                val repo = ReminderRepository(db.reminderDao())
                                val scheduler = AlarmScheduler(applicationContext)
                                val reminder = repo.getReminderById(reminderId)
                                if (reminder != null) {
                                    val snoozedTime = System.currentTimeMillis() + (10 * 60 * 1000)
                                    val updated = reminder.copy(timeMillis = snoozedTime)
                                    repo.updateReminder(updated)
                                    scheduler.schedule(updated)
                                }
                            }
                        }
                        finish()
                    },
                    onDismiss = {
                        stopEffects()
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        if (reminderId != -1L) notificationManager?.cancel(reminderId.toInt())
                        if (reminderId != -1L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = AppDatabase.getInstance(applicationContext)
                                val repo = ReminderRepository(db.reminderDao())
                                repo.markCompleted(reminderId)
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enforceScreenOvertake()
    }

    private fun enforceScreenOvertake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun startAlarmEffects() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800), 0))
            } else {
                vibrator?.vibrate(longArrayOf(0, 800, 400, 800), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopEffects() {
        try {
            alarmScope?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ringtone?.stop()
        vibrator?.cancel()
        ttsManager?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEffects()
        ttsManager?.shutdown()
    }
}

@Composable
fun AlarmFullScreenContent(
    title: String,
    script: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val bellScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(bellScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(OceanBlueAccent, Color(0xFF0284C7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Yaad AI Reminder Alert",
                    style = MaterialTheme.typography.titleMedium,
                    color = OceanBlueAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (script.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SkyBlueContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OceanBlueAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = script,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanBlueAccent
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dismiss Alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, tint = OceanBlueAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snooze 10 Mins", fontSize = 16.sp, color = OceanBlueAccent)
                }
            }
        }
    }
}
