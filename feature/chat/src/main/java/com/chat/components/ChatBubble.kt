package com.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.chat.ui.chat.ChatMessage

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isFromUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalAlignment = alignment
    ) {
        Surface(
                color = containerColor,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
        ) {
            Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}