package com.example.whispry.presentation.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared animated background for the onboarding flow.
 * Features a deep void, ambient glow bloom, and a drifting particle field.
 */
@Composable
fun WhispryBackground(
    glowIntensity: Float,      // 0f-1f, drives ambient glow size
    glowOffset: Offset,        // center of glow (animates between screens)
    particleAlpha: Float,      // 0f-1f
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundDrive")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    val accent = WhispryTheme.colors.accent
    val accentSoft = WhispryTheme.colors.accentSoft
    val accentGlow = WhispryTheme.colors.accentGlow

    // Seed particles once
    val particles = remember(accent, accentSoft) {
        val random = Random(42)
        List(60) {
            ParticleData(
                baseX = random.nextFloat(),
                baseY = random.nextFloat(),
                radius = (0.8f + random.nextFloat() * 1.4f).dp,
                alpha = 0.06f + random.nextFloat() * 0.19f,
                speed = 0.5f + random.nextFloat() * 1.5f,
                phaseOffset = random.nextFloat() * 6.28f,
                color = lerp(accent, accentSoft, random.nextFloat())
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Base fill: DeepVoid
        drawRect(color = WhispryTokens.DeepVoid)

        // 2. Ambient glow bloom
        val radius = 200.dp.toPx() + (200.dp.toPx() * glowIntensity)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentGlow, Color.Transparent),
                center = glowOffset,
                radius = radius
            ),
            center = glowOffset,
            radius = radius,
            blendMode = BlendMode.Screen
        )

        // 3. Particle field
        particles.forEach { p ->
            val drift = sin(time * p.speed + p.phaseOffset) * 3.dp.toPx()
            val pos = Offset(
                x = p.baseX * size.width + drift,
                y = p.baseY * size.height + drift
            )
            
            drawCircle(
                color = p.color,
                radius = p.radius.toPx(),
                center = pos,
                alpha = p.alpha * particleAlpha,
                blendMode = BlendMode.Screen
            )
        }
    }
}

private data class ParticleData(
    val baseX: Float,
    val baseY: Float,
    val radius: androidx.compose.ui.unit.Dp,
    val alpha: Float,
    val speed: Float,
    val phaseOffset: Float,
    val color: Color
)
