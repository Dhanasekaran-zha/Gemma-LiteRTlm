package com.domain.repository

import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for chat operations.
 *
 * Clean architecture boundary — the domain defines what operations
 * are available; the data layer decides how to implement them.
 */
interface ChatRepository {

    /** Initialize the LiteRT inference engine. */
    suspend fun initialize(): Result<Unit>

    /**
     * Sends a user message and streams the model response tokens.
     *
     * @param prompt The user's text input.
     * @param imageFilePath Optional local file path of a persisted image for multimodal inference.
     * @param sessionId The session to send the message in (null = create new session).
     * @return Flow emitting partial response tokens as they are generated.
     */
    fun generateResponse(prompt: String, imageFilePath: String?, sessionId: Long?): Flow<String>

    /** Release the inference engine and free resources. */
    fun release()

    /** Stream all sessions, ordered by most recent first. */
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * Stream all messages for a given session.
     * The flow re-emits whenever the underlying data changes (Room observation).
     */
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    /** Create a fresh conversation in the inference engine. */
    fun createNewSession()

    /** Returns the current active session ID, or null if none. */
    fun getCurrentSessionId(): Long?

    /**
     * Persists a user message with optional image data to Room.
     * Returns the inserted message ID.
     */
    suspend fun persistUserMessage(
        sessionId: Long,
        text: String,
        imageUri: String? = null,
        imageMimeType: String? = null
    ): Long

    /**
     * Persists a model message placeholder and returns its ID.
     * Used to create the streaming target row before inference starts.
     */
    suspend fun persistModelMessagePlaceholder(sessionId: Long): Long

    /**
     * Updates the text and generation state of a model message.
     */
    suspend fun updateModelMessage(
        messageId: Long,
        text: String,
        generationState: String
    )
}