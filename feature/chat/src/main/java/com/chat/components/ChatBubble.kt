package com.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chat.ui.chat.ChatMessage
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ChatBubble(message: ChatMessage) {

    val isUser = message.isFromUser

    val bubbleColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp // 👈 subtle "tail"
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {

        BoxWithConstraints {
            val maxBubbleWidth = maxWidth * 0.75f

            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                shape = shape,
                color = bubbleColor,
                shadowElevation = 1.dp
            ) {
                Text(
                    text = parseMarkdown(message.content),
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    val regex = "\\*\\*(.*?)\\*\\*".toRegex()

    return buildAnnotatedString {
        var lastIndex = 0

        regex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last

            // Normal text before bold
            append(text.substring(lastIndex, start))

            // Bold text
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(match.groupValues[1])
            pop()

            lastIndex = end + 1
        }

        // Remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}