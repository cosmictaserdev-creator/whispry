package com.example.whispry.service

import kotlin.math.abs

data class BubbleBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

enum class BubbleOrientation { Horizontal, Vertical }

sealed interface SnapTarget {
    data object CenterTop : SnapTarget
    data object CenterBottom : SnapTarget
    data object LeftEdge : SnapTarget
    data object RightEdge : SnapTarget
}

/** Which screen edge the ramp-mode floating widget hugs. */
enum class WidgetEdge { Left, Right }

/** Which screen corner the edge-mode floating widget wraps. */
enum class WidgetCorner { TopLeft, TopRight, BottomLeft, BottomRight }

class BubblePositionManager(private val density: Float) {

    private val snapMarginDp = 16f
    private val centerZoneThresholdDp = 100f

    fun snapTarget(
        currentX: Float,
        currentY: Float,
        bubbleWidth: Float,
        bubbleHeight: Float,
        safeBounds: BubbleBounds
    ): SnapTarget {
        val centerX = safeBounds.centerX
        val thresholdPx = centerZoneThresholdDp * density
        val bubbleCenterX = currentX + bubbleWidth / 2f
        val bubbleCenterY = currentY + bubbleHeight / 2f

        return if (abs(bubbleCenterX - centerX) <= thresholdPx) {
            if (bubbleCenterY < safeBounds.centerY) SnapTarget.CenterTop
            else SnapTarget.CenterBottom
        } else {
            if (bubbleCenterX < centerX) SnapTarget.LeftEdge
            else SnapTarget.RightEdge
        }
    }

    fun snapPosition(
        currentX: Float,
        currentY: Float,
        bubbleWidth: Float,
        bubbleHeight: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val target = snapTarget(currentX, currentY, bubbleWidth, bubbleHeight, safeBounds)
        val margin = snapMarginDp * density
        return when (target) {
            SnapTarget.CenterTop -> Pair(
                safeBounds.centerX - bubbleWidth / 2f,
                safeBounds.top + margin
            )
            SnapTarget.CenterBottom -> Pair(
                safeBounds.centerX - bubbleWidth / 2f,
                safeBounds.bottom - bubbleHeight - margin
            )
            SnapTarget.LeftEdge -> Pair(
                safeBounds.left + margin,
                currentY.coerceIn(safeBounds.top.toFloat(), (safeBounds.bottom - bubbleHeight).toFloat())
            )
            SnapTarget.RightEdge -> Pair(
                safeBounds.right - bubbleWidth - margin,
                currentY.coerceIn(safeBounds.top.toFloat(), (safeBounds.bottom - bubbleHeight).toFloat())
            )
        }
    }

    fun orientationFor(target: SnapTarget): BubbleOrientation = when (target) {
        SnapTarget.CenterTop, SnapTarget.CenterBottom -> BubbleOrientation.Horizontal
        SnapTarget.LeftEdge, SnapTarget.RightEdge -> BubbleOrientation.Vertical
    }

    fun normalize(
        x: Float,
        y: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val nx = ((x - safeBounds.left) / safeBounds.width) * 100f
        val ny = ((y - safeBounds.top) / safeBounds.height) * 100f
        return Pair(nx, ny)
    }

    fun denormalize(
        nx: Float,
        ny: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val x = safeBounds.left + (nx / 100f) * safeBounds.width
        val y = safeBounds.top + (ny / 100f) * safeBounds.height
        return Pair(x, y)
    }

    fun defaultPosition(
        bubbleWidth: Float,
        bubbleHeight: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val x = safeBounds.centerX - bubbleWidth / 2f
        val y = safeBounds.bottom - bubbleHeight - snapMarginDp * density
        return Pair(x, y)
    }

    // ------------------------------------------------------------------
    // Floating widget (physical switch) — its own snap rules, independent
    // of the recording pill's targets above.
    // ------------------------------------------------------------------

    /** Ramp mode snaps to the left/right edges only, chosen by which half holds the center. */
    fun widgetEdgeTarget(
        currentX: Float,
        widgetWidth: Float,
        safeBounds: BubbleBounds
    ): WidgetEdge {
        val centerX = currentX + widgetWidth / 2f
        return if (centerX < safeBounds.centerX) WidgetEdge.Left else WidgetEdge.Right
    }

    /**
     * Position for a ramp-mode widget snapped to [edge]: flush against the edge (the shape
     * itself renders the half-clipped protrusion), free along y within the safe bounds.
     */
    fun widgetEdgePosition(
        edge: WidgetEdge,
        currentY: Float,
        widgetWidth: Float,
        widgetHeight: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val x = when (edge) {
            WidgetEdge.Left -> safeBounds.left.toFloat()
            WidgetEdge.Right -> safeBounds.right - widgetWidth
        }
        val y = currentY.coerceIn(
            safeBounds.top.toFloat(),
            (safeBounds.bottom - widgetHeight).coerceAtLeast(safeBounds.top.toFloat())
        )
        return Pair(x, y)
    }

    /** The wedge ramp always points inward, so the shape mirrors on the left edge. */
    fun widgetMirrored(edge: WidgetEdge): Boolean = edge == WidgetEdge.Left

    /** Edge mode snaps to the nearest of the four corners, chosen by the widget's center. */
    fun widgetCornerTarget(
        currentX: Float,
        currentY: Float,
        widgetWidth: Float,
        widgetHeight: Float,
        safeBounds: BubbleBounds
    ): WidgetCorner {
        val centerX = currentX + widgetWidth / 2f
        val centerY = currentY + widgetHeight / 2f
        val left = centerX < safeBounds.centerX
        val top = centerY < safeBounds.centerY
        return when {
            left && top -> WidgetCorner.TopLeft
            !left && top -> WidgetCorner.TopRight
            left -> WidgetCorner.BottomLeft
            else -> WidgetCorner.BottomRight
        }
    }

    /** Position for an edge-mode widget tucked flush into [corner]. */
    fun widgetCornerPosition(
        corner: WidgetCorner,
        widgetWidth: Float,
        widgetHeight: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val x = when (corner) {
            WidgetCorner.TopLeft, WidgetCorner.BottomLeft -> safeBounds.left.toFloat()
            else -> safeBounds.right - widgetWidth
        }
        val y = when (corner) {
            WidgetCorner.TopLeft, WidgetCorner.TopRight -> safeBounds.top.toFloat()
            else -> safeBounds.bottom - widgetHeight
        }
        return Pair(x, y)
    }

    /** Out-of-the-box ramp position: lower-mid of the right edge. */
    fun widgetDefaultEdgePosition(
        widgetWidth: Float,
        widgetHeight: Float,
        safeBounds: BubbleBounds
    ): Pair<Float, Float> {
        val y = safeBounds.top + safeBounds.height * 0.62f
        return widgetEdgePosition(WidgetEdge.Right, y, widgetWidth, widgetHeight, safeBounds)
    }
}
