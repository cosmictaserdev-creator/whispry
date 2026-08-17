// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.model.PresetGroup
import com.example.whispry.ui.theme.WhispryTheme

/**
 * The keyboard-anchored record toggle. A compact pill that floats above the soft keyboard:
 * tap = start/stop recording, double-tap = open the preset panel, 2D drag moves it freely.
 * The host re-anchors it to the keyboard (saved offset above the IME) and persists the
 * placement when [onDragEnd] fires after a drag. With [onDoubleTap] set, single taps resolve
 * after the double-tap window (~300ms) so the two don't collide.
 */
@Composable
fun KeyboardLogoSurface(
    isRecording: Boolean,
    onToggle: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = WhispryTheme.colors.accent
    val recordingRed = Color(0xFFE5484D)
    val surface = if (isRecording) recordingRed else accent

    val pulse = if (isRecording) {
        val transition = rememberInfiniteTransition(label = "LogoPulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.82f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "LogoPulseAlpha"
        ).value
    } else 1f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pulse }
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF01A1A20))
            .border(1.dp, surface.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                if (onDoubleTap != null) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() }, onTap = { onToggle() })
                } else {
                    detectTapGestures(onTap = { onToggle() })
                }
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
        Image(
            painter = painterResource(id = R.drawable.whisperlogo),
            contentDescription = if (isRecording) "Stop recording" else "Start recording",
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
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
            .background(Color(0xF21A1A20))
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
