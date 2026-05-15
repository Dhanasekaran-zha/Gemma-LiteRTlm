package com.domain.repository

import com.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun getThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}