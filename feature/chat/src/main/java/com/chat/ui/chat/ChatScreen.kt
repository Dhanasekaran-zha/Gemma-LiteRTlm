package com.chat.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.chat.components.ChatBubble
import com.chat.components.ChatInputBar
import com.chat.components.TypingIndicator

@Composable
fun ChatScreen(
        viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold { paddingValues ->

        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
        ) {

            LazyColumn(
                    modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(
                        items = uiState.messages,
                ) { message ->
                    ChatBubble(message)
                }

                if (uiState.status == ChatStatus.Generating) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = uiState.status != ChatStatus.LoadingModel
            )
        }
    }
}