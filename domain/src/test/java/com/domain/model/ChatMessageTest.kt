package com.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ChatMessage domain model verifying:
 * - Default values
 * - Computed properties (isFromUser, hasImage, hasText)
 * - Message type variations
 * - Immutability via copy
 */
class ChatMessageTest {

    @Test
    fun `default ChatMessage has correct defaults`() {
        val msg = ChatMessage()

        assertEquals(0L, msg.id)
        assertEquals(0L, msg.sessionId)
        assertEquals(MessageRole.USER, msg.role)
        assertEquals("", msg.text)
        assertNull(msg.imageUri)
        assertNull(msg.imageMimeType)
        assertEquals(MessageType.TEXT, msg.messageType)
        assertEquals(GenerationState.COMPLETE, msg.generationState)
    }

    @Test
    fun `isFromUser returns true for USER role`() {
        val userMsg = ChatMessage(role = MessageRole.USER)
        assertTrue(userMsg.isFromUser)
    }

    @Test
    fun `isFromUser returns false for MODEL role`() {
        val modelMsg = ChatMessage(role = MessageRole.MODEL)
        assertFalse(modelMsg.isFromUser)
    }

    @Test
    fun `hasImage returns true when imageUri is set`() {
        val msg = ChatMessage(imageUri = "/path/to/image.jpg")
        assertTrue(msg.hasImage)
    }

    @Test
    fun `hasImage returns false when imageUri is null`() {
        val msg = ChatMessage(imageUri = null)
        assertFalse(msg.hasImage)
    }

    @Test
    fun `hasText returns true for non-blank text`() {
        val msg = ChatMessage(text = "Hello")
        assertTrue(msg.hasText)
    }

    @Test
    fun `hasText returns false for blank text`() {
        val blankMsg = ChatMessage(text = "   ")
        assertFalse(blankMsg.hasText)

        val emptyMsg = ChatMessage(text = "")
        assertFalse(emptyMsg.hasText)
    }

    @Test
    fun `image text message has both image and text`() {
        val msg = ChatMessage(
            text = "What is this?",
            imageUri = "/path/image.jpg",
            imageMimeType = "image/jpeg",
            messageType = MessageType.IMAGE_TEXT
        )

        assertTrue(msg.hasImage)
        assertTrue(msg.hasText)
        assertEquals(MessageType.IMAGE_TEXT, msg.messageType)
    }

    @Test
    fun `copy creates independent instance with overrides`() {
        val original = ChatMessage(
            id = 1,
            sessionId = 10,
            role = MessageRole.USER,
            text = "Hello"
        )

        val copy = original.copy(
            id = 2,
            text = "World",
            generationState = GenerationState.STREAMING
        )

        assertEquals(2L, copy.id)
        assertEquals(10L, copy.sessionId) // preserved
        assertEquals("World", copy.text)
        assertEquals(GenerationState.STREAMING, copy.generationState)
    }
}

/**
 * Tests for GenerationState enum.
 */
class GenerationStateTest {

    @Test
    fun `all generation states exist`() {
        val states = GenerationState.entries
        assertEquals(4, states.size)
        assertTrue(states.contains(GenerationState.PENDING))
        assertTrue(states.contains(GenerationState.STREAMING))
        assertTrue(states.contains(GenerationState.COMPLETE))
        assertTrue(states.contains(GenerationState.ERROR))
    }

    @Test
    fun `valueOf parses correctly`() {
        assertEquals(GenerationState.STREAMING, GenerationState.valueOf("STREAMING"))
        assertEquals(GenerationState.COMPLETE, GenerationState.valueOf("COMPLETE"))
    }
}

/**
 * Tests for MessageType enum.
 */
class MessageTypeTest {

    @Test
    fun `all message types exist`() {
        val types = MessageType.entries
        assertEquals(5, types.size)
        assertTrue(types.contains(MessageType.TEXT))
        assertTrue(types.contains(MessageType.IMAGE))
        assertTrue(types.contains(MessageType.IMAGE_TEXT))
        assertTrue(types.contains(MessageType.AUDIO))
        assertTrue(types.contains(MessageType.AUDIO_TEXT))
    }
}

/**
 * Tests for MessageRole enum.
 */
class MessageRoleTest {

    @Test
    fun `all roles exist`() {
        val roles = MessageRole.entries
        assertEquals(3, roles.size)
        assertTrue(roles.contains(MessageRole.USER))
        assertTrue(roles.contains(MessageRole.MODEL))
        assertTrue(roles.contains(MessageRole.SYSTEM))
    }
}

/**
 * Tests for AppError sealed hierarchy.
 */
class AppErrorTest {

    @Test
    fun `ModelLoadError has correct default message`() {
        val error = AppError.ModelLoadError()
        assertEquals("Failed to load AI model", error.message)
        assertNull(error.cause)
    }

    @Test
    fun `errors carry custom messages and causes`() {
        val cause = RuntimeException("OOM")
        val error = AppError.OutOfMemoryError(
            message = "Custom OOM message",
            cause = cause
        )
        assertEquals("Custom OOM message", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `all error types are distinct`() {
        val errors = listOf(
            AppError.ModelLoadError(),
            AppError.OutOfMemoryError(),
            AppError.StorageError(),
            AppError.ImageDecodeError(),
            AppError.CancelledGeneration(),
            AppError.InferenceTimeout(),
            AppError.InferenceError(),
            AppError.Unknown()
        )

        assertEquals(8, errors.size)
        assertEquals(8, errors.map { it::class }.toSet().size)
    }
}
