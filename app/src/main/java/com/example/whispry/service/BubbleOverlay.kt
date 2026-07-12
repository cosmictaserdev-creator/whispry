package com.example.whispry.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modern Pill-style Overlay that slides up from the bottom with spring physics.
 */
@Composable
fun BubbleOverlay(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String,
    cancelArming: Boolean = false,
    onRetry: () -> Unit = {},
    onCancel: () -> Unit = {},
    onStop: () -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    val visibleState = remember { MutableTransitionState(false) }
    
    // Sync visibleState with state
    LaunchedEffect(state) {
        visibleState.targetState = state !is BubbleState.Idle
    }

    // Modern glass backdrop
    val backdrop = rememberLayerBackdrop()

    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeOut(animationSpec = tween(250))
    ) {
        PillContent(
            state = state,
            amplitudeProvider = amplitudeProvider,
            message = message,
            cancelArming = cancelArming,
            backdrop = backdrop,
            onRetry = onRetry,
            onCancel = onCancel,
            onStop = onStop,
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )
    }
}

@Composable
private fun PillContent(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String,
    cancelArming: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val isListening = state is BubbleState.Listening
    val isProcessing = state is BubbleState.Processing
    val isFormatting = state is BubbleState.Formatting
    val isMiniMode = (state as? BubbleState.Processing)?.miniMode == true
    val isError = state is BubbleState.Error
    val accentColor = WhispryTheme.colors.accent

    val targetWidth = when {
        isMiniMode -> 56.dp
        isProcessing || isFormatting -> 200.dp
        isListening && message.isEmpty() -> 280.dp 
        isError -> 220.dp
        message.isNotEmpty() -> 300.dp 
        else -> 120.dp
    }
    
    val targetHeight = if (isMiniMode) 56.dp else 68.dp 
    val cornerRadius = if (isMiniMode) 28.dp else 34.dp

    val configuration = LocalConfiguration.current
    val maxBubbleWidth = with(LocalDensity.current) {
        val widthDp = (configuration.screenWidthDp * 0.85f).dp
        if (widthDp > 420.dp) 420.dp else widthDp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "PillWidth"
    )
    
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "PillHeight"
    )

    Box(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .width(animatedWidth)
            .height(animatedHeight)
            .graphicsLayer {
                clip = true
                shape = ContinuousRoundedRectangle(cornerRadius)
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(if (isMiniMode) 8.dp.toPx() else 4.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = if (isMiniMode) 0.3f else 0.2f))
                    if (isListening || isProcessing || isFormatting) {
                        drawRect(accentColor.copy(alpha = if (isMiniMode) 0.4f else 0.3f))
                    }
                    // Widget drag-down cancel mirror: drain the pill red while armed.
                    if (isListening && cancelArming) {
                        drawRect(Color(0xFFE5484D).copy(alpha = 0.45f))
                    }
                    if (isError) {
                        drawRect(Color.Red.copy(alpha = 0.3f))
                    }
                }
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isError) onRetry()
            }
            .padding(horizontal = if (isMiniMode) 0.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMiniMode) {
            ThreeDotLoader()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isListening || isProcessing || isFormatting) {
                    if (isListening) {
                        VoiceBarsVisualizer(
                            amplitudeProvider = amplitudeProvider,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        SiriRingBubble(
                            isListening = false,
                            isProcessing = true,
                            amplitudeProvider = amplitudeProvider,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (isError) {
                        val networkError = (state as? BubbleState.Error)?.isNetworkError == true
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (networkError) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiOff,
                                    contentDescription = "No Internet",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column {
                                Text(
                                    text = if (networkError) "No Internet" else message,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap to retry",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (isFormatting) {
                        val preset = (state as BubbleState.Formatting).preset
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.emoji,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Formatting...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (isProcessing) {
                        Text(
                            text = "Thinking...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isListening) {
                        Text(
                            text = if (cancelArming) "Release to cancel" else "Listening...",
                            color = if (cancelArming) Color.White else Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = if (cancelArming) FontWeight.SemiBold else FontWeight.Medium
                        )
                    } else if (message.isNotEmpty()) {
                        Text(
                            text = message,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                if (isListening || isProcessing || isFormatting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isListening) {
                            // Stop Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { onStop() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = "Stop",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Cancel/Discard Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.15f))
                                .clickable { onCancel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Discard",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreeDotLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "Dots")
    val dotCount = 3
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { i ->
            val translationY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = i * 150, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Dot$i"
            )
            
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer {
                        this.translationY = translationY
                    }
                    .background(WhispryTheme.colors.accent.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}
