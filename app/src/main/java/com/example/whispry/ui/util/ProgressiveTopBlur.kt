package com.example.whispry.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

/**
 * A real progressive (variable) blur strip for the top of a screen: frosts the content scrolling
 * underneath and fades that frosting out toward the bottom, so it reads as a soft gradient blur
 * rather than a hard band.
 *
 * IMPORTANT: this samples [backdrop] (the global layer backdrop), so it must be placed as a
 * sibling OUTSIDE the `layerBackdrop(...)` content layer — exactly like the floating bottom bar.
 * Drawing a backdrop from inside its own layer self-references the render node and crashes
 * (native stack overflow in libhwui). See [TopFadeScrim] for the original note.
 *
 * Purely decorative — no pointer-input, so touches pass straight through to the content below.
 */
@Composable
fun ProgressiveTopBlur(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    blurRadius: Float = 18f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // Offscreen compositing lets the DstIn gradient carve the alpha of the frosted strip.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Slight darken at the very top so status-bar icons stay legible.
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.30f),
                        0.55f to Color.Transparent
                    )
                )
                // Fade the whole frosted strip out toward the bottom -> progressive blur.
                drawRect(
                    brush = Brush.verticalGradient(
                        0.45f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(blurRadius) }
            )
    )
}
