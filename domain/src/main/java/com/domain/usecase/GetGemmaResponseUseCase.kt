package com.domain.usecase

import com.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGemmaResponseUseCase @Inject constructor(
        private val repository: ChatRepository
) {
    /**
     * Executes the LLM inference for a given prompt.
     * We return a Flow<String> to support real-time token streaming.
     */
    operator fun invoke(prompt: String): Flow<String> {
        return repository.generateResponse(prompt)
    }

    /**
     * Pre-warms the engine. This can be called when the Chat screen
     * is first opened to reduce the "first-token" latency.
     */
    suspend fun initializeEngine(): Result<Unit> {
        return repository.initialize()
    }

    /**
     * Explicitly releases the engine resources.
     */
    fun dispose() {
        repository.release()
    }
}