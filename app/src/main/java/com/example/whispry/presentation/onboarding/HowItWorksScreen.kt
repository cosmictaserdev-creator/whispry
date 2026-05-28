package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlin.math.sin

enum class DemoPhase { IDLE, FIRST_PRESS, PAUSE, SECOND_PRESS_HOLD, ACTIVATED, RESULT, RESET }

@Composable
fun HowItWorksScreen(
    onContinue: () -> Unit,
    backdrop: Backdrop
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DemoCycle")
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "Cycle"
    )

    val phase = remember(cycleProgress) {
        when {
            cycleProgress < 500 -> DemoPhase.IDLE
            cycleProgress < 800 -> DemoPhase.FIRST_PRESS
            cycleProgress < 1100 -> DemoPhase.PAUSE
            cycleProgress < 1600 -> DemoPhase.SECOND_PRESS_HOLD
            cycleProgress < 2800 -> DemoPhase.ACTIVATED
            cycleProgress < 3500 -> DemoPhase.RESULT
            else -> DemoPhase.RESET
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(72.dp))

            PhoneMockup(phase, cycleProgress)

            Spacer(modifier = Modifier.height(48.dp))

            StaggeredTextReveal(
                text = "Double press and hold\nvolume down to talk.",
                style = TextStyle(
                    color = WhispryTokens.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StaggeredTextReveal(
                text = "Release the button to instantly\ntranscribe and paste.",
                style = TextStyle(
                    color = WhispryTokens.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                ),
                delayMs = 250
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            TimingDiagram(cycleProgress)
        }

        Column {
            LiquidButton(
                onClick = onContinue,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    "Got it",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WhispryTheme.colors.accent
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun PhoneMockup(phase: DemoPhase, cycleProgress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "PhoneFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Float"
    )

    val accent = WhispryTheme.colors.accent

    Canvas(
        modifier = Modifier
            .size(width = 180.dp, height = 310.dp)
            .graphicsLayer {
                translationY = floatOffset.dp.toPx()
                cameraDistance = 12 * density
            }
    ) {
        val cornerRadius = 36.dp.toPx()
        
        drawRoundRect(
            color = WhispryTokens.GlassBorder.copy(alpha = 0.45f),
            size = size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawRoundRect(
            color = WhispryTokens.SurfaceGlass.copy(alpha = 0.12f),
            size = size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        val buttonWidth = 6.dp.toPx()
        val buttonHeight = 48.dp.toPx()
        val buttonTop = 110.dp.toPx()
        val buttonLeft = size.width - buttonWidth / 2
        
        val isPressing = phase == DemoPhase.FIRST_PRESS || phase == DemoPhase.SECOND_PRESS_HOLD || phase == DemoPhase.ACTIVATED
        val buttonScale = if (isPressing) 0.85f else 1.0f
        
        drawRoundRect(
            color = if (isPressing) accent else Color.White.copy(alpha = 0.25f),
            topLeft = Offset(buttonLeft, buttonTop + (buttonHeight * (1 - buttonScale) / 2)),
            size = Size(buttonWidth, buttonHeight * buttonScale),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        if (phase == DemoPhase.ACTIVATED) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = 54.dp.toPx()
            val pulse = 1.0f + 0.18f * sin(cycleProgress * 0.015f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.3f), Color.Transparent)
                ),
                center = center,
                radius = baseRadius * pulse
            )
        }
    }
}

@Composable
private fun TimingDiagram(progress: Float) {
    val accent = WhispryTheme.colors.accent
    val accentSoft = WhispryTheme.colors.accentSoft

    Canvas(modifier = Modifier.size(width = 220.dp, height = 50.dp)) {
        val lineY = size.height / 2
        val strokeWidth = 2.5.dp.toPx()
        
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = strokeWidth
        )

        val blockSize = 16.dp.toPx()
        val blockCorner = 5.dp.toPx()
        
        drawRoundRect(
            color = if (progress > 500 && progress < 800) accent else accent.copy(alpha = 0.35f),
            topLeft = Offset(size.width * 0.2f - blockSize / 2, lineY - blockSize / 2),
            size = Size(blockSize, blockSize),
            cornerRadius = CornerRadius(blockCorner, blockCorner)
        )
        
        drawRoundRect(
            color = if (progress > 1100 && progress < 2800) accent else accent.copy(alpha = 0.35f),
            topLeft = Offset(size.width * 0.4f - blockSize / 2, lineY - blockSize / 2),
            size = Size(blockSize, blockSize),
            cornerRadius = CornerRadius(blockCorner, blockCorner)
        )
        
        if (progress > 1100) {
            val bracketStart = size.width * 0.4f
            val bracketEnd = size.width * 0.8f
            drawLine(
                color = accentSoft,
                start = Offset(bracketStart, lineY + 18.dp.toPx()),
                end = Offset(bracketEnd, lineY + 18.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
