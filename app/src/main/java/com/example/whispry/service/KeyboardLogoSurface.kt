// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.model.PresetGroup
import com.example.whispry.ui.theme.WhispryTheme

/**
 * The keyboard-anchored record toggle. A modern, ultra-aesthetic pill floating above the soft keyboard:
 * tap = start/stop recording, double-tap = open the preset panel, 2D drag moves it freely.
 * Features fluid enter/exit animations when the keyboard comes and goes, a sleek glassmorphic container,
 * dynamic recording audio-wave motion, and tactile press scaling.
 */
@Composable
fun KeyboardLogoSurface(
    isRecording: Boolean,
    onToggle: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accent = WhispryTheme.colors.accent
    val recordingRed = Color(0xFFEF4444)
    val density = LocalDensity.current

    // Smooth visibility transition (entrance/exit when keyboard comes/goes)
    val visibilityProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isVisible) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(durationMillis = 180, easing = FastOutLinearInEasing)
        },
        label = "PillVisibilityProgress"
    )

    // Tactile press state
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "PillPressScale"
    )

    // Animated recording glow & pulse
    val transition = rememberInfiniteTransition(label = "LogoPulse")
    val recordingPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "BadgePulse"
    )

    // Dynamic color morphing between idle and recording
    val currentAccentColor by animateColorAsState(
        targetValue = if (isRecording) recordingRed else accent,
        animationSpec = tween(350),
        label = "AccentColorMorph"
    )

    val backgroundBrush = if (isRecording) {
        Brush.horizontalGradient(listOf(Color(0xFA2C1014), Color(0xFA1D0D12)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFA14151E), Color(0xFA1E1F2C)))
    }

    val borderBrush = if (isRecording) {
        Brush.horizontalGradient(listOf(recordingRed.copy(alpha = 0.85f), Color(0xFFFF6B6B).copy(alpha = 0.60f)))
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                accent.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    }

    val combinedScale = (0.75f + (0.25f * visibilityProgress)) * pressScale
    val alphaVal = visibilityProgress.coerceIn(0f, 1f)
    val translationYPx = with(density) { ((1f - visibilityProgress) * 20.dp.toPx()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = alphaVal
                translationY = translationYPx
            }
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onDoubleTap = { onDoubleTap?.invoke() },
                    onTap = { onToggle() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Centered Whispry Logo Emblem
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(currentAccentColor.copy(alpha = if (isRecording) 0.25f else 0.15f))
                .border(1.dp, currentAccentColor.copy(alpha = 0.35f), CircleShape)
                .graphicsLayer {
                    if (isRecording) {
                        scaleX = recordingPulse
                        scaleY = recordingPulse
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.whisperlogo),
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Dynamic sound wave animation bars displayed inside the pill when recording.
 */
@Composable
private fun AudioWaveformBars(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
    }
}

/**
 * Compact preset picker for the keyboard logo's preset panel. Renders every preset grouped by
 * [PresetGroup] in a scrollable column; picking calls [onPick] with the new default. Scales in
 * from the pill (0.8 -> 1.0) and fades on first composition.
 */
@Composable
fun KeyboardLogoPresetPanel(
    selectedPreset: OutputPreset,
    onPick: (OutputPreset) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = WhispryTheme.colors.accent
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(220)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = scale.value
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF2161722))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Text(
            text = "Output preset",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            OutputPreset.byGroup().forEach { (group, presets) ->
                item(key = "header_${group.name}") {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp)
                    )
                }
                items(presets, key = { it.name }) { preset ->
                    val isSelected = preset == selectedPreset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onPick(preset) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = preset.emoji, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(
                                text = "•",
                                color = accent,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            item(key = "dismiss") {
                Text(
                    text = "Tap outside to close",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

