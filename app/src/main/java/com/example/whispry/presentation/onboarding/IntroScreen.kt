// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.service.SiriRingBubble
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

/**
 * Single onboarding opener. The logo forms out of the void, the name reveals, then the logo
 * dissolves into a living orb and a Continue button fades in. Replaces the old separate
 * intro + welcome screens.
 */
enum class IntroPhase {
    VOID,
    AWAKENING,
    WORDS_FLOWING, // the tagline's words drift in and converge into the logo's spot
    LOGO_FORMING,
    NAME_REVEAL,
    DISSOLVE,     // logo cross-fades into the orb
    READY         // orb breathing, Continue available
}

@Composable
fun IntroScreen(
    onTransition: () -> Unit,
    backdrop: Backdrop
) {
    var phase by remember { mutableStateOf(IntroPhase.VOID) }

    LaunchedEffect(Unit) {
        delay(400)
        phase = IntroPhase.AWAKENING
        delay(500)
        phase = IntroPhase.WORDS_FLOWING
        delay(900)
        phase = IntroPhase.LOGO_FORMING
        delay(850)
        phase = IntroPhase.NAME_REVEAL
        delay(900)
        phase = IntroPhase.DISSOLVE
        delay(750)
        phase = IntroPhase.READY
    }

    // Logo: fades/scales in at LOGO_FORMING, fades out once it dissolves into the orb.
    val logoAlpha by animateFloatAsState(
        targetValue = when {
            phase < IntroPhase.LOGO_FORMING -> 0f
            phase >= IntroPhase.DISSOLVE -> 0f
            else -> 1f
        },
        animationSpec = tween(700),
        label = "LogoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = when {
            phase < IntroPhase.LOGO_FORMING -> 0.88f
            phase >= IntroPhase.DISSOLVE -> 1.15f // scales up slightly as it dissolves
            else -> 1f
        },
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.72f),
        label = "LogoScale"
    )

    // Orb: emerges as the logo dissolves.
    val orbScale by animateFloatAsState(
        targetValue = if (phase >= IntroPhase.DISSOLVE) 1f else 0.4f,
        animationSpec = spring(stiffness = 140f, dampingRatio = 0.65f),
        label = "OrbScale"
    )
    val orbAlpha by animateFloatAsState(
        targetValue = if (phase >= IntroPhase.DISSOLVE) 1f else 0f,
        animationSpec = tween(900),
        label = "OrbAlpha"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Compact on short screens: shrink the hero stage + tighten the vertical gutters so the
        // whole intro (logo, name, CTA) fits without scrolling on budget phones.
        val compact = maxHeight < 640.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shared center stage: logo and orb occupy the same spot and cross-fade.
        val stage = if (compact) 200.dp else 280.dp
        Box(
            modifier = Modifier.size(stage),
            contentAlignment = Alignment.Center
        ) {
            // The tagline's own words drift in from the edges and converge here, dissolving
            // into the logo as it forms — the "words flowing into the icon" moment.
            FlowingWordsField(phase)

            // Orb (underneath, emerges on dissolve)
            Box(
                modifier = Modifier
                    .size(stage)
                    .graphicsLayer {
                        scaleX = orbScale
                        scaleY = orbScale
                        alpha = orbAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                AmbientOrbField(stage)
                SiriRingBubble(
                    isListening = phase >= IntroPhase.DISSOLVE,
                    isProcessing = false,
                    amplitudeProvider = { if (phase >= IntroPhase.DISSOLVE) 0.5f else 0f },
                    modifier = Modifier.size(if (compact) 140.dp else 180.dp)
                )
                CoreOrbGlow()
            }

            // Logo (on top, dissolves away)
            Image(
                painter = painterResource(id = R.drawable.whisperlogo),
                contentDescription = "Whispry Logo",
                modifier = Modifier
                    .size(if (compact) 140.dp else 180.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 24.dp else 56.dp))

        AnimatedVisibility(
            visible = phase >= IntroPhase.NAME_REVEAL,
            enter = fadeIn(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StaggeredTextReveal(
                    text = "Whispry",
                    style = TextStyle(
                        color = WhispryTokens.TextPrimary,
                        fontSize = if (compact) 36.sp else 44.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )
                )

                Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))

                StaggeredTextReveal(
                    text = "Your voice, instantly understood.",
                    style = TextStyle(
                        color = WhispryTokens.TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    ),
                    delayMs = 400
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 32.dp else 72.dp))

        AnimatedVisibility(
            visible = phase >= IntroPhase.READY,
            enter = fadeIn(tween(700)) + expandVertically()
        ) {
            LiquidButton(
                onClick = onTransition,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    "Continue",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    }
}

/**
 * The app's own tagline words drift in from off-center and converge into the logo's spot,
 * fading out as [IntroPhase.LOGO_FORMING] begins — echoes the Wispr-Flow-style hero animation
 * without inventing new marketing copy (reuses the same words [NAME_REVEAL] shows below).
 */
@Composable
private fun FlowingWordsField(phase: IntroPhase) {
    val words = remember { listOf("Whispry", "Your voice,", "instantly", "understood.") }
    val startOffsets = remember {
        listOf(
            Offset(-150f, -130f), // top-left
            Offset(150f, -110f),  // top-right
            Offset(-140f, 140f),  // bottom-left
            Offset(160f, 130f)    // bottom-right
        )
    }

    words.forEachIndexed { i, word ->
        val progress by animateFloatAsState(
            targetValue = if (phase >= IntroPhase.WORDS_FLOWING) 1f else 0f,
            animationSpec = tween(900, delayMillis = i * 90, easing = FastOutSlowInEasing),
            label = "WordFlow$i"
        )
        val alpha by animateFloatAsState(
            targetValue = when {
                phase >= IntroPhase.LOGO_FORMING -> 0f
                phase >= IntroPhase.WORDS_FLOWING -> 1f
                else -> 0f
            },
            animationSpec = tween(
                durationMillis = if (phase >= IntroPhase.LOGO_FORMING) 600 else 350,
                delayMillis = i * 60
            ),
            label = "WordAlpha$i"
        )
        val start = startOffsets[i]

        Text(
            text = word,
            style = TextStyle(
                color = WhispryTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.graphicsLayer {
                translationX = start.x * (1f - progress)
                translationY = start.y * (1f - progress)
                this.alpha = alpha
                // Shrink slightly as the word converges into the icon, rather than arriving
                // at full size and popping out.
                val convergeScale = 1f - 0.35f * progress
                scaleX = convergeScale
                scaleY = convergeScale
            }
        )
    }
}

@Composable
private fun AmbientOrbField(size: Dp = 240.dp) {
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

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = breath
                scaleY = breath
            }
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = Color.Black)
            .background(Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
            .border(1.dp, WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
    )
}

@Composable
private fun CoreOrbGlow() {
    Canvas(modifier = Modifier.size(40.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0.85f), Color.Transparent)
            ),
            blendMode = BlendMode.Screen
        )
    }
}
