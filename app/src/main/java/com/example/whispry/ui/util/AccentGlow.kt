package com.example.whispry.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The app's signature accent glow: a soft radial gradient anchored to the top-right corner.
 * Shared by every top-level layout so the treatment stays identical across phone tabs and
 * the tablet/landscape rail rather than being copy-pasted per layout.
 */
fun Modifier.accentGlow(color: Color): Modifier = drawBehind {
    val radius = size.width * 0.7f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(size.width, 0f),
            radius = radius
        ),
        radius = radius,
        center = Offset(size.width, 0f)
    )
}
