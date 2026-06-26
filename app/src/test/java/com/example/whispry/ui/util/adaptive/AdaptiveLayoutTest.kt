package com.example.whispry.ui.util.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {

    // --- width size class boundaries ---

    @Test
    fun `width below 600 is compact`() {
        assertEquals(WindowWidthSizeClass.Compact, widthSizeClassFor(0))
        assertEquals(WindowWidthSizeClass.Compact, widthSizeClassFor(360))
        assertEquals(WindowWidthSizeClass.Compact, widthSizeClassFor(599))
    }

    @Test
    fun `width 600 to 839 is medium`() {
        assertEquals(WindowWidthSizeClass.Medium, widthSizeClassFor(600))
        assertEquals(WindowWidthSizeClass.Medium, widthSizeClassFor(720))
        assertEquals(WindowWidthSizeClass.Medium, widthSizeClassFor(839))
    }

    @Test
    fun `width 840 and above is expanded`() {
        assertEquals(WindowWidthSizeClass.Expanded, widthSizeClassFor(840))
        assertEquals(WindowWidthSizeClass.Expanded, widthSizeClassFor(1280))
    }

    // --- layout mode across the full matrix (preserves established behavior) ---

    @Test
    fun `compact portrait is phone portrait`() {
        assertEquals(LayoutMode.PhonePortrait, layoutModeFor(360, isLandscape = false))
        assertEquals(LayoutMode.PhonePortrait, layoutModeFor(599, isLandscape = false))
    }

    @Test
    fun `compact landscape is phone landscape`() {
        assertEquals(LayoutMode.PhoneLandscape, layoutModeFor(360, isLandscape = true))
        assertEquals(LayoutMode.PhoneLandscape, layoutModeFor(599, isLandscape = true))
    }

    @Test
    fun `medium portrait is tablet`() {
        assertEquals(LayoutMode.Tablet, layoutModeFor(600, isLandscape = false))
        assertEquals(LayoutMode.Tablet, layoutModeFor(839, isLandscape = false))
    }

    @Test
    fun `medium landscape is phone landscape`() {
        assertEquals(LayoutMode.PhoneLandscape, layoutModeFor(600, isLandscape = true))
        assertEquals(LayoutMode.PhoneLandscape, layoutModeFor(839, isLandscape = true))
    }

    @Test
    fun `expanded is tablet in either orientation`() {
        assertEquals(LayoutMode.Tablet, layoutModeFor(840, isLandscape = false))
        assertEquals(LayoutMode.Tablet, layoutModeFor(840, isLandscape = true))
        assertEquals(LayoutMode.Tablet, layoutModeFor(1280, isLandscape = true))
    }

    // --- grid columns ---

    @Test
    fun `grid is one column on compact and two on larger`() {
        assertEquals(1, gridColumnsFor(WindowWidthSizeClass.Compact))
        assertEquals(2, gridColumnsFor(WindowWidthSizeClass.Medium))
        assertEquals(2, gridColumnsFor(WindowWidthSizeClass.Expanded))
    }

    // --- master-detail engagement ---

    @Test
    fun `master detail engages only at expanded`() {
        assertEquals(false, masterDetailEnabledFor(WindowWidthSizeClass.Compact))
        assertEquals(false, masterDetailEnabledFor(WindowWidthSizeClass.Medium))
        assertEquals(true, masterDetailEnabledFor(WindowWidthSizeClass.Expanded))
    }
}
