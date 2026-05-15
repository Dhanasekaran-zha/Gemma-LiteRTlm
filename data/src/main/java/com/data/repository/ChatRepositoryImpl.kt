package com.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.database.dao.ChatDao
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity
import com.domain.model.ChatMessage
import com.domain.model.ChatSession
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
        @ApplicationContext private val context: Context,
        private val chatDao: ChatDao
) : ChatRepository {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSessionId: Long? = null

    @Volatile
    private var isGenerating = false

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

    override fun generateResponse(prompt: String, image: File?, sessionId: Long?): Flow<String> = callbackFlow {
        if (isGenerating) {
            close(IllegalStateException("Already generating a response"))
            return@callbackFlow
        }
        isGenerating = true

        val job = launch {
            try {
                if (sessionId != null && currentSessionId != sessionId) {
                    conversation = createConversationWithHistory(sessionId)
                    currentSessionId = sessionId
                }

                val convo = conversation ?: throw IllegalStateException("Conversation not initialized")

                val targetSessionId = sessionId ?: chatDao.insertSession(
                        ChatSessionEntity(title = prompt.take(20))
                ).also { currentSessionId = it }

                chatDao.insertMessage(
                        ChatMessageEntity(sessionId = targetSessionId, content = prompt, isFromUser = true)
                )

                val responseBuilder = StringBuilder()
                val userMessage =
                        if (image != null) {
                            Message.user(
                                    Contents.of(
                                            Content.Text(prompt),
                                            Content.ImageFile(image.absolutePath)
                                    )
                            )

                        } else {

                            Message.user(text = prompt)
                        }
                convo.sendMessageAsync(userMessage).collect { partialMessage ->
                    val text = partialMessage.toString()
                    responseBuilder.append(text)
                    trySend(text)
                }
                chatDao.insertMessage(
                        ChatMessageEntity(
                                sessionId = targetSessionId,
                                content = responseBuilder.toString(),
                                isFromUser = false
                        )
                )

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
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { ChatMessage(it.content, it.isFromUser) }
        }
    }

    override fun createNewSession() {
        conversation?.close() // Close the old one first!
        currentSessionId = null
        conversation = engine?.createConversation(ConversationConfig(channels = emptyList()))
    }

    override fun getCurrentSessionId(): Long? {
        return currentSessionId
    }

    private suspend fun createConversationWithHistory(
            sessionId: Long
    ): Conversation {
        conversation?.close()
        val history = chatDao
                .getMessagesForSessionOnce(sessionId)
                .reversed()

        val initialMessages = history.map { entity ->
            if (entity.isFromUser) Message.user(text = entity.content) else Message.model(text = entity.content)
        }

        return engine!!.createConversation(
                ConversationConfig(
                        initialMessages = initialMessages,
                )
        )
    }

}