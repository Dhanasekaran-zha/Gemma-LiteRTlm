package com.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun initialize(): Result<Unit>
    fun generateResponse(prompt: String): Flow<String>
    fun release()
}