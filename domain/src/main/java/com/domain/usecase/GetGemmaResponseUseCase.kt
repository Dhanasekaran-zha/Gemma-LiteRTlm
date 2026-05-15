package com.domain.usecase

import android.graphics.Bitmap
import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import com.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class GetGemmaResponseUseCase @Inject constructor(
        private val repository: ChatRepository
) {

    operator fun invoke(prompt: String, image: File?, sessionId: Long?): Flow<String> {
        return repository.generateResponse(prompt = prompt, image = image, sessionId = sessionId)
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
}