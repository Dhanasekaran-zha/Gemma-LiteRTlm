package com.chat.ui.chat

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chat.components.ChatInputBar
import com.chat.components.ChatHistoryDrawer
import com.chat.components.ChatMessageItem
import com.chat.components.TypingIndicator
import com.domain.model.ChatMessage
import com.domain.model.GenerationState
import com.domain.model.MessageRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onSettingsClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Derive scroll trigger to avoid recomposing entire screen
    val messageCount by remember { derivedStateOf { uiState.messages.size } }
    val isGenerating by remember { derivedStateOf { uiState.status == ChatStatus.Generating } }

    LaunchedEffect(messageCount, uiState.streamingText) {
        val totalItems = messageCount +
            (if (isGenerating && uiState.streamingText.isNotEmpty()) 1 else 0) +
            (if (isGenerating) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                sessions = uiState.sessions,
                onSessionClick = { sessionId ->
                    viewModel.loadSession(sessionId)
                    drawerScope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.startNewChat()
                    drawerScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Gemma-Edge") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                drawerScope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onSettingsClicked()
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                when (uiState.status) {
                    ChatStatus.LoadingModel -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Initializing AI...")
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .wrapContentHeight(
                                        align = Alignment.Bottom,
                                        unbounded = false
                                    ),
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    items = uiState.messages,
                                    key = { msg -> msg.id.takeIf { it != 0L } ?: msg.hashCode() }
                                ) { message ->
                                    ChatMessageItem(message)
                                }

                                // Streaming model response
                                if (isGenerating && uiState.streamingText.isNotEmpty()) {
                                    item(key = "streaming_response") {
                                        ChatMessageItem(
                                            ChatMessage(
                                                role = MessageRole.MODEL,
                                                text = uiState.streamingText,
                                                generationState = GenerationState.STREAMING
                                            )
                                        )
                                    }
                                }

                                // Typing indicator
                                if (isGenerating) {
                                    item(key = "typing_indicator") {
                                        TypingIndicator()
                                    }
                                }
                            }

                            // Error snackbar
                            if (uiState.status is ChatStatus.Error) {
                                Snackbar(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    action = {
                                        TextButton(onClick = { viewModel.dismissError() }) {
                                            Text("Dismiss")
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text((uiState.status as ChatStatus.Error).message)
                                }
                            }

                            ChatInputBar(
                                text = inputText,
                                onTextChange = { inputText = it },
                                onSend = {
                                    if (inputText.isNotBlank() || uiState.pendingImageUri != null) {
                                        viewModel.sendMessage(userPrompt = inputText.trim())
                                        inputText = ""
                                    }
                                },
                                onImageSelected = { uri ->
                                    viewModel.onImageSelected(uri)
                                },
                                enabled = uiState.status != ChatStatus.LoadingModel
                                    && uiState.status != ChatStatus.SavingImage,
                                selectedImageUri = uiState.pendingImageUri?.let { Uri.parse("file://$it") },
                                onCancelImage = { viewModel.clearPendingImage() }
                            )
                        }
                    }
                }
            }
        }
    }
}