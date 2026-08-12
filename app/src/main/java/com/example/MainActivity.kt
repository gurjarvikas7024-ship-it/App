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
            val showPaywallLimitDialog by viewModel.showPaywallLimitDialog.collectAsStateWithLifecycle()

            val context = LocalContext.current

            // Silently request notification permission on Android 13+ on first launch without showing custom UI dialogs
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission result handled silently */ }

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
                                            onError = { /* Handled via showPaywallLimitDialog */ }
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
                                    onError = { /* Handled via showPaywallLimitDialog */ }
                                )
                            }
                        )
                    }

                    if (showPaywallLimitDialog) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissPaywallDialog() },
                            title = {
                                Text(
                                    text = "Upgrade to Memory Plus Premium",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepSlateNavy
                                )
                            },
                            text = {
                                Text(
                                    text = "Free version is limited to 2 active reminders. Upgrade for just ₹40/month (or $4.99/year) to unlock unlimited voice reminders.",
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
                                    Text("Upgrade Now", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { viewModel.dismissPaywallDialog() }
                                ) {
                                    Text("Cancel", color = SlateMutedText, fontWeight = FontWeight.Medium)
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
}
