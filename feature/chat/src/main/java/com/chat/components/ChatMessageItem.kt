package com.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.domain.model.ChatMessage
import com.domain.model.GenerationState
import com.domain.model.MessageType
import java.io.File

/**
 * Multimodal chat message item supporting:
 * - Text-only messages
 * - Image-only messages
 * - Combined image + text messages
 * - Streaming response rendering
 * - Error/retry states
 * - Loading shimmer for images
 *
 * Uses Coil SubcomposeAsyncImage for efficient image loading
 * with built-in loading/error states.
 */
@Composable
fun ChatMessageItem(message: ChatMessage) {
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
            bottomEnd = 4.dp
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
            val maxBubbleWidth = maxWidth * 0.78f

            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                shape = shape,
                color = bubbleColor,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (message.hasImage) 4.dp else 14.dp,
                        vertical = if (message.hasImage) 4.dp else 10.dp
                    )
                ) {
                    // ─── Image Content ──────────────────────────────
                    if (message.hasImage) {
                        MessageImage(
                            imageUri = message.imageUri!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )

                        if (message.hasText) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // ─── Text Content ───────────────────────────────
                    if (message.hasText) {
                        Text(
                            text = parseMarkdown(message.text),
                            modifier = Modifier.padding(
                                horizontal = if (message.hasImage) 10.dp else 0.dp,
                                vertical = if (message.hasImage) 6.dp else 0.dp
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    }

                    // ─── Generation State Indicator ─────────────────
                    when (message.generationState) {
                        GenerationState.STREAMING -> {
                            // Handled by streaming text in parent
                        }
                        GenerationState.ERROR -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.BrokenImage,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Failed to generate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        else -> { /* PENDING, COMPLETE — no indicator */ }
                    }
                }
            }
        }
    }
}

/**
 * Coil-powered async image loader with loading shimmer and error state.
 * Uses file:// URIs for local persistence compatibility.
 */
@Composable
private fun MessageImage(
    imageUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(File(imageUri))
            .crossfade(300)
            .size(720) // Thumbnail-size decode for memory efficiency
            .build()
    }

    SubcomposeAsyncImage(
        model = imageModel,
        contentDescription = "Attached image",
        modifier = modifier
            .height(200.dp),
        contentScale = ContentScale.Crop,
        loading = {
            // Shimmer placeholder
            ImageShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        },
        error = {
            // Error state with broken image icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = "Failed to load image",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "Image unavailable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    )
}

/**
 * Animated shimmer placeholder for loading images.
 */
@Composable
private fun ImageShimmer(modifier: Modifier = Modifier) {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush)
    )
}

/**
 * Simple markdown bold parser for **text** patterns.
 */
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
