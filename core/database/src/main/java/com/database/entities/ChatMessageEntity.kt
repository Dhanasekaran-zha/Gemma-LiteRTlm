package com.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting chat messages with multimodal support.
 *
 * Design decisions:
 * - Images are stored as local file URI strings, NEVER as blobs, to avoid OOM.
 * - Indices on sessionId for fast session-scoped queries.
 * - Index on createdAt for ordered retrieval.
 * - Foreign key with CASCADE ensures messages are deleted when session is deleted.
 * - messageType and generationState stored as strings for readability and migration safety.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["createdAt"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,

    val sessionId: Long,

    /** "USER", "MODEL", or "SYSTEM" */
    val role: String,

    /** Text content — empty string for image-only messages. */
    val text: String = "",

    /** Local file URI for persisted image. Null for text-only messages. */
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    /** MIME type of the image (e.g., "image/jpeg"). Null for text-only. */
    @ColumnInfo(name = "image_mime_type")
    val imageMimeType: String? = null,

    /** "TEXT", "IMAGE", "IMAGE_TEXT", "AUDIO", "AUDIO_TEXT" */
    @ColumnInfo(name = "message_type")
    val messageType: String = "TEXT",

    /** "PENDING", "STREAMING", "COMPLETE", "ERROR" */
    @ColumnInfo(name = "generation_state")
    val generationState: String = "COMPLETE",

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)