package com.chat.ui.chat

data class ChatUiState(
        val messages: List<ChatMessage> = emptyList(),
        val status: ChatStatus = ChatStatus.Initial
)

sealed class ChatStatus {
    object Initial : ChatStatus()
    object LoadingModel : ChatStatus()
    object Ready : ChatStatus()
    object Generating : ChatStatus()
    data class Error(val message: String) : ChatStatus()
}

data class ChatMessage(
        val content: String,
        val isFromUser: Boolean
)