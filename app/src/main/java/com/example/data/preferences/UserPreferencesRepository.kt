package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_LANGUAGE = stringPreferencesKey("language") // "en" or "hi"
        val KEY_VOICE_GENDER = stringPreferencesKey("voice_gender") // "FEMALE" or "MALE"
        val KEY_VOICE_PRESET = stringPreferencesKey("voice_preset") // "Studio Female", "Executive Male", "Soft Narrator", "Bold Leader"
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_LOGIN_PROVIDER = stringPreferencesKey("login_provider") // "GOOGLE", "PHONE", "EMAIL", "GUEST"
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "User"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    val voiceGenderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_GENDER] ?: "FEMALE"
    }

    val voicePresetFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_PRESET] ?: "Studio Female"
    }

    val isOnboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL] ?: "user@example.com"
    }

    val loginProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGIN_PROVIDER] ?: "GUEST"
    }

    val isPremiumFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_PREMIUM] ?: false
    }

    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE]
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = lang
        }
    }

    suspend fun setVoiceGender(gender: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOICE_GENDER] = gender
        }
    }

    suspend fun setVoicePreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOICE_PRESET] = preset
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }

    suspend fun setUserAuth(isLoggedIn: Boolean, email: String, provider: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = isLoggedIn
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_LOGIN_PROVIDER] = provider
        }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = isPremium
        }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(KEY_DARK_MODE)
            } else {
                prefs[KEY_DARK_MODE] = enabled
            }
        }
    }
}
