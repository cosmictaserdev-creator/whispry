package com.example.whispry.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BubblePositionManagerTest {

    private lateinit var manager: BubblePositionManager
    private val density = 2f // mdpi = 1, xhdpi = 2, xxhdpi = 3
    private val phonePortrait = BubbleBounds(0, 100, 1080, 2200)
    private val phoneLandscape = BubbleBounds(0, 0, 2200, 1080)
    private val tablet = BubbleBounds(0, 100, 1600, 2400)

    @Before
    fun setUp() {
        manager = BubblePositionManager(density)
    }

    @Test
    fun `default position returns center bottom`() {
        val (x, y) = manager.defaultPosition(200f, 68f, phonePortrait)

        val expectedX = phonePortrait.centerX - 100f
        val expectedY = phonePortrait.bottom - 68f - 16f * density

        assertEquals(expectedX, x, 0.01f)
        assertEquals(expectedY, y, 0.01f)
    }

    @Test
    fun `snap to center bottom when in bottom half and near center`() {
        val (x, y) = phonePortrait.centerX - 100f to (phonePortrait.bottom - 200f)

        val target = manager.snapTarget(x, y, 200f, 68f, phonePortrait)

        assertEquals(SnapTarget.CenterBottom, target)
    }

    @Test
    fun `snap to center top when in top half and near center`() {
        val (x, y) = phonePortrait.centerX + 50f to (phonePortrait.top + 200f)

        val target = manager.snapTarget(x, y, 200f, 68f, phonePortrait)

        assertEquals(SnapTarget.CenterTop, target)
    }

    @Test
    fun `snap to left edge when far from center on the left`() {
        val x = phonePortrait.left + 50f
        val y = phonePortrait.centerY

        val target = manager.snapTarget(x, y, 200f, 68f, phonePortrait)

        assertEquals(SnapTarget.LeftEdge, target)
    }

    @Test
    fun `snap to right edge when far from center on the right`() {
        val x = phonePortrait.right - 250f
        val y = phonePortrait.centerY

        val target = manager.snapTarget(x, y, 200f, 68f, phonePortrait)

        assertEquals(SnapTarget.RightEdge, target)
    }

    @Test
    fun `snap to left edge in landscape when dragged far left`() {
        val x = phoneLandscape.left + 50f
        val y = phoneLandscape.centerY

        val target = manager.snapTarget(x, y, 200f, 68f, phoneLandscape)

        assertEquals(SnapTarget.LeftEdge, target)
    }

    @Test
    fun `snap to right edge on tablet when dragged far right`() {
        val x = tablet.right - 250f
        val y = tablet.centerY

        val target = manager.snapTarget(x, y, 200f, 68f, tablet)

        assertEquals(SnapTarget.RightEdge, target)
    }

    @Test
    fun `snap to center bottom on tablet when near center`() {
        val x = tablet.centerX - 50f
        val y = tablet.bottom - 300f

        val target = manager.snapTarget(x, y, 200f, 68f, tablet)

        assertEquals(SnapTarget.CenterBottom, target)
    }

    @Test
    fun `snap position for center bottom is centered horizontally with bottom margin`() {
        val (snapX, snapY) = manager.snapPosition(
            phonePortrait.centerX, phonePortrait.bottom - 200f,
            200f, 68f, phonePortrait
        )

        val expectedX = phonePortrait.centerX - 100f
        val expectedY = phonePortrait.bottom - 68f - 32f

        assertEquals(expectedX, snapX, 0.01f)
        assertEquals(expectedY, snapY, 0.01f)
    }

    @Test
    fun `snap position for left edge has left margin and same y`() {
        val dragY = phonePortrait.centerY
        val (snapX, snapY) = manager.snapPosition(
            phonePortrait.left + 50f, dragY,
            200f, 68f, phonePortrait
        )

        assertEquals(32f, snapX, 0.01f) // 16dp * 2 density = 32px
        assertEquals(dragY, snapY, 0.01f)
    }

    @Test
    fun `snap position for right edge has right margin`() {
        val dragY = phonePortrait.centerY
        val (snapX, snapY) = manager.snapPosition(
            phonePortrait.right - 100f, dragY,
            200f, 68f, phonePortrait
        )

        val expectedX = phonePortrait.right - 200f - 32f
        assertEquals(expectedX, snapX, 0.01f)
        assertEquals(dragY, snapY, 0.01f)
    }

    @Test
    fun `snap position for center top is centered horizontally with top margin`() {
        val (snapX, snapY) = manager.snapPosition(
            phonePortrait.centerX, phonePortrait.top + 200f,
            200f, 68f, phonePortrait
        )

        val expectedX = phonePortrait.centerX - 100f
        val expectedY = phonePortrait.top + 32f

        assertEquals(expectedX, snapX, 0.01f)
        assertEquals(expectedY, snapY, 0.01f)
    }

    @Test
    fun `normalize and denormalize round trip to identity`() {
        val originalX = 400f
        val originalY = 800f

        val (nx, ny) = manager.normalize(originalX, originalY, phonePortrait)
        val (rx, ry) = manager.denormalize(nx, ny, phonePortrait)

        assertEquals(originalX, rx, 0.01f)
        assertEquals(originalY, ry, 0.01f)
    }

    @Test
    fun `normalize and denormalize round trip on different screen sizes`() {
        val originalX = 200f
        val originalY = 500f

        val (nx, ny) = manager.normalize(originalX, originalY, phonePortrait)
        val (rx, ry) = manager.denormalize(nx, ny, tablet)

        val xRatio = (originalX - phonePortrait.left) / phonePortrait.width
        val yRatio = (originalY - phonePortrait.top) / phonePortrait.height
        val expectedX = tablet.left + xRatio * tablet.width
        val expectedY = tablet.top + yRatio * tablet.height

        assertEquals(expectedX, rx, 0.01f)
        assertEquals(expectedY, ry, 0.01f)
    }

    @Test
    fun `normalize converts to percentage of safe bounds`() {
        val quarterWidth = phonePortrait.left + phonePortrait.width / 4f
        val quarterHeight = phonePortrait.top + phonePortrait.height / 4f

        val (nx, ny) = manager.normalize(quarterWidth, quarterHeight, phonePortrait)

        assertEquals(25f, nx, 0.01f)
        assertEquals(25f, ny, 0.01f)
    }

    @Test
    fun `snap within exact center threshold boundary`() {
        val thresholdPx = 100f * density
        val x = phonePortrait.centerX + thresholdPx
        val y = phonePortrait.bottom - 200f

        val target = manager.snapTarget(x - 200f / 2f, y, 200f, 68f, phonePortrait)

        assertEquals(SnapTarget.CenterBottom, target)
    }

    @Test
    fun `orientation is horizontal for center targets`() {
        assertEquals(BubbleOrientation.Horizontal, manager.orientationFor(SnapTarget.CenterTop))
        assertEquals(BubbleOrientation.Horizontal, manager.orientationFor(SnapTarget.CenterBottom))
    }

    @Test
    fun `orientation is vertical for edge targets`() {
        assertEquals(BubbleOrientation.Vertical, manager.orientationFor(SnapTarget.LeftEdge))
        assertEquals(BubbleOrientation.Vertical, manager.orientationFor(SnapTarget.RightEdge))
    }

    @Test
    fun `edge snap y is constrained within safe bounds`() {
        val aboveTop = phonePortrait.top - 500f
        val (_, snapY) = manager.snapPosition(
            phonePortrait.left + 50f, aboveTop,
            200f, 68f, phonePortrait
        )

        assertEquals(phonePortrait.top.toFloat(), snapY, 0.01f)
    }

    @Test
    fun `edge snap y is constrained below safe bounds`() {
        val belowBottom = phonePortrait.bottom + 500f
        val (_, snapY) = manager.snapPosition(
            phonePortrait.left + 50f, belowBottom,
            200f, 68f, phonePortrait
        )

        val expectedY = phonePortrait.bottom - 68f
        assertEquals(expectedY.toFloat(), snapY, 0.01f)
    }

    @Test
    fun `denormalize with zero values returns top-left of safe bounds`() {
        val (x, y) = manager.denormalize(0f, 0f, phonePortrait)

        assertEquals(phonePortrait.left.toFloat(), x, 0.01f)
        assertEquals(phonePortrait.top.toFloat(), y, 0.01f)
    }

    @Test
    fun `denormalize with 100 percent returns bottom-right of safe bounds`() {
        val (x, y) = manager.denormalize(100f, 100f, phonePortrait)

        assertEquals(phonePortrait.right.toFloat(), x, 0.01f)
        assertEquals(phonePortrait.bottom.toFloat(), y, 0.01f)
    }

    // ------------------------------------------------------------------
    // Floating widget: ramp mode (left/right edges only)
    // ------------------------------------------------------------------

    @Test
    fun `widget in left half targets left edge`() {
        val target = manager.widgetEdgeTarget(phonePortrait.left + 100f, 80f, phonePortrait)

        assertEquals(WidgetEdge.Left, target)
    }

    @Test
    fun `widget in right half targets right edge`() {
        val target = manager.widgetEdgeTarget(phonePortrait.right - 300f, 80f, phonePortrait)

        assertEquals(WidgetEdge.Right, target)
    }

    @Test
    fun `widget near center never targets top or bottom`() {
        // Dragged to the exact horizontal center near the top: still resolves to an edge.
        val slightlyLeft = manager.widgetEdgeTarget(phonePortrait.centerX - 41f, 80f, phonePortrait)
        val slightlyRight = manager.widgetEdgeTarget(phonePortrait.centerX - 39f, 80f, phonePortrait)

        assertEquals(WidgetEdge.Left, slightlyLeft)
        assertEquals(WidgetEdge.Right, slightlyRight)
    }

    @Test
    fun `widget edge position sits flush on the left edge keeping y`() {
        val (x, y) = manager.widgetEdgePosition(
            WidgetEdge.Left, 900f, 80f, 140f, phonePortrait
        )

        assertEquals(phonePortrait.left.toFloat(), x, 0.01f)
        assertEquals(900f, y, 0.01f)
    }

    @Test
    fun `widget edge position clears the left edge by the clearance`() {
        val clearancePx = 24f * density
        val (x, _) = manager.widgetEdgePosition(
            WidgetEdge.Left, 900f, 80f, 140f, phonePortrait, clearancePx
        )

        assertEquals(phonePortrait.left.toFloat() + clearancePx, x, 0.01f)
    }

    @Test
    fun `widget edge position clears the right edge by the clearance`() {
        val clearancePx = 12f * density
        val (x, _) = manager.widgetEdgePosition(
            WidgetEdge.Right, 900f, 80f, 140f, phonePortrait, clearancePx
        )

        assertEquals(phonePortrait.right - 80f - clearancePx, x, 0.01f)
    }

    @Test
    fun `widget default edge position honors clearance`() {
        val clearancePx = 12f * density
        val (x, _) = manager.widgetDefaultEdgePosition(80f, 140f, phonePortrait, clearancePx)

        assertEquals(phonePortrait.right - 80f - clearancePx, x, 0.01f)
    }

    @Test
    fun `widget edge position sits flush on the right edge`() {
        val (x, _) = manager.widgetEdgePosition(
            WidgetEdge.Right, 900f, 80f, 140f, phonePortrait
        )

        assertEquals(phonePortrait.right - 80f, x, 0.01f)
    }

    @Test
    fun `widget edge position constrains y within safe bounds`() {
        val (_, yTop) = manager.widgetEdgePosition(
            WidgetEdge.Right, phonePortrait.top - 400f, 80f, 140f, phonePortrait
        )
        val (_, yBottom) = manager.widgetEdgePosition(
            WidgetEdge.Right, phonePortrait.bottom + 400f, 80f, 140f, phonePortrait
        )

        assertEquals(phonePortrait.top.toFloat(), yTop, 0.01f)
        assertEquals(phonePortrait.bottom - 140f, yBottom, 0.01f)
    }

    @Test
    fun `widget mirrors on the left edge only`() {
        assertEquals(true, manager.widgetMirrored(WidgetEdge.Left))
        assertEquals(false, manager.widgetMirrored(WidgetEdge.Right))
    }

    @Test
    fun `widget default edge position is lower-mid right edge`() {
        val (x, y) = manager.widgetDefaultEdgePosition(80f, 140f, phonePortrait)

        assertEquals(phonePortrait.right - 80f, x, 0.01f)
        assertEquals(phonePortrait.top + phonePortrait.height * 0.62f, y, 0.01f)
    }

}
