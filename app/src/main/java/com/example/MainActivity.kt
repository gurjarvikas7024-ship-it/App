package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ReminderEntity
import com.example.service.SmartVoiceParser
import com.example.ui.components.VoiceInputBottomSheet
import com.example.ui.paywall.PaywallActivity
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Battery Optimization check for reliable alarms
        checkAndRequestBatteryOptimization()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
            val remindersForSelectedDate by viewModel.remindersForSelectedDate.collectAsStateWithLifecycle()
            val showPaywallLimitDialog by viewModel.showPaywallLimitDialog.collectAsStateWithLifecycle()

            val context = LocalContext.current
            var showVoiceModal by remember { mutableStateOf(false) }
            var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

            // 1. Strict 2-Reminder Limit Gatekeeper
            fun checkGatekeeperAndProceed(): Boolean {
                val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                val isPro = prefs.getBoolean("is_pro_unlocked", false)
                val count = prefs.getInt("reminder_count", 0)

                if (!isPro && count >= 2) {
                    Toast.makeText(
                        this@MainActivity,
                        "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                    return false // Block reminder creation
                }

                if (!isPro) {
                    prefs.edit().putInt("reminder_count", count + 1).apply()
                }
                return true
            }

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Handled */ }

            // Speech-to-Text Voice Recognizer Launcher
            val speechRecognizerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = spokenTextList?.firstOrNull()?.trim()

                    if (!spokenText.isNullOrBlank()) {
                        // Gatekeeper check before saving voice reminder
                        if (!checkGatekeeperAndProceed()) {
                            return@rememberLauncherForActivityResult
                        }

                        val parsed = SmartVoiceParser.parse(spokenText, uiState.userName)
                        val reminder = ReminderEntity(
                            title = parsed.title,
                            description = spokenText,
                            timeMillis = parsed.timeMillis,
                            repeatType = parsed.repeatType,
                            customVoiceScript = parsed.voiceScript,
                            voicePreset = uiState.voicePreset
                        )

                        viewModel.addReminder(
                            reminder = reminder,
                            onSuccess = {
                                val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.ENGLISH)
                                val dateStr = sdf.format(Date(parsed.timeMillis))
                                Toast.makeText(
                                    context,
                                    "🔔 Reminder Set: \"${parsed.title}\" for $dateStr",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = {
                                PaywallActivity.start(context)
                            }
                        )
                    } else {
                        showVoiceModal = true
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        try {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to launch notification permission request", e)
                        }
                    }
                }
            }

            val darkTheme = when (uiState.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

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
                                on1TapMic = {
                                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                    val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                    val count = prefs.getInt("reminder_count", 0)

                                    if (!isPro && count >= 2) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                        return@HomeScreen
                                    }

                                    startNativeSpeechRecognizer(speechRecognizerLauncher)
                                },
                                onOpenVoiceDialog = {
                                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                    val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                    val count = prefs.getInt("reminder_count", 0)

                                    if (!isPro && count >= 2) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                        return@HomeScreen
                                    }

                                    showVoiceModal = true
                                },
                                onOpenAddReminder = {
                                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                    val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                    val count = prefs.getInt("reminder_count", 0)

                                    if (!isPro && count >= 2) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                        return@HomeScreen
                                    }

                                    editingReminder = null
                                    navController.navigate("add_edit")
                                },
                                onOpenCalendar = { navController.navigate("calendar") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenPaywall = { PaywallActivity.start(context) },
                                onToggleCompleted = { viewModel.markCompleted(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onEditReminder = { reminder ->
                                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                    val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                    val count = prefs.getInt("reminder_count", 0)

                                    if (!isPro && count >= 2) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                        return@HomeScreen
                                    }

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
                                        if (!checkGatekeeperAndProceed()) {
                                            return@AddEditReminderScreen
                                        }

                                        viewModel.addReminder(
                                            reminder = reminder,
                                            onSuccess = { navController.popBackStack() },
                                            onError = {
                                                Toast.makeText(
                                                    context,
                                                    "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                PaywallActivity.start(context)
                                            }
                                        )
                                    } else {
                                        val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                        val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                        val count = prefs.getInt("reminder_count", 0)

                                        if (!isPro && count >= 2) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                            return@AddEditReminderScreen
                                        }

                                        viewModel.updateReminder(
                                            reminder = reminder,
                                            onSuccess = { navController.popBackStack() },
                                            onError = {
                                                Toast.makeText(
                                                    context,
                                                    "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                PaywallActivity.start(context)
                                            }
                                        )
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
                                    val prefs = getSharedPreferences("MemoryPlusPrefs", Context.MODE_PRIVATE)
                                    val isPro = prefs.getBoolean("is_pro_unlocked", false)
                                    val count = prefs.getInt("reminder_count", 0)

                                    if (!isPro && count >= 2) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "2 Free Reminders Limit Reached! Unlock Pro for unlimited access.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                                        return@CalendarScreen
                                    }

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
                                onOpenPaywall = { PaywallActivity.start(context) }
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
                                if (!checkGatekeeperAndProceed()) {
                                    showVoiceModal = false
                                    return@VoiceInputBottomSheet
                                }

                                viewModel.addReminder(
                                    reminder = reminder,
                                    onSuccess = {
                                        showVoiceModal = false
                                        Toast.makeText(context, "Reminder saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        showVoiceModal = false
                                        PaywallActivity.start(context)
                                    }
                                )
                            }
                        )
                    }

                    if (showPaywallLimitDialog) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissPaywallDialog() },
                            title = {
                                Text(
                                    text = "2 Free Reminders Limit Reached",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSlateNavy
                                )
                            },
                            text = {
                                Text(
                                    text = "You have reached your 2 free reminders limit. Upgrade to Lifetime Pro for ₹399 to unlock unlimited voice & photo reminders forever!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMutedText
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.dismissPaywallDialog()
                                        PaywallActivity.start(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Unlock Lifetime Pro (₹399)", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { viewModel.dismissPaywallDialog() }
                                ) {
                                    Text("Dismiss", color = SlateMutedText, fontWeight = FontWeight.Medium)
                                }
                            },
                            containerColor = CleanPureWhite,
                            shape = RoundedCornerShape(22.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPreferences()
    }

    private fun startNativeSpeechRecognizer(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak reminder (e.g. \"Tomorrow 9 AM meeting\" or \"Kal subah 9 baje\")")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition unavailable. Opening manual input.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check & Request Battery Optimization Whitelist for reliable alarms
     */
    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.d("MainActivity", "Direct battery whitelist intent ignored by OS", e)
            }
        }
    }
}
