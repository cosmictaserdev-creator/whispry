package com.example.whispry.service

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.whispry.ui.theme.WhispryTheme
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val BAR_COUNT = 5

/**
 * Equalizer-style bars that react to live mic amplitude, replacing [SiriRingBubble] while the
 * pill is actively Listening. Each bar has its own phase so bars don't move in lockstep, and
 * smooths toward the polled amplitude (rather than snapping) so it doesn't look jittery between
 * the ~120ms amplitude polls it's fed from.
 */
@Composable
fun VoiceBarsVisualizer(
    amplitudeProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceBarsTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    val color = WhispryTheme.colors.accent
    val smoothed = remember { FloatArray(BAR_COUNT) { 0.08f } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val amplitude = amplitudeProvider().coerceIn(0f, 1f)
        val barWidth = size.width / (BAR_COUNT * 2 - 1)
        val maxBarHeight = size.height

        for (i in 0 until BAR_COUNT) {
            // Per-bar phase so bars don't all rise and fall in lockstep.
            val jitter = 0.55f + 0.45f * sin(time * (1f + i * 0.35f) + i)
            val target = max(0.08f, amplitude * jitter)
            // Fast attack, slower decay — reads like a real VU meter rather than snapping.
            val rate = if (target > smoothed[i]) 0.5f else 0.18f
            smoothed[i] += (target - smoothed[i]) * rate

            val barHeight = maxBarHeight * smoothed[i].coerceIn(0.08f, 1f)
            val x = i * barWidth * 2
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (maxBarHeight - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
