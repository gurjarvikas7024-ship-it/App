package com.example.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TTSManager
import com.example.ui.theme.OceanBlueAccent
import com.example.ui.theme.SkyBlueContainer
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveVoiceSettings: (String, String, String) -> Unit,
    onToggleDarkMode: (Boolean?) -> Unit,
    onOpenPaywall: () -> Unit = {},
    onShareApp: () -> Unit = {}
) {
    var nameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var selectedVoicePreset by remember(uiState.voicePreset) { mutableStateOf(uiState.voicePreset) }
    var isTestingVoice by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val testTtsManager = remember {
        TTSManager(context).apply {
            onSpeechFinished = {
                isTestingVoice = false
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            testTtsManager.shutdown()
        }
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isBatteryOptimized = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false
        } else false
    }

    val canDrawOverlays = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    val canScheduleExact = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() == true
        } else true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Profile Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            if (nameInput != uiState.userName) {
                                IconButton(onClick = { onSaveName(nameInput) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Name", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // AI Voice Assistant (Sweet Indian Female Voice - Bodyguard Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SkyBlueContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SkyBlueContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = OceanBlueAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AI Voice Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "भारतीय महिला आवाज़ (साफ़ और मधुर)",
                                fontSize = 12.sp,
                                color = OceanBlueAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Single Voice Badge (Bodyguard Movie / Kareena Style)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SkyBlueContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, OceanBlueAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OceanBlueAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Kareena (Sweet Indian Female)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = OceanBlueAccent,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Active Voice",
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    "Bodyguard फ़िल्म जैसी मीठी, शांत और बिल्कुल साफ़ भारतीय आवाज़। हर रिमाइंडर को बिना किसी शोर के स्पष्ट और प्यार से याद दिलाएगी।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smart Volume Ducking & Clarity Feature
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = OceanBlueAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "स्मार्ट वॉल्यूम डकिंग (Smart Audio Ducking)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "अलार्म बजते वक्त रिंगटोन अपने आप 15% पर धीमी हो जाती है ताकि करीना की आवाज़ 100% स्पष्ट, लाउड और साफ़ सुनाई दे।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test Voice Button
                    Button(
                        onClick = {
                            isTestingVoice = true
                            val testMessage = "Hello! Main aapki AI assistant hoon. Bodyguard movie jaisi meethi aur saaf aawaz me, main aapko aapka har zaroori kaam time par yaad dilaungi."
                            testTtsManager.speak(testMessage, "Indian Female")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent)
                    ) {
                        Icon(
                            imageVector = if (isTestingVoice) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isTestingVoice) "आवाज़ चल रही है (Playing Voice)..." else "Test Voice / आवाज़ सुनें",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Reliable 100% On-Time Alarms Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = OceanBlueAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Zero-Delay Alarm Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlueAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ensure exact on-time ringing when phone screen is locked or while using apps like Facebook & Instagram.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Battery Unrestricted
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Battery Saver", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (isBatteryOptimized) "Optimized (May delay alarms when idle)" else "Unrestricted (Instant alarms guaranteed)",
                                fontSize = 11.sp,
                                color = if (isBatteryOptimized) MaterialTheme.colorScheme.error else OceanBlueAccent
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBatteryOptimized) "Disable" else "Allowed", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Draw Over Other Apps / Pop-up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Display Over Other Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (canDrawOverlays) "Granted (Full screen popup enabled)" else "Permission needed for full screen popup over FB/Insta",
                                fontSize = 11.sp,
                                color = if (canDrawOverlays) OceanBlueAccent else MaterialTheme.colorScheme.error
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (canDrawOverlays) "Active" else "Enable", fontSize = 12.sp)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Exact Alarms Permission", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Required for exact second alarms", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Alarm & Notification Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OceanBlueAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Alarm & Alert Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Full Screen Display Alert", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Opens large alarm screen even when locked or using other apps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Loud Vibration", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Vibrates continuously until snoozed or dismissed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }
                }
            }

            // App Version & Share Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Memory Plus - 100% Free", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Unlimited Alarms • No Payment • Free Forever",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onShareApp,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share App", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Memory Plus Link", "https://ais-pre-mvsv77bjsyvy3eq4bsm3vs-505949836468.asia-east1.run.app")
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "App Link Copied!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Copy Link", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

