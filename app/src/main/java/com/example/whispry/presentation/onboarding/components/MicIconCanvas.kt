// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Programmatically drawn Microphone icon for the Intro screen.
 * Allows staggered animation of individual parts.
 */
@Composable
fun MicIconCanvas(
    bodyAlpha: Float,
    standAlpha: Float,
    baseAlpha: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // 1. Mic Body (Rounded Rectangle)
        val bodyWidth = w * 0.4f
        val bodyHeight = h * 0.55f
        val bodyTop = h * 0.1f
        val bodyLeft = (w - bodyWidth) / 2
        
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyWidth / 2, bodyWidth / 2),
            alpha = bodyAlpha
        )

        // 2. Mic Stand (Arc)
        val arcWidth = w * 0.7f
        val arcHeight = h * 0.4f
        val arcTop = bodyTop + bodyHeight * 0.4f
        val arcLeft = (w - arcWidth) / 2
        
        val standPath = Path().apply {
            arcTo(
                rect = Rect(Offset(arcLeft, arcTop), Size(arcWidth, arcHeight)),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        }
        
        drawPath(
            path = standPath,
            color = color,
            alpha = standAlpha,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Connect Line
        val lineTop = arcTop + arcHeight
        val lineBottom = h * 0.95f
        
        drawLine(
            color = color,
            start = Offset(w / 2, lineTop),
            end = Offset(w / 2, lineBottom),
            alpha = baseAlpha,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 4. Base Line
        val baseWidth = w * 0.35f
        drawLine(
            color = color,
            start = Offset(w / 2 - baseWidth / 2, lineBottom),
            end = Offset(w / 2 + baseWidth / 2, lineBottom),
            alpha = baseAlpha,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
