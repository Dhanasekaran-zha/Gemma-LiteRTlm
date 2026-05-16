package com.chat.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.model.ChatMessage
import com.domain.model.GenerationState
import com.domain.model.MessageRole
import com.domain.model.MessageType
import com.domain.usecase.GetGemmaResponseUseCase
import com.utils.media.MediaStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Chat screen.
 *
 * Orchestrates:
 * - LiteRT engine initialization
 * - Image persistence via MediaStorageManager
 * - Message sending (text + multimodal)
 * - Token streaming with batched UI updates
 * - Session management
 *
 * Performance optimizations:
 * - Token streaming uses StringBuilder + batched StateFlow updates
 * - Message observation uses distinctUntilChanged from repository
 * - Session switching cancels stale observers
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getGemmaResponseUseCase: GetGemmaResponseUseCase,
    private val mediaStorageManager: MediaStorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageObserverJob: Job? = null
    private var streamingBuffer = StringBuilder()
    private var lastUiUpdateTime = 0L

    init {
        initGemma()
        observeSessions()
    }

    // ─── Engine Initialization ─────────────────────────────────────

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

    // ─── Image Selection ───────────────────────────────────────────

    /**
     * Called when user selects an image from the picker.
     * Persists the image to app storage immediately on a background thread.
     */
    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(status = ChatStatus.SavingImage) }
                val storedMedia = mediaStorageManager.saveImage(uri)
                _uiState.update {
                    it.copy(
                        pendingImageUri = storedMedia.localUri,
                        pendingImageMimeType = storedMedia.mimeType,
                        status = ChatStatus.Ready
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = ChatStatus.Error("Failed to save image: ${e.message}")
                    )
                }
            }
        }
    }

    fun clearPendingImage() {
        val pendingUri = _uiState.value.pendingImageUri
        if (pendingUri != null) {
            viewModelScope.launch {
                mediaStorageManager.deleteMedia(pendingUri)
            }
        }
        _uiState.update {
            it.copy(pendingImageUri = null, pendingImageMimeType = null)
        }
    }

    // ─── Message Sending ───────────────────────────────────────────

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank() && _uiState.value.pendingImageUri == null) return

        val currentSessionId = _uiState.value.sessionId
        val imageFilePath = _uiState.value.pendingImageUri
        val imageMimeType = _uiState.value.pendingImageMimeType

        // Determine message type
        val messageType = when {
            imageFilePath != null && userPrompt.isNotBlank() -> MessageType.IMAGE_TEXT
            imageFilePath != null -> MessageType.IMAGE
            else -> MessageType.TEXT
        }

        // Optimistically add user message to UI
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            text = userPrompt,
            imageUri = imageFilePath,
            imageMimeType = imageMimeType,
            messageType = messageType,
            generationState = GenerationState.COMPLETE
        )

        // Reset streaming state
        streamingBuffer.clear()
        lastUiUpdateTime = 0L

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                status = ChatStatus.Generating,
                streamingText = "",
                pendingImageUri = null,
                pendingImageMimeType = null
            )
        }

        viewModelScope.launch {
            try {
                getGemmaResponseUseCase(userPrompt, imageFilePath, currentSessionId)
                    .onCompletion { cause ->
                        if (cause == null) {
                            val resolvedSessionId = currentSessionId
                                ?: getGemmaResponseUseCase.getCurrentSessionId()

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

    // ─── Token Streaming ───────────────────────────────────────────

    /**
     * Batches incoming tokens to reduce StateFlow churn and recompositions.
     * Updates UI at most every ~40ms to maintain smooth scrolling.
     */
    private fun updateAiMessage(newText: String) {
        streamingBuffer.append(newText)

        val now = System.currentTimeMillis()
        if (now - lastUiUpdateTime >= 40) {
            lastUiUpdateTime = now
            val currentText = streamingBuffer.toString()
            _uiState.update { state ->
                state.copy(streamingText = currentText)
            }
        }
    }

    // ─── Message Observation ───────────────────────────────────────

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

    // ─── Session Management ────────────────────────────────────────

    fun startNewChat() {
        messageObserverJob?.cancel()
        getGemmaResponseUseCase.createNewSession()
        streamingBuffer.clear()

        _uiState.update {
            it.copy(
                messages = emptyList(),
                status = ChatStatus.Ready,
                sessionId = null,
                streamingText = "",
                pendingImageUri = null,
                pendingImageMimeType = null
            )
        }
    }

    fun loadSession(sessionId: Long?) {
        messageObserverJob?.cancel()
        getGemmaResponseUseCase.createNewSession()
        streamingBuffer.clear()
        _uiState.update {
            it.copy(
                status = ChatStatus.Ready,
                sessionId = sessionId,
                streamingText = ""
            )
        }
        startObservingMessages(sessionId = sessionId)
    }

    fun dismissError() {
        _uiState.update {
            it.copy(status = ChatStatus.Ready, error = null)
        }
    }

    // ─── Lifecycle ─────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        getGemmaResponseUseCase.dispose()
        streamingBuffer.clear()
    }
}