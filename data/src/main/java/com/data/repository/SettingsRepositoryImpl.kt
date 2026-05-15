package com.data.repository

import com.datastore.managers.SettingsManager
import com.domain.model.ThemeMode
import com.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class SettingsRepositoryImpl @Inject constructor(
        private val settingsManager: SettingsManager
) : SettingsRepository {

    override fun getThemeMode(): Flow<ThemeMode> {
        return settingsManager.themeMode
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
    }
}