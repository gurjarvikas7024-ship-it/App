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
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var ttsManager: TTSManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndKeyguard()

        val reminderId = intent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L)
        val reminderTitle = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TITLE) ?: "रिमाइंडर अलार्म!"
        val customScript = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT) ?: ""

        // Sound & Vibration
        startAlarmEffects()

        // TTS Speech
        ttsManager = TTSManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            val userPrefs = UserPreferencesRepository(applicationContext)
            val name = userPrefs.userNameFlow.first()
            val gender = userPrefs.voiceGenderFlow.first()

            val spokenText = if (customScript.isNotBlank()) customScript
            else "$name जी, उठ जाइए। आपका रिमाइंडर $reminderTitle का समय हो गया है। Best of Luck."

            // Short delay so ringtone rings first then TTS speaks
            kotlinx.coroutines.delay(1000)
            ttsManager?.speak(spokenText, gender)
        }

        setContent {
            MyApplicationTheme {
                AlarmFullScreenContent(
                    title = reminderTitle,
                    script = customScript,
                    onSnooze = {
                        stopEffects()
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

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
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
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = IndigoDark
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
                // Ringing Bell Animated Graphic
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(bellScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AmberAccent, Color(0xFFFF6F00))
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
                    text = "याद AI - समय हो गया!",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (script.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AmberAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = script,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Snooze and Dismiss Buttons
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
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("स्वीकार करें (Dismiss)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("10 मिनट स्नूज़ करें (Snooze 10 Mins)", fontSize = 16.sp)
                }
            }
        }
    }
}
