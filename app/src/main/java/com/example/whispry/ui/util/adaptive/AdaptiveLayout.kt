package com.example.whispry.ui.util.adaptive

/**
 * Pure, framework-free adaptive layout decisions.
 *
 * No Compose/Android imports live here so the breakpoint behavior can be unit-tested
 * without an emulator. This is the single source of truth for adaptive layout: the
 * `@Composable` readers in `WindowSize.kt` delegate here, and screens consume the
 * returned values rather than re-deriving width/orientation logic of their own.
 */

enum class WindowWidthSizeClass { Compact, Medium, Expanded }

enum class LayoutMode { PhonePortrait, PhoneLandscape, Tablet }

/**
 * Width breakpoints in dp. Always fed the *current-window* width (not the physical
 * display) so split-screen / multi-window adapts for free.
 */
fun widthSizeClassFor(widthDp: Int): WindowWidthSizeClass = when {
    widthDp < 600 -> WindowWidthSizeClass.Compact
    widthDp < 840 -> WindowWidthSizeClass.Medium
    else -> WindowWidthSizeClass.Expanded
}

/**
 * The single layout decision. Preserves the app's established behavior:
 * - portrait phone (compact width, portrait) keeps bottom tabs
 * - any landscape narrower than Expanded is treated as a landscape phone rail
 * - everything else (medium-portrait, expanded either orientation) is a tablet rail
 */
fun layoutModeFor(sizeClass: WindowWidthSizeClass, isLandscape: Boolean): LayoutMode = when {
    sizeClass == WindowWidthSizeClass.Compact && !isLandscape -> LayoutMode.PhonePortrait
    isLandscape && sizeClass != WindowWidthSizeClass.Expanded -> LayoutMode.PhoneLandscape
    else -> LayoutMode.Tablet
}

fun layoutModeFor(widthDp: Int, isLandscape: Boolean): LayoutMode =
    layoutModeFor(widthSizeClassFor(widthDp), isLandscape)

/** List grid density: 1 column on phones (compact), 2 on tablets (medium/expanded). No 3-column. */
fun gridColumnsFor(sizeClass: WindowWidthSizeClass): Int =
    if (sizeClass == WindowWidthSizeClass.Compact) 1 else 2

/** Master-detail panes engage only at Expanded; Medium and below use tap-to-open detail. */
fun masterDetailEnabledFor(sizeClass: WindowWidthSizeClass): Boolean =
    sizeClass == WindowWidthSizeClass.Expanded
