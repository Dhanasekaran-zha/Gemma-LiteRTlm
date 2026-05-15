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
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chat.components.ChatBubble
import com.chat.components.ChatHistoryDrawer
import com.chat.components.ChatInputBar
import com.chat.components.TypingIndicator
import com.domain.model.ChatMessage
import com.utils.image.ImageUtils.toCompressedFile
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
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
                                            .weight(1f)          // takes remaining space
                                            .wrapContentHeight(  // but aligns content to bottom like WhatsApp
                                                    align = Alignment.Bottom,
                                                    unbounded = false
                                            ),
                                    state = listState,
                                    contentPadding = PaddingValues(
                                            start = 12.dp,
                                            end = 12.dp,
                                            top = 8.dp,
                                            bottom = 8.dp   // 👈 no need for 90.dp anymore
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(uiState.messages) { message ->
                                    ChatBubble(message)
                                }

                                if (uiState.status == ChatStatus.Generating && uiState.streamingText.isNotEmpty()) {
                                    item {
                                        ChatBubble(
                                                ChatMessage(
                                                        content = uiState.streamingText,
                                                        isFromUser = false
                                                )
                                        )
                                    }
                                }

                                if (uiState.status == ChatStatus.Generating) {
                                    item { TypingIndicator() }
                                }
                            }

                            ChatInputBar(
                                    text = inputText,
                                    onTextChange = { inputText = it },
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            viewModel.sendMessage(userPrompt = inputText.trim(), image = selectedImageUri?.toCompressedFile(context))
                                            inputText = ""
                                            selectedImageUri = null
                                        }
                                    },
                                    onImageSelected = { uri ->
                                        selectedImageUri = uri
                                    },
                                    enabled = uiState.status != ChatStatus.LoadingModel,
                                    selectedImageUri = selectedImageUri,
                                    onCancelImage = { selectedImageUri = null }
                            )
                        }
                    }
                }
            }
        }
    }
}