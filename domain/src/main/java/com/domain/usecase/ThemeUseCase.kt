package com.domain.usecase

import com.domain.model.ThemeMode
import com.domain.repository.SettingsRepository
import javax.inject.Inject

class ThemeUseCase @Inject constructor(
        private val repository: SettingsRepository
) {
    operator fun invoke() = repository.getThemeMode()

    suspend operator fun invoke(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }
}