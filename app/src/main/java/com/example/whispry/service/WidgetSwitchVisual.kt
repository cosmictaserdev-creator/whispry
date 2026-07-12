package com.example.whispry.service

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.whispry.ui.theme.WhispryTheme
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * The contentless "physical plastic switch". Pure visuals — all gesture state is fed in
 * by [FloatingWidgetManager]; the only glyph it ever shows is the cancel cross while a
 * drag-down is in progress.
 */
@Composable
fun WidgetSwitchVisual(
    config: WidgetConfig,
    phase: WidgetGesturePhase,
    sessionActive: Boolean,
    cancelArmed: Boolean,
    dragYPx: Float,
    cancelThresholdPx: Float,
    idleFaded: Boolean,
    editMode: Boolean,
    edge: WidgetEdge,
    corner: WidgetCorner,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = WhispryTheme.colors.accent
    val pressed = phase == WidgetGesturePhase.ARMING
    val recording = phase == WidgetGesturePhase.RECORDING
    val sliver = phase == WidgetGesturePhase.SLIVER

    // The switch shrinks INTO its edge/corner, so scaling pivots on the anchored side.
    val (alignment, origin) = anchorFor(config.shapeMode, edge, corner, editMode)

    // Arming progress drives the depress gate below, so it's computed before the scale.
    val armingProgress = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            armingProgress.snapTo(0f)
            armingProgress.animateTo(1f, tween(config.armingDelayMs.toInt(), easing = LinearEasing))
        } else {
            armingProgress.snapTo(if (recording) 1f else 0f)
        }
    }
    // A press only starts LOOKING pressed a beat in — a back-gesture swipe grazing the
    // edge aborts before this gate opens, so the switch doesn't flash on every swipe.
    val pressCommitted = pressed && armingProgress.value > 0.15f

    val targetScale = when {
        editMode -> 1f
        // Swell while arming, then pop noticeably bigger once recording — the "it
        // triggered" moment is meant to be clearly visible.
        recording -> 1.5f
        pressCommitted -> 1f + 0.25f * armingProgress.value
        // Collapsed edge sliver: a much more aggressive shrink than CORNER's idle-fade, since
        // the actual touch window has already shrunk to match (see FloatingWidgetManager).
        // ponytail: uniform scale-down of the wedge shape, not a bespoke slim-rectangle Shape —
        // upgrade to a dedicated sliver Shape if the scaled wedge doesn't read cleanly on device.
        sliver -> 0.3f
        idleFaded -> 0.75f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = when {
            reducedMotion -> snap()
            recording -> spring(dampingRatio = 0.45f, stiffness = 320f) // bouncy pop on trigger
            pressed -> spring(dampingRatio = 0.7f, stiffness = 900f)
            else -> spring(dampingRatio = 0.55f, stiffness = 380f)
        },
        label = "WidgetScale"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            sliver -> 0.5f
            idleFaded && !editMode -> config.idleOpacityPct / 100f
            else -> 1f
        },
        animationSpec = if (reducedMotion) snap() else tween(500),
        label = "WidgetAlpha"
    )

    // Gentle breathing while recording (the pill carries the real waveform).
    val pulse = if (recording && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "WidgetPulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "WidgetPulseScale"
        ).value
    } else 1f

    // Elastic drag-down: resistance grows as the finger travels (tanh saturates the trail).
    val cancelProgress = if (recording) (dragYPx / cancelThresholdPx).coerceIn(0f, 1f) else 0f
    val trailPx = if (recording && dragYPx > 0f) {
        30f * LocalDensity.current.density * tanh(dragYPx / cancelThresholdPx)
    } else 0f

    // Red creeps in with the drag so the cancel direction is obvious, then locks full red
    // the moment the threshold arms.
    val redShift by animateFloatAsState(
        targetValue = if (cancelArmed) 1f else cancelProgress * 0.55f,
        animationSpec = if (reducedMotion) snap() else spring(stiffness = 700f),
        label = "WidgetRedShift"
    )
    val cancelRed = Color(0xFFE5484D)
    val fillColor = lerp(accent, cancelRed, redShift)
    val restColor = lerp(accent.copy(alpha = 1f), cancelRed, redShift)

    val density = LocalDensity.current
    val (visualW, visualH) = config.visualSizeDp()
    val shape: Shape = when {
        editMode -> RoundedCornerShape(50)
        config.shapeMode == WidgetShapeMode.RAMP -> RampWedgeShape(mirrored = edge == WidgetEdge.Left)
        else -> with(density) {
            CornerArchShape(corner, config.archDp.dp.toPx(), config.protrusionDp.dp.toPx())
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        Box(
            modifier = (if (editMode) Modifier.fillMaxSize() else Modifier.size(visualW.dp, visualH.dp))
                .graphicsLayer {
                    scaleX = scale * pulse
                    scaleY = scale * pulse
                    this.alpha = alpha
                    transformOrigin = origin
                    translationY = trailPx * 0.5f
                }
                .clip(shape)
                // Flat, solid body — no gradients; just the accent at two intensities.
                .background(restColor.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            // Arming fill sweeping up from the bottom; solid while any recording session is
            // live (also lights up when recording was started by another trigger). Gated a
            // beat in so an edge-swipe graze doesn't flash it.
            val fillFraction = when {
                recording || sessionActive || editMode -> 1f
                pressCommitted -> armingProgress.value
                else -> 0f
            }
            if (fillFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .background(fillColor)
                )
            }
        }

        // The cancel cross: grows and trails the finger during a drag-down.
        if (recording && cancelProgress > 0.04f) {
            val crossSize = (18 + 30 * cancelProgress).dp
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = lerp(Color.White, cancelRed, redShift),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(0, trailPx.roundToInt()) }
                    .size(crossSize)
                    .graphicsLayer { this.alpha = (0.5f + 0.5f * cancelProgress) }
            )
        }
    }
}

private fun anchorFor(
    mode: WidgetShapeMode,
    edge: WidgetEdge,
    corner: WidgetCorner,
    editMode: Boolean
): Pair<Alignment, TransformOrigin> {
    if (editMode) return Alignment.Center to TransformOrigin.Center
    return when (mode) {
        WidgetShapeMode.RAMP -> when (edge) {
            WidgetEdge.Left -> Alignment.CenterStart to TransformOrigin(0f, 0.5f)
            WidgetEdge.Right -> Alignment.CenterEnd to TransformOrigin(1f, 0.5f)
        }
        WidgetShapeMode.CORNER -> when (corner) {
            WidgetCorner.TopLeft -> Alignment.TopStart to TransformOrigin(0f, 0f)
            WidgetCorner.TopRight -> Alignment.TopEnd to TransformOrigin(1f, 0f)
            WidgetCorner.BottomLeft -> Alignment.BottomStart to TransformOrigin(0f, 1f)
            WidgetCorner.BottomRight -> Alignment.BottomEnd to TransformOrigin(1f, 1f)
        }
    }
}

/** Visible shape size in dp for the current mode (the window adds invisible touch slack). */
fun WidgetConfig.visualSizeDp(): Pair<Float, Float> = when (shapeMode) {
    // Wedge: inner face + two ramps; protrusion is the visible width off the edge.
    WidgetShapeMode.RAMP -> protrusionDp.toFloat() to baseHeightDp * 2.2f
    // Corner arch: square box big enough for the arch plus the two arms.
    WidgetShapeMode.CORNER -> {
        val box = maxOf(archDp + 28f, baseHeightDp * 1.6f)
        box to box
    }
}
