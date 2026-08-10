package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderEntity
import com.example.data.model.ReminderStatus
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ReminderRepository
import com.example.service.AlarmScheduler
import com.example.service.GeminiReminderService
import com.example.service.ParsedReminderResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class HomeFilterTab(val labelHindi: String, val labelEnglish: String) {
    TODAY("आज", "Today"),
    UPCOMING("आगामी", "Upcoming"),
    MISSED("छूट गए", "Missed"),
    COMPLETED("पूरे हुए", "Completed")
}

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

data class UiState(
    val userName: String = "User",
    val isPremium: Boolean = false,
    val language: String = "hi",
    val voiceGender: String = "FEMALE",
    val isLoggedIn: Boolean = false,
    val userEmail: String = "user@example.com",
    val loginProvider: String = "GUEST",
    val isOnboardingDone: Boolean = false,
    val isDarkMode: Boolean? = null,
    val activeReminderCount: Int = 0,
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

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow<Long>(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val userBasicState = combine(
        userPrefs.userNameFlow,
        userPrefs.isPremiumFlow,
        userPrefs.languageFlow,
        userPrefs.voiceGenderFlow,
        userPrefs.isLoggedInFlow
    ) { name, premium, lang, voice, loggedIn ->
        Tuple5(name, premium, lang, voice, loggedIn)
    }

    private val userExtraState = combine(
        userPrefs.userEmailFlow,
        userPrefs.loginProviderFlow,
        userPrefs.isOnboardingDoneFlow,
        userPrefs.isDarkModeFlow,
        repository.activeCount
    ) { email, provider, onboarding, darkMode, activeCount ->
        Tuple5(email, provider, onboarding, darkMode, activeCount)
    }

    val uiState: StateFlow<UiState> = combine(
        userBasicState,
        userExtraState
    ) { basic, extra ->
        UiState(
            userName = basic.a,
            isPremium = basic.b,
            language = basic.c,
            voiceGender = basic.d,
            isLoggedIn = basic.e,
            userEmail = extra.a,
            loginProvider = extra.b,
            isOnboardingDone = extra.c,
            isDarkMode = extra.d,
            activeReminderCount = extra.e
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    val allReminders: StateFlow<List<ReminderEntity>> = combine(
        repository.allReminders,
        _selectedTab,
        _searchQuery,
        _selectedCategory
    ) { list, tab, query, category ->
        val now = System.currentTimeMillis()
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

        if (!category.isNullOrEmpty()) {
            filtered = filtered.filter { it.category == category }
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

    fun selectTab(tab: HomeFilterTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
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

    fun setVoiceSettings(language: String, gender: String) {
        viewModelScope.launch {
            userPrefs.setLanguage(language)
            userPrefs.setVoiceGender(gender)
        }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            userPrefs.setDarkMode(enabled)
        }
    }

    fun setPremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            userPrefs.setPremiumStatus(isPremium)
        }
    }

    fun parseVoiceReminder(userPrompt: String, onComplete: (ReminderEntity?) -> Unit) {
        viewModelScope.launch {
            val name = uiState.value.userName
            val result = geminiService.parseNaturalLanguageReminder(userPrompt, name)

            try {
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

                val reminder = ReminderEntity(
                    title = result.title,
                    description = userPrompt,
                    timeMillis = cal.timeInMillis,
                    category = result.category,
                    repeatType = result.repeatType,
                    customVoiceScript = result.voiceGreeting.ifEmpty {
                        "$name जी, आपका रिमाइंडर: ${result.title}"
                    }
                )
                onComplete(reminder)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    fun addReminder(reminder: ReminderEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (!currentState.isPremium && currentState.activeReminderCount >= 5) {
                onError("Free plan is limited to 5 active reminders. Upgrade to Premium for unlimited reminders!")
                return@launch
            }

            val id = repository.insertReminder(reminder)
            val newReminder = reminder.copy(id = id)
            alarmScheduler.schedule(newReminder)
            onSuccess()
        }
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
                val snoozedTime = System.currentTimeMillis() + (minutes * 60 * 1000)
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
