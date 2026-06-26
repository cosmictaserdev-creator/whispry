package com.example.whispry.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The darkening top bar shared by every main screen: a vertical gradient that is opaque at the
 * very top (so a pinned title and the status-bar icons stay legible) and fades to transparent
 * by the bottom, letting content scroll up underneath it. Purely decorative — no pointer-input
 * modifier, so touches pass straight through to the content below.
 *
 * Caller controls the size via [modifier] — typically `fillMaxWidth().height(statusBar + title)`
 * or `matchParentSize()` inside a header panel.
 *
 * Note: an earlier version frosted the content beneath with a `drawBackdrop` blur, but doing
 * that from *inside* the global `layerBackdrop` layer makes the render node reference itself and
 * crashes (native stack overflow in libhwui `prepareTreeImpl`). A real under-bar blur would have
 * to live as a top overlay in the nav scaffold, outside the backdrop layer, like the bottom bar.
 */
@Composable
fun TopFadeScrim(
    modifier: Modifier = Modifier,
    scrimColor: Color = Color(0xFF121212),
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to scrimColor,
                0.55f to scrimColor.copy(alpha = 0.85f),
                1.0f to Color.Transparent
            )
        )
    )
}
