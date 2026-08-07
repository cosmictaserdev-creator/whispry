package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.onboarding.components.MultiLineStaggeredText
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun TutorialScreen(
    state: OnboardingState,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    backdrop: Backdrop
) {
    LaunchedEffect(Unit) {
        onStart()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Shrink the phone mock and vertical gutters on short screens so the guide and actions
        // fit without scrolling.
        val compact = maxHeight < 640.dp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(if (compact) 16.dp else 32.dp))

            // Visual Guide
            Box(
                modifier = Modifier
                    .size(width = if (compact) 150.dp else 180.dp, height = if (compact) 260.dp else 310.dp)
                    .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(36.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(36.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(36.dp))
                    .border(1.dp, Color.White.copy(0.12f), ContinuousRoundedRectangle(36.dp)),
                contentAlignment = Alignment.TopEnd
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "ButtonPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )

                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = if (compact) 86.dp else 110.dp)
                        .size(width = 8.dp, height = 44.dp)
                        .scale(if (state.tutorialStep == TutorialStep.DoublePressMe || state.tutorialStep == TutorialStep.HoldMe) pulseScale else 1f)
                        .background(
                            if (state.tutorialStep == TutorialStep.Recording) androidx.compose.ui.graphics.Color.White 
                            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                            RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                        )
                )
                
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = state.tutorialStep,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "TutorialVisualState"
                    ) { step ->
                        when(step) {
                            TutorialStep.DoublePressMe -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.TouchApp, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(48.dp))
                                    Text("Double Press", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                                }
                            }
                            TutorialStep.HoldMe -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.TouchApp, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(48.dp))
                                    Text("Hold", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                                }
                            }
                            TutorialStep.Recording -> {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(56.dp))
                                    Icon(Icons.Rounded.Mic, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                            TutorialStep.Success -> {
                                Icon(Icons.Rounded.CheckCircle, null, tint = WhispryTokens.SuccessGreen, modifier = Modifier.size(64.dp))
                            }
                            TutorialStep.Failed -> {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = Color(0xFFFF5252), modifier = Modifier.size(64.dp))
                            }
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                key(state.tutorialStep) {
                    StaggeredTextReveal(
                        text = when(state.tutorialStep) {
                            TutorialStep.DoublePressMe -> "Double Press ${state.triggerKeyLabel}"
                            TutorialStep.HoldMe -> if (state.isHoldGesture) "Hold ${state.triggerKeyLabel}" else "Now Hold it!"
                            TutorialStep.Recording -> "Recording... Say something!"
                            TutorialStep.Success -> "Perfect!"
                            TutorialStep.Failed -> "Hmm, that didn't go through"
                            else -> "Get Ready"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = WhispryTokens.TextPrimary,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    StaggeredTextReveal(
                        text = when(state.tutorialStep) {
                            TutorialStep.DoublePressMe -> "Press the ${state.triggerKeyLabel.lowercase()} button twice quickly, then hold."
                            TutorialStep.HoldMe -> if (state.isHoldGesture) "Press and hold the ${state.triggerKeyLabel.lowercase()} button while you speak." else "Keep holding the button while you speak."
                            TutorialStep.Recording -> "Release the button when you're finished."
                            TutorialStep.Success -> "You've mastered the physical gesture!"
                            TutorialStep.Failed -> "We couldn't transcribe that — usually a weak connection. You can try again or skip and explore the app."
                            else -> "Follow the guide above."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WhispryTokens.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            fontSize = 17.sp
                        ),
                        delayMs = 250
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 20.dp else 40.dp))

            AnimatedVisibility(
                visible = state.tutorialStep == TutorialStep.Success,
                enter = expandVertically() + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                        MultiLineStaggeredText(
                            text = state.recordedText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = WhispryTokens.TextPrimary,
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                lineHeight = 26.sp
                            ),
                            modifier = Modifier.padding(24.dp),
                            delayMs = 500,
                            staggerPerWordMs = 70
                        )
                    }

            Spacer(modifier = Modifier.height(if (compact) 24.dp else 48.dp))

                    LiquidButton(
                        onClick = onContinue,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text(
                            "Complete Onboarding",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            // Graceful failure: retry the live practice or skip into the app.
            AnimatedVisibility(
                visible = state.tutorialStep == TutorialStep.Failed,
                enter = expandVertically() + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LiquidButton(
                        onClick = onRetry,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text(
                            "Try Again",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip for now",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = WhispryTokens.TextSecondary
                        )
                    }
                }
            }

            // Always let the user out of the live practice so a stuck gesture never traps them.
            AnimatedVisibility(
                visible = state.tutorialStep != TutorialStep.Success && state.tutorialStep != TutorialStep.Failed,
                enter = fadeIn()
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        "Skip for now",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = WhispryTokens.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 16.dp else 32.dp))
        }
    }
    }
}
