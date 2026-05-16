package com.chat.ui.chat

import androidx.compose.runtime.Immutable
import com.domain.model.AppError
import com.domain.model.ChatMessage
import com.domain.model.ChatSession

/**
 * Immutable UI state for the chat screen.
 * Marked @Immutable to help Compose skip unnecessary recompositions.
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val status: ChatStatus = ChatStatus.Initial,
    val sessions: List<ChatSession> = emptyList(),
    val streamingText: String = "",
    val sessionId: Long? = null,
    val pendingImageUri: String? = null,
    val pendingImageMimeType: String? = null,
    val error: AppError? = null
)

sealed class ChatStatus {
    data object Initial : ChatStatus()
    data object LoadingModel : ChatStatus()
    data object Ready : ChatStatus()
    data object Generating : ChatStatus()
    data object SavingImage : ChatStatus()
    data class Error(val message: String) : ChatStatus()
}