package com.example.whispry.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Modern Pill-style Overlay that slides up from the bottom with spring physics.
 */
@Composable
fun BubbleOverlay(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String
) {
    val visibleState = remember { MutableTransitionState(false) }
    
    // Sync visibleState with state
    LaunchedEffect(state) {
        visibleState.targetState = state !is BubbleState.Idle
    }
    
    // Create a backdrop that captures ONLY the background (e.g. a slight dim)
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Capture layer (this blurs nothing since we are an overlay, but we need it for the library)
        // We capture a slight dim layer so the pill has something to "blur"
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
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = 150f
                )
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // PillContent is OUTSIDE the layerBackdrop box to avoid circular dependency
                PillContent(state, amplitudeProvider, message, backdrop)
            }
        }
    }
}

@Composable
private fun PillContent(
    state: BubbleState,
    amplitudeProvider: () -> Float,
    message: String,
    backdrop: com.kyant.backdrop.Backdrop
) {
    val isListening = state is BubbleState.Listening
    val isProcessing = state is BubbleState.Loading
    val accentColor = WhispryTheme.colors.accent

    val targetWidth = when {
        isProcessing -> 160.dp
        isListening && message.isEmpty() -> 160.dp
        message.isNotEmpty() -> 240.dp
        else -> 120.dp
    }
    
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 200f
        ),
        label = "PillWidth"
    )

    Box(
        modifier = Modifier
            .width(animatedWidth)
            .height(58.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(29.dp) },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.05f))
                    if (isListening || isProcessing) {
                        drawRect(accentColor.copy(alpha = 0.1f))
                    }
                }
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
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

            if (isProcessing && message.isEmpty()) {
                Text(
                    text = "Thinking...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
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
