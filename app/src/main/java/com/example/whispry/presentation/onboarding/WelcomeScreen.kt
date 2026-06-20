package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.service.SiriRingBubble
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    backdrop: Backdrop
) {
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    val orbScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(stiffness = 140f, dampingRatio = 0.65f),
        label = "OrbScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = orbScale
                    scaleY = orbScale
                },
            contentAlignment = Alignment.Center
        ) {
            AmbientOrbField(backdrop)

            SiriRingBubble(
                isListening = showContent,
                isProcessing = false,
                amplitudeProvider = { if (showContent) 0.5f else 0f },
                modifier = Modifier.size(180.dp)
            )

            CoreOrbGlow()
        }

        Spacer(modifier = Modifier.height(64.dp))

        if (showContent) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                StaggeredTextReveal(
                    text = "Meet Whispry",
                    style = TextStyle(
                        color = WhispryTokens.TextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                StaggeredTextReveal(
                    text = "Voice to text, everywhere.\nInstantly understood.",
                    style = TextStyle(
                        color = WhispryTokens.TextSecondary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    ),
                    delayMs = 500
                )

                Spacer(modifier = Modifier.height(80.dp))

                LiquidButton(
                    onClick = onContinue,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        "Continue",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientOrbField(backdrop: Backdrop) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientOrb")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breath"
    )

    val glowColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
            }
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
    )
}

@Composable
private fun CoreOrbGlow() {
    val accent = androidx.compose.ui.graphics.Color.White
    Canvas(modifier = Modifier.size(40.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accent.copy(alpha = 0.85f), Color.Transparent)
            ),
            blendMode = BlendMode.Screen
        )
    }
}
