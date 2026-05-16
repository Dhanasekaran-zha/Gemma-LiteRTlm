package com.domain.model

/**
 * Classifies the content type of a message.
 * Extensible for future modalities (audio, video).
 */
enum class MessageType {
    TEXT,
    IMAGE,
    IMAGE_TEXT,
    AUDIO,
    AUDIO_TEXT
}
