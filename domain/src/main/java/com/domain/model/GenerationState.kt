package com.domain.model

/**
 * Tracks the lifecycle state of a model-generated message.
 * Used to differentiate in-progress streaming from finalized responses.
 */
enum class GenerationState {
    PENDING,
    STREAMING,
    COMPLETE,
    ERROR
}
