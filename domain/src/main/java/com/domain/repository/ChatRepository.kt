package com.domain.repository

import android.graphics.Bitmap
import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    suspend fun initialize(): Result<Unit>
    fun generateResponse(prompt: String, image: File?, sessionId: Long?): Flow<String>
    fun release()
    fun getAllSessions(): Flow<List<ChatSession>>
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>
    fun createNewSession()
    fun getCurrentSessionId(): Long?

}