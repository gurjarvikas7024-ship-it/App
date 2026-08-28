package com.example

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.EditText
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
import com.example.data.preferences.PreferenceManager
import com.example.service.SmartVoiceParser
import com.example.ui.components.VoiceInputBottomSheet
import com.example.ui.paywall.PaywallScreenContent
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()

    companion object {
        const val STRIPE_PAYMENT_URL = "https://buy.stripe.com/YOUR_STRIPE_LINK"
        const val RAZORPAY_UPI_URL = "https://rzp.io/l/YOUR_RAZORPAY_LINK"
    }

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
            val showPaywallLimitDialog by viewModel.showPaywallLimitDialog.collectAsStateWithLifecycle()

            val context = LocalContext.current
            var showVoiceModal by remember { mutableStateOf(false) }
            var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

            // Silently request notification permission on Android 13+
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
                        val parsed = SmartVoiceParser.parse(spokenText, uiState.userName)
                        val reminder = ReminderEntity(
                            title = parsed.title,
                            description = spokenText,
                            timeMillis = parsed.timeMillis,
                            repeatType = "ONCE",
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
                                viewModel.refreshPreferences()
                            }
                        )
                    } else {
                        showVoiceModal = true
                    }
                } else {
                    showVoiceModal = true
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        try {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: Exception) {
                            e.printStackTrace()
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

                    // If locked, render the Paywall hard screen directly
                    if (uiState.isLocked) {
                        PaywallScreenContent(
                            onPayStripe = { openUrl(STRIPE_PAYMENT_URL) },
                            onPayRazorpay = { openUrl(RAZORPAY_UPI_URL) },
                            onSecretKeyClick = { showSecretKeyDialog() }
                        )
                    } else {
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
                                        if (PreferenceManager.isLocked(context)) {
                                            viewModel.refreshPreferences()
                                        } else {
                                            startNativeSpeechRecognizer(speechRecognizerLauncher)
                                        }
                                    },
                                    onOpenVoiceDialog = {
                                        if (PreferenceManager.isLocked(context)) {
                                            viewModel.refreshPreferences()
                                        } else {
                                            showVoiceModal = true
                                        }
                                    },
                                    onOpenAddReminder = {
                                        if (PreferenceManager.isLocked(context)) {
                                            viewModel.refreshPreferences()
                                        } else {
                                            editingReminder = null
                                            navController.navigate("add_edit")
                                        }
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
                                                onError = {
                                                    viewModel.refreshPreferences()
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
                                PaywallScreenContent(
                                    onPayStripe = { openUrl(STRIPE_PAYMENT_URL) },
                                    onPayRazorpay = { openUrl(RAZORPAY_UPI_URL) },
                                    onSecretKeyClick = { showSecretKeyDialog() }
                                )
                            }
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
                                    onSuccess = {
                                        showVoiceModal = false
                                        Toast.makeText(context, "Reminder saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        showVoiceModal = false
                                        viewModel.refreshPreferences()
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
                                    text = "Free Trial Ended (2 Free Reminders Used)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSlateNavy
                                )
                            },
                            text = {
                                Text(
                                    text = "You have used your 2 free reminders. Upgrade to Pro to continue using Memory Plus with unlimited smart reminders.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMutedText
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.dismissPaywallDialog()
                                        navController.navigate("paywall")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlueAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Upgrade to Pro", fontWeight = FontWeight.Bold, color = Color.White)
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

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link in browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSecretKeyDialog() {
        val input = EditText(this).apply {
            hint = "Enter Secret Key (e.g. MP2026PRO)"
            setSingleLine(true)
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Pro Secret Key")
            .setMessage("If you have completed payment or received a VIP activation key, enter it below:")
            .setView(input)
            .setPositiveButton("Activate") { dialog, _ ->
                val key = input.text.toString().trim()
                if (viewModel.unlockWithSecretKey(key)) {
                    Toast.makeText(
                        this,
                        "🎉 Memory Plus Pro Unlocked Successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        "❌ Invalid Secret Key. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
            Toast.makeText(this, "Opening voice helper", Toast.LENGTH_SHORT).show()
        }
    }
}
