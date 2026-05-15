package com.gemmaEdge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.model.ThemeMode
import com.domain.usecase.ThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
        private val themeUseCase: ThemeUseCase
) : ViewModel() {

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    init {
        observeTheme()
    }

    private fun observeTheme() {

        viewModelScope.launch {

            themeUseCase().collect {
                themeMode = it
            }
        }
    }
}