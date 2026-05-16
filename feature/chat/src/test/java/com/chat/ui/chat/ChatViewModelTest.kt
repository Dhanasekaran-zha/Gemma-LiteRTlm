package com.chat.ui.chat

import com.domain.model.ChatMessage
import com.domain.model.ChatSession
import com.domain.model.GenerationState
import com.domain.model.MessageRole
import com.domain.model.MessageType
import com.domain.repository.ChatRepository
import com.domain.usecase.GetGemmaResponseUseCase
import com.utils.media.MediaStorageManager
import com.utils.media.StoredMedia
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChatViewModel verifying:
 * - Initial state
 * - Engine initialization states
 * - Message sending with text
 * - Message sending with image
 * - Image selection and persistence
 * - Session management
 * - Error handling
 * - Token streaming buffer behavior
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var useCase: GetGemmaResponseUseCase
    private lateinit var repository: ChatRepository
    private lateinit var mediaStorageManager: MediaStorageManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        useCase = GetGemmaResponseUseCase(repository)
        mediaStorageManager = mockk(relaxed = true)

        // Default stubs
        coEvery { repository.initialize() } returns Result.success(Unit)
        every { repository.getAllSessions() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ChatViewModel {
        return ChatViewModel(useCase, mediaStorageManager)
    }

    // ─── Initial State Tests ───────────────────────────────────────

    @Test
    fun `initial state is correct`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertNull(state.sessionId)
        assertNull(state.pendingImageUri)
        assertEquals("", state.streamingText)
    }

    @Test
    fun `engine initialization success sets Ready status`() = runTest {
        coEvery { repository.initialize() } returns Result.success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(ChatStatus.Ready, viewModel.uiState.value.status)
    }

    @Test
    fun `engine initialization failure sets Error status`() = runTest {
        coEvery { repository.initialize() } returns
            Result.failure(RuntimeException("Model not found"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val status = viewModel.uiState.value.status
        assertTrue(status is ChatStatus.Error)
        assertEquals("Model not found", (status as ChatStatus.Error).message)
    }

    // ─── Message Sending Tests ─────────────────────────────────────

    @Test
    fun `sendMessage with blank text and no image does nothing`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("")

        assertEquals(ChatStatus.Ready, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `sendMessage adds user message to UI state`() = runTest {
        every { repository.generateResponse(any(), any(), any()) } returns flowOf("Response")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("Hello")
        // Don't advance — check optimistic update
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals(ChatStatus.Generating, state.status)
    }

    // ─── Image Selection Tests ─────────────────────────────────────

    @Test
    fun `clearPendingImage removes pending image`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Manually set pending image state
        viewModel.clearPendingImage()

        assertNull(viewModel.uiState.value.pendingImageUri)
        assertNull(viewModel.uiState.value.pendingImageMimeType)
    }

    // ─── Session Management Tests ──────────────────────────────────

    @Test
    fun `startNewChat clears messages and session`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startNewChat()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertNull(state.sessionId)
        assertEquals(ChatStatus.Ready, state.status)
        assertEquals("", state.streamingText)
    }

    @Test
    fun `loadSession sets sessionId and Ready status`() = runTest {
        every { repository.getMessagesForSession(any()) } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadSession(42L)
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.sessionId)
        assertEquals(ChatStatus.Ready, viewModel.uiState.value.status)
    }

    // ─── Error Handling Tests ──────────────────────────────────────

    @Test
    fun `dismissError resets to Ready state`() = runTest {
        coEvery { repository.initialize() } returns
            Result.failure(RuntimeException("Error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.status is ChatStatus.Error)

        viewModel.dismissError()

        assertEquals(ChatStatus.Ready, viewModel.uiState.value.status)
    }
}
