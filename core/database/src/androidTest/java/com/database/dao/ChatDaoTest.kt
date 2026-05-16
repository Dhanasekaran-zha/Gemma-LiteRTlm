package com.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.database.AppDatabase
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ChatDao verifying:
 * - Session CRUD
 * - Message persistence with image fields
 * - Streaming text updates
 * - Generation state transitions
 * - Cascade delete behavior
 */
@RunWith(AndroidJUnit4::class)
class ChatDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var chatDao: ChatDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        chatDao = database.chatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ─── Session Tests ─────────────────────────────────────────────

    @Test
    fun insertSession_returnsValidId() = runTest {
        val sessionId = chatDao.insertSession(
            ChatSessionEntity(title = "Test Session")
        )
        assertTrue(sessionId > 0)
    }

    @Test
    fun getAllSessions_returnsInsertedSessions() = runTest {
        chatDao.insertSession(ChatSessionEntity(title = "Session 1"))
        chatDao.insertSession(ChatSessionEntity(title = "Session 2"))

        val sessions = chatDao.getAllSessions().first()
        assertEquals(2, sessions.size)
    }

    // ─── Message Persistence Tests ─────────────────────────────────

    @Test
    fun insertTextMessage_persistsCorrectly() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "USER",
                text = "Hello",
                messageType = "TEXT",
                generationState = "COMPLETE"
            )
        )

        val messages = chatDao.streamMessages(sessionId).first()
        assertEquals(1, messages.size)
        assertEquals("Hello", messages[0].text)
        assertEquals("USER", messages[0].role)
        assertNull(messages[0].imageUri)
    }

    @Test
    fun insertImageTextMessage_persistsImageFields() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "USER",
                text = "What is this?",
                imageUri = "/data/app/chat_media/img_123.jpg",
                imageMimeType = "image/jpeg",
                messageType = "IMAGE_TEXT",
                generationState = "COMPLETE"
            )
        )

        val messages = chatDao.streamMessages(sessionId).first()
        assertEquals(1, messages.size)
        assertEquals("What is this?", messages[0].text)
        assertEquals("/data/app/chat_media/img_123.jpg", messages[0].imageUri)
        assertEquals("image/jpeg", messages[0].imageMimeType)
        assertEquals("IMAGE_TEXT", messages[0].messageType)
    }

    // ─── Streaming Text Update Tests ───────────────────────────────

    @Test
    fun updateMessageText_updatesTextCorrectly() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        val messageId = chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "MODEL",
                text = "",
                generationState = "STREAMING"
            )
        )

        chatDao.updateMessageText(messageId, "Hello, I am Gemma!")

        val messages = chatDao.streamMessages(sessionId).first()
        assertEquals("Hello, I am Gemma!", messages[0].text)
    }

    @Test
    fun appendMessageText_appendsCorrectly() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        val messageId = chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "MODEL",
                text = "Hello",
                generationState = "STREAMING"
            )
        )

        chatDao.appendMessageText(messageId, " World")

        val messages = chatDao.streamMessages(sessionId).first()
        assertEquals("Hello World", messages[0].text)
    }

    // ─── Generation State Tests ────────────────────────────────────

    @Test
    fun updateGenerationState_updatesCorrectly() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        val messageId = chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "MODEL",
                text = "",
                generationState = "PENDING"
            )
        )

        chatDao.updateGenerationState(messageId, "STREAMING")
        var messages = chatDao.streamMessages(sessionId).first()
        assertEquals("STREAMING", messages[0].generationState)

        chatDao.updateGenerationState(messageId, "COMPLETE")
        messages = chatDao.streamMessages(sessionId).first()
        assertEquals("COMPLETE", messages[0].generationState)
    }

    // ─── Cascade Delete Tests ──────────────────────────────────────

    @Test
    fun deleteSession_cascadeDeletesMessages() = runTest {
        val sessionId = chatDao.insertSession(ChatSessionEntity(title = "Test"))
        chatDao.insertMessage(
            ChatMessageEntity(sessionId = sessionId, role = "USER", text = "Hello")
        )
        chatDao.insertMessage(
            ChatMessageEntity(sessionId = sessionId, role = "MODEL", text = "Hi!")
        )

        assertEquals(2, chatDao.getMessageCount(sessionId))

        chatDao.deleteSessionById(sessionId)

        assertEquals(0, chatDao.getMessageCount(sessionId))
    }

    @Test
    fun deleteSessionMessages_clearsOnlyTargetSession() = runTest {
        val session1 = chatDao.insertSession(ChatSessionEntity(title = "Session 1"))
        val session2 = chatDao.insertSession(ChatSessionEntity(title = "Session 2"))

        chatDao.insertMessage(
            ChatMessageEntity(sessionId = session1, role = "USER", text = "S1 msg")
        )
        chatDao.insertMessage(
            ChatMessageEntity(sessionId = session2, role = "USER", text = "S2 msg")
        )

        chatDao.deleteSessionMessages(session1)

        assertEquals(0, chatDao.getMessageCount(session1))
        assertEquals(1, chatDao.getMessageCount(session2))
    }
}
