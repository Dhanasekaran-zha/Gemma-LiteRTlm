package com.chat.components

import androidx.compose.runtime.Composable
import com.domain.model.ChatMessage

/**
 * Legacy ChatBubble composable — delegates to the new multimodal ChatMessageItem.
 * Preserved for backward compatibility with any external references.
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    ChatMessageItem(message)
}