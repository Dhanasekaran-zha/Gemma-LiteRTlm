package com.data.repository

import android.content.Context
import com.database.dao.ChatDao
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity
import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import com.domain.model.GenerationState
import com.domain.model.MessageRole
import com.domain.model.MessageType
import com.domain.repository.ChatRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Production implementation of [ChatRepository].
 *
 * Manages the LiteRT Engine lifecycle, Room persistence,
 * and the token streaming pipeline with incremental DB writes.
 *
 * Key design decisions:
 * - Engine is lazy-initialized as a singleton; auto-released on low memory.
 * - Token streaming uses callbackFlow with batching (~50ms debounce via StringBuilder + batch writes).
 * - Image messages persist file paths only — never bitmap blobs.
 * - DB writes are on Dispatchers.IO; inference on Dispatchers.Default.
 * - distinctUntilChanged on message flows to reduce recompositions.
 */
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao
) : ChatRepository {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSessionId: Long? = null

    @Volatile
    private var isGenerating = false

    // ─── Engine Lifecycle ──────────────────────────────────────────

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(
                context.getExternalFilesDir(null),
                "gemma-4-E2B-it.litertlm"
            )

            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Model file not found: ${modelFile.absolutePath}")
                )
            }

            val engineConfig = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                visionBackend = Backend.GPU(),
                cacheDir = context.cacheDir.absolutePath,
            )

            val conversationConfig = ConversationConfig(channels = emptyList())

            engine = Engine(engineConfig).also { it.initialize() }
            conversation = engine?.createConversation(conversationConfig = conversationConfig)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Inference ─────────────────────────────────────────────────

    override fun generateResponse(
        prompt: String,
        imageFilePath: String?,
        sessionId: Long?
    ): Flow<String> = callbackFlow {
        if (isGenerating) {
            close(IllegalStateException("Already generating a response"))
            return@callbackFlow
        }
        isGenerating = true

        val job = launch {
            try {
                // Restore conversation context if switching sessions
                if (sessionId != null && currentSessionId != sessionId) {
                    conversation = createConversationWithHistory(sessionId)
                    currentSessionId = sessionId
                }

                val convo = conversation
                    ?: throw IllegalStateException("Conversation not initialized")

                // Create or reuse session
                val targetSessionId = sessionId ?: chatDao.insertSession(
                    ChatSessionEntity(title = prompt.take(30))
                ).also { currentSessionId = it }

                // Determine message type
                val messageType = when {
                    imageFilePath != null && prompt.isNotBlank() -> MessageType.IMAGE_TEXT.name
                    imageFilePath != null -> MessageType.IMAGE.name
                    else -> MessageType.TEXT.name
                }

                // Persist user message
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = targetSessionId,
                        role = MessageRole.USER.name,
                        text = prompt,
                        imageUri = imageFilePath,
                        imageMimeType = if (imageFilePath != null) "image/jpeg" else null,
                        messageType = messageType,
                        generationState = GenerationState.COMPLETE.name
                    )
                )

                // Create model message placeholder for streaming
                val modelMessageId = chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = targetSessionId,
                        role = MessageRole.MODEL.name,
                        text = "",
                        messageType = MessageType.TEXT.name,
                        generationState = GenerationState.STREAMING.name
                    )
                )

                // Build LiteRT message
                val userMessage = if (imageFilePath != null) {
                    Message.user(
                        Contents.of(
                            Content.Text(prompt),
                            Content.ImageFile(imageFilePath)
                        )
                    )
                } else {
                    Message.user(text = prompt)
                }

                // Stream inference tokens with batching
                val responseBuilder = StringBuilder()
                var lastFlushTime = System.currentTimeMillis()
                val batchBuffer = StringBuilder()

                convo.sendMessageAsync(userMessage).collect { partialMessage ->
                    val text = partialMessage.toString()
                    responseBuilder.append(text)
                    batchBuffer.append(text)

                    // Batch DB writes every ~50ms to reduce write pressure
                    val now = System.currentTimeMillis()
                    if (now - lastFlushTime >= 50) {
                        val batch = batchBuffer.toString()
                        batchBuffer.clear()
                        lastFlushTime = now

                        withContext(Dispatchers.IO) {
                            chatDao.updateMessageText(
                                modelMessageId,
                                responseBuilder.toString()
                            )
                        }
                    }

                    // Emit token for immediate UI streaming
                    trySend(text)
                }

                // Final flush: write complete response
                withContext(Dispatchers.IO) {
                    chatDao.updateMessageText(
                        modelMessageId,
                        responseBuilder.toString()
                    )
                    chatDao.updateGenerationState(
                        modelMessageId,
                        GenerationState.COMPLETE.name
                    )
                }

            } catch (e: Exception) {
                close(e)
            } finally {
                isGenerating = false
                close()
            }
        }

        awaitClose {
            job.cancel()
            isGenerating = false
        }
    }.flowOn(Dispatchers.Default)

    // ─── Resource Management ───────────────────────────────────────

    override fun release() {
        try {
            conversation = null
            engine?.close()
        } catch (_: Exception) {
        } finally {
            engine = null
            isGenerating = false
        }
    }

    // ─── Session Operations ────────────────────────────────────────

    override fun getAllSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllSessions().map { entities ->
            entities.map {
                ChatSession(
                    sessionId = it.sessionId,
                    title = it.title,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.streamMessages(sessionId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .distinctUntilChanged()
    }

    override fun createNewSession() {
        conversation?.close()
        currentSessionId = null
        conversation = engine?.createConversation(ConversationConfig(channels = emptyList()))
    }

    override fun getCurrentSessionId(): Long? {
        return currentSessionId
    }

    // ─── Message Persistence (called from ViewModel layer) ─────────

    override suspend fun persistUserMessage(
        sessionId: Long,
        text: String,
        imageUri: String?,
        imageMimeType: String?
    ): Long {
        val messageType = when {
            imageUri != null && text.isNotBlank() -> MessageType.IMAGE_TEXT.name
            imageUri != null -> MessageType.IMAGE.name
            else -> MessageType.TEXT.name
        }
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = MessageRole.USER.name,
                text = text,
                imageUri = imageUri,
                imageMimeType = imageMimeType,
                messageType = messageType,
                generationState = GenerationState.COMPLETE.name
            )
        )
    }

    override suspend fun persistModelMessagePlaceholder(sessionId: Long): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = MessageRole.MODEL.name,
                text = "",
                messageType = MessageType.TEXT.name,
                generationState = GenerationState.PENDING.name
            )
        )
    }

    override suspend fun updateModelMessage(
        messageId: Long,
        text: String,
        generationState: String
    ) {
        chatDao.updateMessageText(messageId, text)
        chatDao.updateGenerationState(messageId, generationState)
    }

    // ─── Private Helpers ───────────────────────────────────────────

    private suspend fun createConversationWithHistory(
        sessionId: Long
    ): Conversation {
        conversation?.close()
        val history = chatDao
            .getMessagesForSessionOnce(sessionId)
            .reversed()

        val initialMessages = history.map { entity ->
            when (entity.role) {
                MessageRole.USER.name -> {
                    if (entity.imageUri != null) {
                        Message.user(
                            Contents.of(
                                Content.Text(entity.text),
                                Content.ImageFile(entity.imageUri!!)
                            )
                        )
                    } else {
                        Message.user(text = entity.text)
                    }
                }
                else -> Message.model(text = entity.text)
            }
        }

        return engine!!.createConversation(
            ConversationConfig(
                initialMessages = initialMessages,
            )
        )
    }

    /**
     * Maps a Room entity to the domain model.
     * Parsing enums safely with fallback defaults.
     */
    private fun ChatMessageEntity.toDomainModel(): ChatMessage {
        return ChatMessage(
            id = messageId,
            sessionId = sessionId,
            role = try { MessageRole.valueOf(role) } catch (_: Exception) { MessageRole.USER },
            text = text,
            imageUri = imageUri,
            imageMimeType = imageMimeType,
            messageType = try { MessageType.valueOf(messageType) } catch (_: Exception) { MessageType.TEXT },
            generationState = try { GenerationState.valueOf(generationState) } catch (_: Exception) { GenerationState.COMPLETE },
            timestamp = createdAt
        )
    }
}