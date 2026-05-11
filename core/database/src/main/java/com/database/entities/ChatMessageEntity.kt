package com.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
        tableName = "chat_messages",
        foreignKeys = [
            ForeignKey(
                    entity = ChatSessionEntity::class,
                    parentColumns = ["sessionId"],
                    childColumns = ["sessionId"],
                    onDelete = ForeignKey.CASCADE
            )
        ]
)
data class ChatMessageEntity(
        @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
        val sessionId: Long,
        val content: String,
        val isFromUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
)