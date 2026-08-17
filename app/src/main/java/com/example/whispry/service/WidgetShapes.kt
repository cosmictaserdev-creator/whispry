// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Ramp-mode widget shape: a smooth bump hugging a screen edge — seen from the side it
 * reads like a speed ramp: the surface arches up from the edge, runs flat along the
 * inner face (the plateau), then arches back down to the edge. Both ramp ends are
 * tangent to the screen edge and the plateau is tangent to the inner face, so there
 * are no visible corners anywhere — a rounded "finger" resting against the edge.
 *
 * Drawn for the RIGHT edge; [mirrored] reflects it for the left edge so the ramp
 * always points inward.
 *
 * @param rampFraction each ramp's share of the total height (the plateau keeps the rest).
 */
class RampWedgeShape(
    private val mirrored: Boolean,
    private val rampFraction: Float = 0.24f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val ramp = (h * rampFraction).coerceAtLeast(0f)

        fun x(v: Float): Float = if (mirrored) w - v else v

        val path = Path().apply {
            // Top end, flush on the screen edge.
            moveTo(x(w), 0f)
            // Arch up: leaves the edge running ALONG it (vertical tangent) and lands on
            // the plateau also vertically — a smooth S with no corner at either end.
            cubicTo(
                x(w), ramp * 0.55f,
                x(0f), ramp * 0.45f,
                x(0f), ramp
            )
            // The flat plane of the ramp (inner face).
            lineTo(x(0f), h - ramp)
            // Arch back down to the edge, mirrored S.
            cubicTo(
                x(0f), h - ramp * 0.45f,
                x(w), h - ramp * 0.55f,
                x(w), h
            )
            // Straight back up along the screen edge.
            close()
        }
        return Outline.Generic(path)
    }
}
