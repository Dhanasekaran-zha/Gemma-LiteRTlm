package com.domain.model

/**
 * Represents the sender role for a chat message.
 * Maps cleanly to LiteRT conversation roles.
 */
enum class MessageRole {
    USER,
    MODEL,
    SYSTEM
}
