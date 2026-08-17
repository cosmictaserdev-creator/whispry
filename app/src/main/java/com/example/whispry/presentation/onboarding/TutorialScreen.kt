// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.onboarding.components.MultiLineStaggeredText
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var fieldText by remember { mutableStateOf("") }

    // Open the keyboard for the practice field so the real keyboard-logo mic appears above it.
    LaunchedEffect(state.tutorialStep) {
        when (state.tutorialStep) {
            TutorialStep.TapField -> {
                fieldText = ""
                delay(900)
                focusRequester.requestFocus()
            }
            TutorialStep.Success -> {
                fieldText = state.recordedText
                // Practice is over: drop the keyboard (and the logo with it) so the card shows.
                keyboardController?.hide()
            }
            TutorialStep.Failed -> keyboardController?.hide()
            else -> {}
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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

                key(state.tutorialStep) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StaggeredTextReveal(
                            text = when (state.tutorialStep) {
                                TutorialStep.TapField -> "Tap the field below"
                                TutorialStep.TapLogo -> "Tap the mic button"
                                TutorialStep.Recording -> "Speak now"
                                TutorialStep.Processing -> "One sec..."
                                TutorialStep.Success -> "You're done!"
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
                            text = when (state.tutorialStep) {
                                TutorialStep.TapField -> "Your keyboard will open, and Whispry's mic button will appear above it."
                                TutorialStep.TapLogo -> "It's the small accent pill floating above your keyboard. Tap it once to start listening."
                                TutorialStep.Recording -> "Say what you want to write. Tap the mic button again when you're done."
                                TutorialStep.Processing -> "Turning your words into text."
                                TutorialStep.Success -> "Your words were placed in the field. That's how you talk-to-type — anywhere."
                                TutorialStep.Failed -> "We couldn't transcribe that — usually a weak connection. You can try again or skip."
                                else -> "Follow the guide."
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

                Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                // The real practice field: focus it to summon the IME and the real keyboard logo.
                PracticeTextField(
                    value = fieldText,
                    onValueChange = { fieldText = it },
                    isListening = state.tutorialStep == TutorialStep.Recording,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                // Mini progress: which of the three moves has the user completed.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepPill(label = "Tap", done = state.tutorialStep.ordinal > TutorialStep.TapField.ordinal, active = state.tutorialStep == TutorialStep.TapField)
                    StepPill(label = "Speak", done = state.tutorialStep.ordinal > TutorialStep.Recording.ordinal, active = state.tutorialStep == TutorialStep.Recording)
                    StepPill(label = "Done", done = state.tutorialStep == TutorialStep.Success, active = state.tutorialStep == TutorialStep.Processing)
                }

                Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                AnimatedVisibility(
                    visible = state.tutorialStep == TutorialStep.Success,
                    enter = expandVertically() + fadeIn()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GlassCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    null,
                                    tint = WhispryTokens.SuccessGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Your words landed in the field. Nice work!",
                                    color = WhispryTokens.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (compact) 20.dp else 32.dp))

                        LiquidButton(
                            onClick = onContinue,
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text(
                                "Complete Onboarding",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
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
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LiquidButton(
                            onClick = onRetry,
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text(
                                "Try Again",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
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

                // Always let the user out of the live practice so a stuck step never traps them.
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

@Composable
private fun PracticeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isListening: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FieldPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FieldPulseAlpha"
    )

    Box(
        modifier = modifier
            .heightIn(min = 96.dp)
            .background(
                Color.White.copy(alpha = if (isListening) 0.1f else pulseAlpha),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 26.sp
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening... speak now" else "Say something...",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 18.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (isListening) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp)
            ) {
                Icon(Icons.Rounded.Mic, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                Text("recording", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepPill(
    label: String,
    done: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(
                when {
                    done -> WhispryTokens.SuccessGreen.copy(alpha = 0.15f)
                    active -> Color.White.copy(alpha = 0.12f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                RoundedCornerShape(50)
            )
            .border(
                1.dp,
                when {
                    done -> WhispryTokens.SuccessGreen.copy(alpha = 0.4f)
                    active -> Color.White.copy(alpha = 0.25f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        if (done) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = WhispryTokens.SuccessGreen,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = if (done) WhispryTokens.SuccessGreen else if (active) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
