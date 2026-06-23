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
}
