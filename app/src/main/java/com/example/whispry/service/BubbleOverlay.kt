package com.example.whispry.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * Modern Pill-style Overlay that slides up from the bottom with spring physics.
 */
@Composable
fun BubbleOverlay(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String,
    onRetry: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val visibleState = remember { MutableTransitionState(false) }
    
    // Sync visibleState with state
    LaunchedEffect(state) {
        visibleState.targetState = state !is BubbleState.Idle
    }
    
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Color.Black.copy(alpha = 0.1f))
        )

        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 150f)
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                PillContent(state, amplitudeProvider, message, backdrop, onRetry, onCancel)
            }
        }
    }
}

@Composable
private fun PillContent(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String,
    backdrop: com.kyant.backdrop.Backdrop,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val isListening = state is BubbleState.Listening
    val isProcessing = state is BubbleState.Processing
    val isMiniMode = (state as? BubbleState.Processing)?.miniMode == true
    val isError = state is BubbleState.Error
    val accentColor = WhispryTheme.colors.accent

    val targetWidth = when {
        isMiniMode -> 56.dp
        isProcessing -> 180.dp
        isListening && message.isEmpty() -> 160.dp
        isError -> 200.dp
        message.isNotEmpty() -> 240.dp
        else -> 120.dp
    }
    
    val targetHeight = if (isMiniMode) 56.dp else 58.dp
    val cornerRadius = if (isMiniMode) 28.dp else 29.dp

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
                    drawRect(Color.White.copy(alpha = if (isMiniMode) 0.08f else 0.05f))
                    if (isListening || isProcessing) {
                        drawRect(accentColor.copy(alpha = if (isMiniMode) 0.15f else 0.1f))
                    }
                    if (isError) {
                        drawRect(Color.Red.copy(alpha = 0.1f))
                    }
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isProcessing) onCancel()
                if (isError) onRetry()
            }
            .padding(horizontal = if (isMiniMode) 0.dp else 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMiniMode) {
            ThreeDotLoader()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isListening || isProcessing) {
                    SiriRingBubble(
                        isListening = isListening,
                        isProcessing = isProcessing,
                        amplitudeProvider = amplitudeProvider,
                        modifier = Modifier.size(28.dp)
                    )
                    if (message.isNotEmpty() || isProcessing) Spacer(modifier = Modifier.width(12.dp))
                }

                if (isError) {
                    val networkError = (state as? BubbleState.Error)?.isNetworkError == true
                    if (networkError) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = "No Internet",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (networkError) "No Internet" else message,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to retry",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (isProcessing) {
                    val showHint = (state as? BubbleState.Processing)?.showCancelHint == true
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Thinking...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (showHint) {
                            Text(
                                text = "Tap to cancel",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                } else if (isListening && message.isEmpty()) {
                    Text(
                        text = "Listening...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreeDotLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "DotLoader")
    val accentColor = WhispryTheme.colors.accent

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val delay = i * 150
            val translationY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6.dp.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "DotTranslation"
            )
            
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer {
                        this.translationY = translationY
                    }
                    .background(accentColor.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}
