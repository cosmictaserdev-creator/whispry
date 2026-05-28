package com.example.whispry.service

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ScreenEdgeGlow(
    amplitude: Float,
    visible: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edgeGlow")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowPhase"
    )

    val breathingScale by animateFloatAsState(
        targetValue = if (visible) 1f + (amplitude * 0.3f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "breathing"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(500))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx() * breathingScale
            
            // Create a rainbow gradient that "moves" around the border
            val rainbowColors = listOf(
                Color(0xFF6200EE), // Purple
                Color(0xFF00E5FF), // Teal
                Color(0xFFFF4081), // Pink
                Color(0xFF3D5AFE), // Blue
                Color(0xFF6200EE)  // Back to Purple
            )
            
            val gradientBrush = Brush.linearGradient(
                colors = rainbowColors,
                start = Offset(size.width * phase, 0f),
                end = Offset(size.width * (phase + 1f), size.height)
            )

            drawRect(
                brush = gradientBrush,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
