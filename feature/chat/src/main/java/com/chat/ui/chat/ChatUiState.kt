package com.chat.ui.chat

import com.domain.model.ChatMessage
import com.domain.model.ChatSession

data class ChatUiState(
        val messages: List<ChatMessage> = emptyList(),
        val status: ChatStatus = ChatStatus.Initial,
        val sessions: List<ChatSession> = emptyList(),
        val streamingText: String = "",
        val sessionId: Long? = null
)

sealed class ChatStatus {
    object Initial : ChatStatus()
    object LoadingModel : ChatStatus()
    object Ready : ChatStatus()
    object Generating : ChatStatus()
    data class Error(val message: String) : ChatStatus()
}