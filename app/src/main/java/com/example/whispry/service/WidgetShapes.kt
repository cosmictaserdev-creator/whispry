package com.example.whispry.service

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

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

/**
 * Edge-mode widget shape: a bar that wraps a screen corner, following the device's
 * rounded corner — an "L" of constant thickness with a concentric outer/inner arc
 * and rounded arm caps, so it reads as part of the screen bezel.
 *
 * Drawn for the BOTTOM-RIGHT corner; [corner] reflects it for the other three.
 *
 * @param archPx outer corner radius in px (user-adjustable to match the device corner).
 * @param thicknessPx bar thickness in px.
 */
class CornerArchShape(
    private val corner: WidgetCorner,
    private val archPx: Float,
    private val thicknessPx: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val t = thicknessPx.coerceIn(1f, min(w, h))
        val outerR = archPx.coerceIn(t, min(w, h))
        // Keep the inner corner a real curve — when the arch is close to the bar thickness
        // the concentric radius collapses to ~0 and reads as a hard edge.
        val minInnerR = 10f * density.density
        val innerR = (outerR - t).coerceAtLeast(minInnerR)
        val cap = min(t / 2f, 12f)

        val flipX = corner == WidgetCorner.TopLeft || corner == WidgetCorner.BottomLeft
        val flipY = corner == WidgetCorner.TopLeft || corner == WidgetCorner.TopRight

        fun x(v: Float): Float = if (flipX) w - v else v
        fun y(v: Float): Float = if (flipY) h - v else v

        val path = Path().apply {
            // Horizontal arm end (toward screen center), outer corner flush to the edge.
            moveTo(x(0f), y(h))
            // Along the bottom screen edge to the outer corner arc.
            lineTo(x(w - outerR), y(h))
            // Outer arc wrapping the device corner.
            cubicTo(
                x(w - outerR * 0.448f), y(h),
                x(w), y(h - outerR * 0.448f),
                x(w), y(h - outerR)
            )
            // Up the right screen edge to the vertical arm end.
            lineTo(x(w), y(0f))
            // Vertical arm cap: rounded on the inner side.
            lineTo(x(w - t + cap), y(0f))
            cubicTo(
                x(w - t + cap * 0.448f), y(0f),
                x(w - t), y(cap * 0.448f),
                x(w - t), y(cap)
            )
            // Down the inner edge to the inner corner arc.
            lineTo(x(w - t), y(h - t - innerR))
            // Inner arc, concentric with the outer one.
            cubicTo(
                x(w - t), y(h - t - innerR * 0.448f),
                x(w - t - innerR * 0.448f), y(h - t),
                x(w - t - innerR), y(h - t)
            )
            // Back along the inner edge of the horizontal arm.
            lineTo(x(cap), y(h - t))
            // Horizontal arm cap.
            cubicTo(
                x(cap * 0.448f), y(h - t),
                x(0f), y(h - t + cap * 0.448f),
                x(0f), y(h - t + cap)
            )
            close()
        }
        return Outline.Generic(path)
    }
}
