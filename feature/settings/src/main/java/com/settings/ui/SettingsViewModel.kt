package com.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.model.ThemeMode
import com.domain.usecase.ThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
        private val themeUseCase: ThemeUseCase,
) : ViewModel() {

    val themeMode = themeUseCase()
            .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    ThemeMode.SYSTEM
            )

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            themeUseCase.invoke(mode)
        }
    }
}