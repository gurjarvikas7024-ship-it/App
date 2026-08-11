package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ReminderEntity
import com.example.ui.components.VoiceInputBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
            val remindersForSelectedDate by viewModel.remindersForSelectedDate.collectAsStateWithLifecycle()

            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("yaad_ai_prefs", Context.MODE_PRIVATE) }
            var isSetupCompleted by remember { mutableStateOf(prefs.getBoolean("is_setup_completed", false)) }

            // Helper function to check all required permissions
            fun hasAllPermissions(): Boolean {
                val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val exactAlarmOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() == true
                } else true

                val overlayOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true

                return notifOk && exactAlarmOk && overlayOk
            }

            var showPermissionDialog by remember { mutableStateOf(!isSetupCompleted && !hasAllPermissions()) }

            // Launcher for notification permission
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Proceed to next permissions in flow if needed
            }

            val darkTheme = when (uiState.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            var showVoiceModal by remember { mutableStateOf(false) }
            var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (uiState.isOnboardingDone) "home" else "onboarding"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onSaveUser = { name, lang, voice, loggedIn, email, provider ->
                                    viewModel.saveUserName(name)
                                    viewModel.setVoiceSettings(lang, voice)
                                    viewModel.setAuth(loggedIn, email, provider)
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                reminders = allReminders,
                                selectedTab = selectedTab,
                                searchQuery = searchQuery,
                                onSelectTab = { viewModel.selectTab(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onOpenVoiceDialog = { showVoiceModal = true },
                                onOpenAddReminder = {
                                    editingReminder = null
                                    navController.navigate("add_edit")
                                },
                                onOpenCalendar = { navController.navigate("calendar") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenPaywall = { navController.navigate("paywall") },
                                onToggleCompleted = { viewModel.markCompleted(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onEditReminder = { reminder ->
                                    editingReminder = reminder
                                    navController.navigate("add_edit")
                                },
                                onSnoozeReminder = { viewModel.snoozeReminder(it) }
                            )
                        }

                        composable("add_edit") {
                            AddEditReminderScreen(
                                userName = uiState.userName,
                                existingReminder = editingReminder,
                                onBack = { navController.popBackStack() },
                                onSave = { reminder ->
                                    if (editingReminder == null) {
                                        viewModel.addReminder(
                                            reminder = reminder,
                                            onSuccess = { navController.popBackStack() },
                                            onError = { err ->
                                                navController.navigate("paywall")
                                            }
                                        )
                                    } else {
                                        viewModel.updateReminder(reminder)
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable("calendar") {
                            CalendarScreen(
                                selectedDateMillis = selectedDateMillis,
                                remindersForDate = remindersForSelectedDate,
                                onSelectDate = { viewModel.setSelectedDate(it) },
                                onBack = { navController.popBackStack() },
                                onToggleCompleted = { viewModel.markCompleted(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onEditReminder = { reminder ->
                                    editingReminder = reminder
                                    navController.navigate("add_edit")
                                },
                                onSnoozeReminder = { viewModel.snoozeReminder(it) }
                            )
                        }

                        composable("settings") {
                            SettingsProfileScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onSaveName = { viewModel.saveUserName(it) },
                                onSaveVoiceSettings = { lang, voice, preset -> viewModel.setVoiceSettings(lang, voice, preset) },
                                onToggleDarkMode = { viewModel.setDarkMode(it) },
                                onOpenPaywall = { navController.navigate("paywall") }
                            )
                        }

                        composable("paywall") {
                            SubscriptionPaywallScreen(
                                isCurrentlyPremium = uiState.isPremium,
                                onBack = { navController.popBackStack() },
                                onActivatePremium = { viewModel.setPremiumStatus(true) }
                            )
                        }
                    }

                    if (showVoiceModal) {
                        VoiceInputBottomSheet(
                            userName = uiState.userName,
                            onDismiss = { showVoiceModal = false },
                            onParseVoicePrompt = { prompt, onDone ->
                                viewModel.parseVoiceReminder(prompt, onDone)
                            },
                            onSaveReminder = { reminder ->
                                viewModel.addReminder(
                                    reminder = reminder,
                                    onSuccess = {},
                                    onError = {
                                        navController.navigate("paywall")
                                    }
                                )
                            }
                        )
                    }

                    // Automated One-Click Permission Dialog
                    if (showPermissionDialog) {
                        Dialog(onDismissRequest = {
                            showPermissionDialog = false
                            prefs.edit().putBoolean("is_setup_completed", true).apply()
                        }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = CleanPureWhite),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, SkyBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(SkyBlueContainer, shape = RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = OceanBlueAccent,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Enable Smart Voice Alarms",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepSlateNavy,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Yaad AI needs permissions to ring full-screen AI voice alarms even while using YouTube, Instagram, or when locked.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateMutedText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            // Execute automated permission flow
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }

                                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == false) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                                try {
                                                    val intent = Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }

                                            prefs.edit().putBoolean("is_setup_completed", true).apply()
                                            showPermissionDialog = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Enable Setup (One Click)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = {
                                            prefs.edit().putBoolean("is_setup_completed", true).apply()
                                            showPermissionDialog = false
                                        }
                                    ) {
                                        Text("Skip for now", color = SlateMutedText, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
