package com.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ─── Session Queries ───────────────────────────────────────────

    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    // ─── Message Queries ───────────────────────────────────────────

    /**
     * Streams all messages for a session, ordered by creation time.
     * Room will automatically re-emit when the table changes.
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun streamMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    /**
     * One-shot query for building conversation history (inference context).
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    /**
     * Incrementally appends streamed tokens to a model message's text.
     * Used during token streaming to avoid full entity replacement.
     */
    @Query("UPDATE chat_messages SET text = text || :appendText WHERE messageId = :messageId")
    suspend fun appendMessageText(messageId: Long, appendText: String)

    /**
     * Updates the full text of a message (used for final streaming result).
     */
    @Query("UPDATE chat_messages SET text = :text WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: Long, text: String)

    /**
     * Updates the generation state of a model message.
     */
    @Query("UPDATE chat_messages SET generation_state = :state WHERE messageId = :messageId")
    suspend fun updateGenerationState(messageId: Long, state: String)

    /**
     * Deletes all messages for a session (used alongside session deletion).
     */
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteSessionMessages(sessionId: Long)

    /**
     * Returns the count of messages in a session (useful for paging readiness checks).
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: Long): Int
}