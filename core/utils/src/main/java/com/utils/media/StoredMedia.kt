package com.utils.media

/**
 * Represents a persisted media file in app-specific storage.
 * Contains metadata needed for display and inference.
 *
 * Placed in core:utils to avoid circular dependency with domain module.
 */
data class StoredMedia(
    val localUri: String,
    val mimeType: String,
    val fileSize: Long,
    val width: Int,
    val height: Int
)
