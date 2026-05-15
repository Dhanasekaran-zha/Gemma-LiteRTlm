package com.datastore.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.domain.model.ThemeMode
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
        private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val SETTINGS_THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeMode = dataStore.data.map { preferences ->
        val value = preferences[SETTINGS_THEME_KEY]
                ?: ThemeMode.SYSTEM.name

        ThemeMode.valueOf(value)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SETTINGS_THEME_KEY] = mode.name
        }
    }
}