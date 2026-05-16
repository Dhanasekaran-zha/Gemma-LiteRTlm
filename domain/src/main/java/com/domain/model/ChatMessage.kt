package com.domain.model

/**
 * Core domain model representing a chat message.
 *
 * Supports text, image, and combined image+text messages.
 * Audio fields are stubbed for future extensibility.
 *
 * @property id Unique message identifier (0 = not yet persisted).
 * @property sessionId The chat session this message belongs to.
 * @property role The sender: USER, MODEL, or SYSTEM.
 * @property text The text content of the message (nullable for image-only).
 * @property imageUri Local file URI for the persisted image (nullable).
 * @property imageMimeType MIME type of the image, e.g. "image/jpeg" (nullable).
 * @property messageType Classification of the content modality.
 * @property generationState Lifecycle state for model-generated messages.
 * @property timestamp Creation time in epoch milliseconds.
 *
 * Future audio fields (not yet implemented):
 * - audioUri: local file path for audio
 * - audioWaveform: amplitude metadata for waveform rendering
 * - transcriptionText: speech-to-text output
 */
data class ChatMessage(
    val id: Long = 0,
    val sessionId: Long = 0,
    val role: MessageRole = MessageRole.USER,
    val text: String = "",
    val imageUri: String? = null,
    val imageMimeType: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val generationState: GenerationState = GenerationState.COMPLETE,
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Convenience check matching the old isFromUser pattern. */
    val isFromUser: Boolean get() = role == MessageRole.USER

    /** Whether this message carries image content. */
    val hasImage: Boolean get() = imageUri != null

    /** Whether this message carries text content. */
    val hasText: Boolean get() = text.isNotBlank()
}