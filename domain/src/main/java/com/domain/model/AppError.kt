package com.domain.model

/**
 * Production-grade sealed error hierarchy for the app.
 * Each subclass carries a human-readable message and optional cause.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /** Model file not found or failed to load into LiteRT engine. */
    data class ModelLoadError(
        override val message: String = "Failed to load AI model",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Out of memory during inference or image processing. */
    data class OutOfMemoryError(
        override val message: String = "Device ran out of memory",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Disk write failure, storage full, or permission denied. */
    data class StorageError(
        override val message: String = "Storage operation failed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Image could not be decoded (corrupt, unsupported format). */
    data class ImageDecodeError(
        override val message: String = "Failed to decode image",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** User or system cancelled an in-progress generation. */
    data class CancelledGeneration(
        override val message: String = "Generation was cancelled",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Inference took longer than the allowed timeout. */
    data class InferenceTimeout(
        override val message: String = "Inference timed out",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Generic inference failure not covered by other types. */
    data class InferenceError(
        override val message: String = "Inference failed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Catch-all for unexpected errors. */
    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
