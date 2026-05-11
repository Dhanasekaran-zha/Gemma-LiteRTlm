package com.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.model.ChatMessage
import com.domain.usecase.GetGemmaResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var messageObserverJob: Job? = null

    init {
        initGemma()
        observeSessions()
    }

    private fun initGemma() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ChatStatus.LoadingModel) }
            getGemmaResponseUseCase.initializeEngine()
                    .onSuccess { _uiState.update { it.copy(status = ChatStatus.Ready) } }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(status = ChatStatus.Error(error.message ?: "Unknown Error"))
                        }
                    }
        }
    }

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return

        // Use existing sessionId from state (null only on very first message)
        val currentSessionId = _uiState.value.sessionId

        val userMessage = ChatMessage(content = userPrompt, isFromUser = true)
        _uiState.update {
            it.copy(messages = it.messages + userMessage, status = ChatStatus.Generating)
        }


        viewModelScope.launch {
            try {
                getGemmaResponseUseCase(userPrompt, currentSessionId)
                        .onCompletion { cause ->
                            if (cause == null) { // Only on clean completion
                                val resolvedSessionId = currentSessionId
                                        ?: getGemmaResponseUseCase.getCurrentSessionId()

                                // Cancel old observer, start fresh one for this session
                                startObservingMessages(resolvedSessionId)

                                _uiState.update {
                                    it.copy(
                                            status = ChatStatus.Ready,
                                            streamingText = "",
                                            sessionId = resolvedSessionId
                                    )
                                }
                            }
                        }
                        .collect { token ->
                            updateAiMessage(token)
                        }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = ChatStatus.Error(e.message ?: "Inference Failed"))
                }
            }
        }
    }

    private fun updateAiMessage(newText: String) {
        _uiState.update { state ->
            state.copy(streamingText = state.streamingText + newText)
        }
    }

    private fun startObservingMessages(sessionId: Long?) {
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            getGemmaResponseUseCase.getMessagesForSession(sessionId!!).collect { messages ->
                if (_uiState.value.status != ChatStatus.Generating) {
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            getGemmaResponseUseCase.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getGemmaResponseUseCase.dispose()
    }

    fun startNewChat() {
        messageObserverJob?.cancel()
        getGemmaResponseUseCase.createNewSession()

        _uiState.update {
            it.copy(
                    messages = emptyList(),
                    status = ChatStatus.Ready,
                    sessionId = null
            )
        }
    }

    fun loadSession(sessionId: Long?) {
        messageObserverJob?.cancel()
        getGemmaResponseUseCase.createNewSession()
        _uiState.update {
            it.copy(
                    status = ChatStatus.Ready,
                    sessionId = sessionId
            )
        }
        startObservingMessages(sessionId = sessionId)
    }
}