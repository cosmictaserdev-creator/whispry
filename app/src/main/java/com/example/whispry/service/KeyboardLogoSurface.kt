package com.example.whispry.service

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.whispry.R
import com.example.whispry.ui.theme.WhispryTheme

/**
 * The keyboard-anchored record toggle. A compact pill that floats above the soft keyboard:
 * tap = start/stop recording, horizontal drag moves it along the keyboard's top edge.
 * [onDragEnd] fires after a horizontal drag so the host can persist the resting X.
 */
@Composable
fun KeyboardLogoSurface(
    isRecording: Boolean,
    onToggle: () -> Unit,
    onDragX: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = WhispryTheme.colors.accent
    val recordingRed = Color(0xFFE5484D)
    val surface = if (isRecording) recordingRed else accent

    val pulse = if (isRecording) {
        val transition = rememberInfiniteTransition(label = "LogoPulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.82f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "LogoPulseAlpha"
        ).value
    } else 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pulse }
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF01A1A20))
            .border(1.dp, surface.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggle() })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onDragEnd,
                    onHorizontalDrag = { _, dragAmount -> onDragX(dragAmount) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.whisperlogo),
            contentDescription = if (isRecording) "Stop recording" else "Start recording",
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
    }
}
