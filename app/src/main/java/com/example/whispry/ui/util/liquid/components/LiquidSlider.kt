package com.example.whispry.ui.util.liquid.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.utils.DampedDragAnimation
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val accentColor = WhispryTheme.colors.accent
    val trackColor = Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.toFloat()

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        // Snapping logic for discrete intervals
        fun snapValue(v: Float): Float {
            if (steps <= 0) return v.coerceIn(valueRange)
            val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
            val stepIndex = ((v - valueRange.start) / stepSize).fastRoundToInt()
            return (valueRange.start + stepIndex * stepSize).coerceIn(valueRange)
        }

        var dragOffset by remember { mutableStateOf(0f) }

        val dampedDragAnimation = remember(animationScope, trackWidth) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = snapValue(value()),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.35f,
                onDragStarted = {
                    dragOffset = progress * trackWidth
                },
                onDragStopped = {
                    if (didDrag) {
                        val snapped = snapValue(this.value)
                        onValueChange(snapped)
                        animateToValue(snapped)
                        didDrag = false
                    }
                },
                onDrag = { _, dragAmount ->
                    didDrag = true
                    dragOffset = (dragOffset + (if (isLtr) dragAmount.x else -dragAmount.x)).coerceIn(0f, trackWidth)
                    
                    val rangeWidth = valueRange.endInclusive - valueRange.start
                    val newValue = valueRange.start + (dragOffset / trackWidth) * rangeWidth
                    
                    val snapped = snapValue(newValue)
                    
                    // Update visual immediately and smoothly
                    snapToValue(if (steps > 0) snapped else newValue)
                    
                    if (snapped != value()) {
                        onValueChange(snapped)
                    }
                }
            )
        }
        
        LaunchedEffect(dampedDragAnimation, value()) {
            val v = value()
            if (dampedDragAnimation.targetValue != v && !didDrag) {
                dampedDragAnimation.updateValue(v)
            }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(ContinuousRoundedRectangle(100.dp))
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val rawValue = (if (isLtr) valueRange.start + delta else valueRange.endInclusive - delta)
                            val targetValue = snapValue(rawValue)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                        }
                    }
                    .height(8.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(ContinuousRoundedRectangle(100.dp))
                    .background(accentColor)
                    .height(8.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousRoundedRectangle(100.dp) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 0.1f)) // More visible surface
                        drawRect(accentColor.copy(alpha = 0.2f * progress)) // Accent hint
                    }
                )
                .size(48.dp, 32.dp) // Slightly larger handle
        )
    }
}
