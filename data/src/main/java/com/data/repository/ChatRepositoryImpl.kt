package com.data.repository

import android.content.Context
import com.database.dao.ChatDao
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity
import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import com.domain.repository.ChatRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
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

    // Prevent multiple parallel generations (important for LiteRTLM)
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

    override fun generateResponse(prompt: String, sessionId: Long?): Flow<String> = callbackFlow {

        if (isGenerating) {
            close(IllegalStateException("Already generating a response"))
            return@callbackFlow
        }

        val convo = conversation ?: run {
            close(IllegalStateException("Conversation not initialized"))
            return@callbackFlow
        }

        isGenerating = true

        val job = launch {
            try {
                currentSessionId = sessionId ?: chatDao.insertSession(
                        ChatSessionEntity(title = prompt.take(20))
                )
                chatDao.insertMessage(ChatMessageEntity(sessionId = currentSessionId!!, content = prompt, isFromUser = true))

                val responseMessage = StringBuilder()

                convo.sendMessageAsync(prompt)
                        .collect { message ->
                            responseMessage.append(message.toString())
                            trySend(message.toString())
                        }

                chatDao.insertMessage(
                        ChatMessageEntity(
                                sessionId = currentSessionId!!,
                                content = responseMessage.toString(),
                                isFromUser = false
                        )
                )

                isGenerating = false
                close()

            } catch (e: Exception) {
                isGenerating = false
                close(e)
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

}