package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ReminderEntity
import com.example.ui.components.VoiceInputBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
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
            val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
            val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()
            val remindersForSelectedDate by viewModel.remindersForSelectedDate.collectAsStateWithLifecycle()

            // Request Notification permission for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                                selectedCategory = selectedCategory,
                                onSelectTab = { viewModel.selectTab(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSelectCategory = { viewModel.selectCategory(it) },
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
                                onSetVoiceSettings = { lang, voice -> viewModel.setVoiceSettings(lang, voice) },
                                onSetDarkMode = { viewModel.setDarkMode(it) },
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
                }
            }
        }
    }
}
