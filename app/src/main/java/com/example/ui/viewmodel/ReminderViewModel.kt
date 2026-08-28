package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderEntity
import com.example.data.model.ReminderStatus
import com.example.data.preferences.PreferenceManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.service.AlarmScheduler
import com.example.service.GeminiReminderService
import com.example.service.ParsedReminderResult
import com.example.service.SmartVoiceParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class HomeFilterTab(val labelEnglish: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    MISSED("Missed"),
    COMPLETED("Completed")
}

data class UserVoice(val gender: String, val preset: String)
data class UserBasic(val name: String, val isPremium: Boolean, val language: String, val voice: UserVoice, val isLoggedIn: Boolean)
data class UserExtra(val email: String, val provider: String, val onboarding: Boolean, val darkMode: Boolean?, val activeCount: Int)

data class UiState(
    val userName: String = "User",
    val isPremium: Boolean = false,
    val language: String = "en",
    val voiceGender: String = "FEMALE",
    val voicePreset: String = "Studio Female",
    val isLoggedIn: Boolean = false,
    val userEmail: String = "user@example.com",
    val loginProvider: String = "GUEST",
    val isOnboardingDone: Boolean = false,
    val isDarkMode: Boolean? = null,
    val activeReminderCount: Int = 0,
    val remindersCreatedCount: Int = 0,
    val isProUnlocked: Boolean = false,
    val isLocked: Boolean = false,
    val remainingFreeCount: Int = 2,
    val isParsingVoice: Boolean = false,
    val voiceParseError: String? = null,
    val lastParsedResult: ParsedReminderResult? = null
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ReminderRepository(db.reminderDao())
    private val userPrefs = UserPreferencesRepository(application)
    private val alarmScheduler = AlarmScheduler(application)
    private val geminiService = GeminiReminderService()

    private val _selectedTab = MutableStateFlow(HomeFilterTab.TODAY)
    val selectedTab: StateFlow<HomeFilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow<Long>(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _showPaywallLimitDialog = MutableStateFlow(false)
    val showPaywallLimitDialog: StateFlow<Boolean> = _showPaywallLimitDialog.asStateFlow()

    private val _preferenceRefreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val voiceSettingsFlow = combine(userPrefs.voiceGenderFlow, userPrefs.voicePresetFlow) { gender, preset ->
        UserVoice(gender, preset)
    }

    private val userBasicState = combine(
        userPrefs.userNameFlow,
        userPrefs.isPremiumFlow,
        userPrefs.languageFlow,
        voiceSettingsFlow,
        userPrefs.isLoggedInFlow
    ) { name, premium, lang, voice, loggedIn ->
        UserBasic(name, premium, lang, voice, loggedIn)
    }

    private val userExtraState = combine(
        userPrefs.userEmailFlow,
        userPrefs.loginProviderFlow,
        userPrefs.isOnboardingDoneFlow,
        userPrefs.isDarkModeFlow,
        repository.activeCount
    ) { email, provider, onboarding, darkMode, activeCount ->
        UserExtra(email, provider, onboarding, darkMode, activeCount)
    }

    val uiState: StateFlow<UiState> = combine(
        userBasicState,
        userExtraState,
        _preferenceRefreshTrigger
    ) { basic, extra, _ ->
        val app = getApplication<Application>()
        val proUnlocked = PreferenceManager.isProUnlocked(app) || basic.isPremium
        val createdCount = PreferenceManager.getRemindersCount(app)
        val locked = !PreferenceManager.canCreateReminder(app) && !basic.isPremium
        val remaining = if (proUnlocked) Int.MAX_VALUE else (PreferenceManager.MAX_FREE_REMINDERS - createdCount).coerceAtLeast(0)

        UiState(
            userName = basic.name,
            isPremium = proUnlocked,
            language = basic.language,
            voiceGender = basic.voice.gender,
            voicePreset = basic.voice.preset,
            isLoggedIn = basic.isLoggedIn,
            userEmail = extra.email,
            loginProvider = extra.provider,
            isOnboardingDone = extra.onboarding,
            isDarkMode = extra.darkMode,
            activeReminderCount = extra.activeCount,
            remindersCreatedCount = createdCount,
            isProUnlocked = proUnlocked,
            isLocked = locked,
            remainingFreeCount = remaining
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    val allReminders: StateFlow<List<ReminderEntity>> = combine(
        repository.allReminders,
        _selectedTab,
        _searchQuery
    ) { list, tab, query ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        var filtered = when (tab) {
            HomeFilterTab.TODAY -> list.filter {
                it.status == ReminderStatus.PENDING.name && it.timeMillis in startOfDay..endOfDay
            }
            HomeFilterTab.UPCOMING -> list.filter {
                it.status == ReminderStatus.PENDING.name && it.timeMillis > endOfDay
            }
            HomeFilterTab.MISSED -> list.filter {
                it.status == ReminderStatus.MISSED.name || (it.status == ReminderStatus.PENDING.name && it.timeMillis < startOfDay)
            }
            HomeFilterTab.COMPLETED -> list.filter {
                it.status == ReminderStatus.COMPLETED.name
            }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }

        filtered.sortedBy { it.timeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersForSelectedDate: StateFlow<List<ReminderEntity>> = combine(
        repository.allReminders,
        _selectedDateMillis
    ) { list, dateMillis ->
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24 * 60 * 60 * 1000

        list.filter { it.timeMillis in start..end }.sortedBy { it.timeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshPreferences() {
        _preferenceRefreshTrigger.value = System.currentTimeMillis()
    }

    fun selectTab(tab: HomeFilterTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedDate(dateMillis: Long) {
        _selectedDateMillis.value = dateMillis
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            userPrefs.setUserName(name)
            userPrefs.setOnboardingDone(true)
        }
    }

    fun setAuth(isLoggedIn: Boolean, email: String, provider: String) {
        viewModelScope.launch {
            userPrefs.setUserAuth(isLoggedIn, email, provider)
        }
    }

    fun setVoiceSettings(language: String, gender: String, preset: String = "Studio Female") {
        viewModelScope.launch {
            userPrefs.setLanguage(language)
            userPrefs.setVoiceGender(gender)
            userPrefs.setVoicePreset(preset)
        }
    }

    fun setVoicePreset(preset: String) {
        viewModelScope.launch {
            userPrefs.setVoicePreset(preset)
        }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            userPrefs.setDarkMode(enabled)
        }
    }

    fun setPremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            PreferenceManager.setProUnlocked(getApplication(), isPremium)
            userPrefs.setPremiumStatus(isPremium)
            refreshPreferences()
        }
    }

    fun unlockWithSecretKey(key: String): Boolean {
        val success = PreferenceManager.verifyAndUnlockSecretKey(getApplication(), key)
        if (success) {
            viewModelScope.launch {
                userPrefs.setPremiumStatus(true)
                refreshPreferences()
            }
        }
        return success
    }

    fun parseVoiceReminder(userPrompt: String, onComplete: (ReminderEntity?) -> Unit) {
        viewModelScope.launch {
            val name = uiState.value.userName
            // 1. Fast, highly reliable natural language keyword parser
            val localParsed = SmartVoiceParser.parse(userPrompt, name)

            try {
                // If local parser succeeded with valid title & future timestamp
                val reminder = ReminderEntity(
                    title = localParsed.title,
                    description = userPrompt,
                    timeMillis = localParsed.timeMillis,
                    repeatType = "ONCE",
                    customVoiceScript = localParsed.voiceScript,
                    voicePreset = uiState.value.voicePreset
                )
                onComplete(reminder)
            } catch (e: Exception) {
                // Fallback to Gemini AI service
                try {
                    val result = geminiService.parseNaturalLanguageReminder(userPrompt, name)
                    val dateParts = result.dateString.split("-")
                    val timeParts = result.timeString.split(":")

                    val cal = Calendar.getInstance()
                    if (dateParts.size == 3) {
                        cal.set(Calendar.YEAR, dateParts[0].toInt())
                        cal.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                        cal.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                    }
                    if (timeParts.size == 2) {
                        cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        cal.set(Calendar.MINUTE, timeParts[1].toInt())
                        cal.set(Calendar.SECOND, 0)
                    }
                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    val geminiReminder = ReminderEntity(
                        title = result.title,
                        description = userPrompt,
                        timeMillis = cal.timeInMillis,
                        repeatType = result.repeatType,
                        customVoiceScript = result.voiceGreeting.ifEmpty {
                            "Hello $name, this is your reminder for ${result.title}"
                        },
                        voicePreset = uiState.value.voicePreset
                    )
                    onComplete(geminiReminder)
                } catch (e2: Exception) {
                    onComplete(null)
                }
            }
        }
    }

    fun addReminder(reminder: ReminderEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val prefs = app.getSharedPreferences(PreferenceManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val isPro = prefs.getBoolean(PreferenceManager.KEY_IS_PRO_UNLOCKED, false)
            val totalCreated = prefs.getInt(PreferenceManager.KEY_LIFETIME_REMINDERS_CREATED, prefs.getInt(PreferenceManager.KEY_REMINDERS_COUNT, 0))

            if (!isPro && totalCreated >= PreferenceManager.MAX_FREE_REMINDERS) {
                _showPaywallLimitDialog.value = true
                onError("Free trial limit reached! Upgrade to Pro.")
                return@launch
            }

            try {
                val id = repository.saveReminder(app, reminder)
                val newReminder = reminder.copy(id = id)
                alarmScheduler.schedule(newReminder)
                refreshPreferences()
                onSuccess()
            } catch (e: Exception) {
                _showPaywallLimitDialog.value = true
                onError(e.message ?: "Free trial limit reached! Upgrade to Pro.")
            }
        }
    }

    fun dismissPaywallDialog() {
        _showPaywallLimitDialog.value = false
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            alarmScheduler.schedule(reminder)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            alarmScheduler.cancel(id)
        }
    }

    fun markCompleted(id: Long) {
        viewModelScope.launch {
            repository.markCompleted(id)
            alarmScheduler.cancel(id)
        }
    }

    fun snoozeReminder(id: Long, minutes: Int = 10) {
        viewModelScope.launch {
            val reminder = repository.getReminderById(id)
            if (reminder != null) {
                val snoozedTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                val updated = reminder.copy(
                    timeMillis = snoozedTime,
                    status = ReminderStatus.PENDING.name
                )
                repository.updateReminder(updated)
                alarmScheduler.schedule(updated)
            }
        }
    }
}
