package com.example.connectany.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeOption { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeOption,
    val isListening: Boolean,
    val defaultPopupStyle: String,
    val hasSeenOnboarding: Boolean
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("theme")
    private val IS_LISTENING_KEY = booleanPreferencesKey("is_listening")
    private val DEFAULT_POPUP_STYLE_KEY = stringPreferencesKey("default_popup_style")
    private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            theme = try {
                ThemeOption.valueOf(preferences[THEME_KEY] ?: ThemeOption.SYSTEM.name)
            } catch (e: IllegalArgumentException) {
                ThemeOption.SYSTEM
            },
            isListening = preferences[IS_LISTENING_KEY] ?: true,
            defaultPopupStyle = preferences[DEFAULT_POPUP_STYLE_KEY] ?: "DROP",
            hasSeenOnboarding = preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
        )
    }

    suspend fun setTheme(theme: ThemeOption) {
        context.dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setListening(isListening: Boolean) {
        context.dataStore.edit { it[IS_LISTENING_KEY] = isListening }
    }

    suspend fun setDefaultPopupStyle(style: String) {
        context.dataStore.edit { it[DEFAULT_POPUP_STYLE_KEY] = style }
    }

    suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_ONBOARDING_KEY] = hasSeen }
    }
}
