package com.example.ui.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.receiver.ReminderReceiver
import com.example.service.AlarmService
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OceanBlueAccent
import com.example.ui.theme.SkyBlueContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class AlarmActivity : ComponentActivity() {

    private val reminderIdState = mutableStateOf(-1L)
    private val reminderTitleState = mutableStateOf("Reminder Alert!")
    private val descriptionState = mutableStateOf("")
    private var autoDismissJob: Job? = null

    private val closeUiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("AlarmActivity", "Received ACTION_CLOSE_ALARM_UI: closing alarm screen immediately")
            finishAndRemoveTaskSafely()
        }
    }

    private fun extractIntentData(targetIntent: Intent?) {
        if (targetIntent == null) return
        val id = targetIntent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L).let {
            if (it == -1L) targetIntent.getLongExtra(AlarmService.EXTRA_REMINDER_ID, -1L) else it
        }
        val title = targetIntent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TITLE)
            ?: targetIntent.getStringExtra(AlarmService.EXTRA_REMINDER_TITLE)
            ?: "Reminder Alert!"
        val desc = targetIntent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_SCRIPT)
            ?: targetIntent.getStringExtra(AlarmService.EXTRA_REMINDER_SCRIPT)
            ?: ""

        if (id != -1L) {
            reminderIdState.value = id
        }
        reminderTitleState.value = title
        descriptionState.value = desc
    }

    private fun startAutoDismissTimer() {
        autoDismissJob?.cancel()
        autoDismissJob = lifecycleScope.launch {
            delay(120_000L) // Exactly 2 minutes (120 seconds) unattended timeout
            Log.d("AlarmActivity", "2-minute timeout elapsed without interaction: closing alarm screen and marking missed")
            try {
                val missedIntent = Intent(applicationContext, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_MISSED_ALARM
                    putExtra(AlarmService.EXTRA_REMINDER_ID, reminderIdState.value)
                    putExtra(AlarmService.EXTRA_REMINDER_TITLE, reminderTitleState.value)
                }
                startService(missedIntent)
            } catch (e: Exception) {
                Log.e("AlarmActivity", "Error requesting missed action on timeout", e)
            }
            finishAndRemoveTaskSafely()
        }
    }

    private fun finishAndRemoveTaskSafely() {
        autoDismissJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceScreenOvertake()
        extractIntentData(intent)

        // Register broadcast receiver to close UI whenever AlarmService stops/snoozes/times out
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(closeUiReceiver, IntentFilter(AlarmService.ACTION_CLOSE_ALARM_UI), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(closeUiReceiver, IntentFilter(AlarmService.ACTION_CLOSE_ALARM_UI))
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Failed to register closeUiReceiver", e)
        }

        // Start 2-minute auto-dismiss timer
        startAutoDismissTimer()

        // Ensure AlarmService is running and playing sound continuously for the current reminder
        val currentId = reminderIdState.value
        val currentTitle = reminderTitleState.value
        val currentDesc = descriptionState.value
        try {
            val startServiceIntent = Intent(applicationContext, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                putExtra(AlarmService.EXTRA_REMINDER_ID, currentId)
                putExtra(AlarmService.EXTRA_REMINDER_TITLE, currentTitle)
                putExtra(AlarmService.EXTRA_REMINDER_SCRIPT, currentDesc)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(applicationContext, startServiceIntent)
            } else {
                startService(startServiceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                val activeId by reminderIdState
                val activeTitle by reminderTitleState
                val activeNotes by descriptionState

                AlarmFullScreenContent(
                    title = activeTitle,
                    notes = activeNotes,
                    onSnooze = { snoozeMinutes ->
                        autoDismissJob?.cancel()
                        val snoozeIntent = Intent(applicationContext, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_SNOOZE_ALARM
                            putExtra(AlarmService.EXTRA_REMINDER_ID, activeId)
                            putExtra(AlarmService.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        }
                        startService(snoozeIntent)
                        finishAndRemoveTaskSafely()
                    },
                    onDismiss = {
                        autoDismissJob?.cancel()
                        val dismissIntent = Intent(applicationContext, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_DISMISS_ALARM
                            putExtra(AlarmService.EXTRA_REMINDER_ID, activeId)
                        }
                        startService(dismissIntent)
                        finishAndRemoveTaskSafely()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enforceScreenOvertake()
        extractIntentData(intent)
        startAutoDismissTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoDismissJob?.cancel()
        try {
            unregisterReceiver(closeUiReceiver)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun enforceScreenOvertake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            try {
                keyguardManager?.requestDismissKeyguard(this, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        @Suppress("DEPRECATION")
        try {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


@Composable
fun AlarmFullScreenContent(
    title: String,
    notes: String,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val bellScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
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
                        modifier = Modifier.size(58.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "MEMORY PLUS REMINDER",
                    style = MaterialTheme.typography.titleMedium,
                    color = OceanBlueAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Huge bold text for the reminder title taking prominent screen space
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 38.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        if (notes.isNotBlank() && notes != title) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = OceanBlueAccent.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Dismiss Reminder", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(5) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = OceanBlueAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze 5m", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OceanBlueAccent)
                    }

                    OutlinedButton(
                        onClick = { onSnooze(10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = OceanBlueAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze 10m", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OceanBlueAccent)
                    }
                }
            }
        }
    }
}
