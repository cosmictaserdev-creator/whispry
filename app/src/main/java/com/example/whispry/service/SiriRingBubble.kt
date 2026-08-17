// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.whispry.ui.theme.WhispryTheme
import kotlin.math.*

import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Simplified and optimized visualizer for the Pill UI.
 */
@Composable
fun SiriRingBubble(
    isListening: Boolean,
    isProcessing: Boolean,
    amplitudeProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    // Only run the infinite transition if we are actively visualizing
    val isActive = isListening || isProcessing

    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTime")
    val time by if (isActive) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2 * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Time"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val colorA = WhispryTheme.colors.accent
    val colorB = WhispryTheme.colors.accentSoft

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val path = Path()
                onDrawWithContent {
                    if (!isActive) return@onDrawWithContent

                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = size.width / 2 * 0.75f
                    val currentAmplitude = amplitudeProvider()

                    // Draw 3 morphing rings with slight variations
                    drawMorphingRing(path, center, baseRadius, time, currentAmplitude, colorA, 0)
                    drawMorphingRing(path, center, baseRadius, time * 1.2f, currentAmplitude * 0.8f, colorB, 1)
                    drawMorphingRing(path, center, baseRadius, time * 0.8f, currentAmplitude * 1.1f, Color.White, 2)
                }
            }
    )
}

private fun DrawScope.drawMorphingRing(
    path: Path,
    center: Offset,
    radius: Float,
    time: Float,
    amplitude: Float,
    color: Color,
    index: Int
) {
    val pointsCount = 8
    val angleStep = (2 * PI / pointsCount).toFloat()

    path.reset()

    for (i in 0 until pointsCount) {
        val angle = i * angleStep
        val wave = sin(time + i * 1.5f + index * 0.8f) * (0.12f + amplitude * 0.5f) +
                   cos(time * 1.5f + i * 0.5f) * 0.03f
        val r = radius * (1f + wave)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)

        if (i == 0) path.moveTo(x, y)

        val nextAngle = (i + 1) * angleStep
        val nextWave = sin(time + (i + 1) * 1.5f + index * 0.8f) * (0.12f + amplitude * 0.5f) +
                   cos(time * 1.5f + (i + 1) * 0.5f) * 0.03f
        val nextR = radius * (1f + nextWave)
        val nextX = center.x + nextR * cos(nextAngle)
        val nextY = center.y + nextR * sin(nextAngle)

        val midX = (x + nextX) / 2
        val midY = (y + nextY) / 2
        path.quadraticTo(x, y, midX, midY)
    }
    path.close()

    drawPath(
        path = path,
        color = color.copy(alpha = 0.7f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}
