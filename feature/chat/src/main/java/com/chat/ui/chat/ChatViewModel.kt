package com.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.usecase.GetGemmaResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
        private val getGemmaResponseUseCase: GetGemmaResponseUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        initGemma()
    }

    private fun initGemma() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ChatStatus.LoadingModel) }

            getGemmaResponseUseCase.initializeEngine()
                    .onSuccess {
                        _uiState.update { it.copy(status = ChatStatus.Ready) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(status = ChatStatus.Error(error.message ?: "Unknown Error")) }
                    }
        }
    }

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val userMessage = ChatMessage(content = userPrompt, isFromUser = true)
        _uiState.update {
            it.copy(
                    messages = it.messages + userMessage,
                    status = ChatStatus.Generating
            )
        }
        viewModelScope.launch {
            try {
                getGemmaResponseUseCase(userPrompt)
                        .onCompletion {
                            _uiState.update { it.copy(status = ChatStatus.Ready) } // ✅ always called
                        }
                        .collect { token ->
                            updateAiMessage(token)
                        }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ChatStatus.Error(e.message ?: "Inference Failed")) }
            }
        }
    }

    private fun updateAiMessage(newText: String) {
        _uiState.update { state ->
            val lastMsg = state.messages.lastOrNull()
            if (lastMsg != null && !lastMsg.isFromUser) {
                val updatedMessages = state.messages.toMutableList()
                updatedMessages[updatedMessages.lastIndex] = lastMsg.copy(content = lastMsg.content + newText)
                state.copy(messages = updatedMessages)
            } else {
                // Create a new AI bubble
                state.copy(messages = state.messages + ChatMessage(content = newText, isFromUser = false))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getGemmaResponseUseCase.dispose() // Crucial for releasing NPU/GPU memory
    }
}