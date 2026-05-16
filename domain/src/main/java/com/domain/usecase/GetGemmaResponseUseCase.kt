package com.domain.usecase

import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import com.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case orchestrating chat interactions with the Gemma LiteRT model.
 *
 * Acts as the single entry point for the presentation layer to:
 * - Initialize the engine
 * - Send messages (text + optional image)
 * - Observe sessions and messages
 * - Manage session lifecycle
 */
class GetGemmaResponseUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * Sends a user prompt (with optional image) and streams model response tokens.
     *
     * @param prompt The user's text input.
     * @param imageFilePath Local file path of a persisted image, or null for text-only.
     * @param sessionId Active session ID, or null to create a new session.
     */
    operator fun invoke(
        prompt: String,
        imageFilePath: String?,
        sessionId: Long?
    ): Flow<String> {
        return repository.generateResponse(
            prompt = prompt,
            imageFilePath = imageFilePath,
            sessionId = sessionId
        )
    }

    suspend fun initializeEngine(): Result<Unit> {
        return repository.initialize()
    }

    fun dispose() {
        repository.release()
    }

    fun getAllSessions(): Flow<List<ChatSession>> {
        return repository.getAllSessions()
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return repository.getMessagesForSession(sessionId)
    }

    fun createNewSession() {
        repository.createNewSession()
    }

    fun getCurrentSessionId(): Long? {
        return repository.getCurrentSessionId()
    }

    /**
     * Persists a user message to the database.
     * Called before inference so the message is immediately visible in chat history.
     */
    suspend fun persistUserMessage(
        sessionId: Long,
        text: String,
        imageUri: String? = null,
        imageMimeType: String? = null
    ): Long {
        return repository.persistUserMessage(sessionId, text, imageUri, imageMimeType)
    }

    /**
     * Creates a model message placeholder row for streaming tokens into.
     */
    suspend fun persistModelMessagePlaceholder(sessionId: Long): Long {
        return repository.persistModelMessagePlaceholder(sessionId)
    }

    /**
     * Updates a model message with final text and generation state.
     */
    suspend fun updateModelMessage(
        messageId: Long,
        text: String,
        generationState: String
    ) {
        repository.updateModelMessage(messageId, text, generationState)
    }
}