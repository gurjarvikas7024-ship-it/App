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
    val context = LocalContext.current

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

