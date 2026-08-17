// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.example.whispry.ui.util.liquid.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * The reusable "liquid button" touch feel, extracted so every card/row across the app shares it.
 *
 * It faithfully reproduces [com.example.whispry.ui.util.liquid.components.LiquidButton]: a radial
 * white glow blooms from the finger while the surface scales up and rubber-stretches toward the
 * touch point. All three pieces are driven by one [InteractiveHighlight].
 *
 * Because the press transform has to wrap the card *surface* (shadow/background/border) while the
 * glow must draw *over* that surface but *under* the content, the effect can't be a single trailing
 * modifier — it needs two insertion points sharing one [LiquidTouch]:
 *
 * ```
 * val touch = rememberLiquidTouch()
 * Box(
 *     Modifier
 *         .liquidExpand(touch)                 // FIRST — transforms the whole surface
 *         .shadow(..., shape)
 *         .background(..., shape)
 *         .border(..., shape)
 *         .liquidGlow(touch, shape)            // AFTER border — glow + gesture, clipped to shape
 *         .clickable(onClick = ...)            // each call site keeps its own click
 *         .padding(...)
 * )
 * ```
 */
class LiquidTouch internal constructor(
    val highlight: InteractiveHighlight
)

/**
 * Remembers a [LiquidTouch] (its coroutine scope + [InteractiveHighlight]) for one element.
 * [intensity] scales the glow brightness: 1f matches the LiquidButton; pass a lower value (e.g.
 * ~0.35f) for surfaces that should glow subtly, like the dense Settings cards.
 */
@Composable
fun rememberLiquidTouch(intensity: Float = 1f): LiquidTouch {
    val scope = rememberCoroutineScope()
    return remember(scope, intensity) { LiquidTouch(InteractiveHighlight(scope, intensity = intensity)) }
}

/**
 * The press transform: scales the surface up slightly and stretches/translates it toward the touch
 * point. Place this FIRST in the modifier chain (before shadow/background) so it moves the whole
 * card surface, not just its content. Math ported verbatim from `LiquidButton`; it self-scales by
 * size so the stretch stays subtle on large cards.
 */
fun Modifier.liquidExpand(touch: LiquidTouch, enabled: Boolean = true): Modifier =
    if (!enabled) this
    else this.graphicsLayer {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@graphicsLayer

        val highlight = touch.highlight
        val progress = highlight.pressProgress
        val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

        val maxOffset = size.minDimension
        val initialDerivative = 0.05f
        val offset = highlight.offset
        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

        val maxDragScale = 4f.dp.toPx() / height
        val offsetAngle = atan2(offset.y, offset.x)
        scaleX = scale +
            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
            (width / height).fastCoerceAtMost(1f)
        scaleY = scale +
            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
            (height / width).fastCoerceAtMost(1f)
    }

/**
 * The glow + gesture tracking. Place this AFTER the background/border so the radial highlight draws
 * over the surface but under the content. [shape] clips the glow to the surface's rounded corners
 * (the one improvement over `LiquidButton`, which doesn't clip). Adds no clickable — the gesture
 * tracker is non-consuming, so it coexists with the call site's own `clickable` (and with drag
 * gestures like the transcript-card swipe, where it simply self-cancels when a drag is consumed).
 */
fun Modifier.liquidGlow(touch: LiquidTouch, shape: Shape, enabled: Boolean = true): Modifier =
    if (!enabled) this
    else this
        .clip(shape)
        .then(touch.highlight.modifier)
        .then(touch.highlight.gestureModifier)
