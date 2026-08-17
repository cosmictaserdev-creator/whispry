// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.util.adaptive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Compose readers for the adaptive layout decisions. These are thin wrappers around the
 * pure functions in `AdaptiveLayout.kt`; all breakpoint logic lives there so it can be
 * unit-tested. Width comes from the current window (`screenWidthDp`), which updates in
 * split-screen / multi-window.
 */

@Composable
fun currentWidthSizeClass(): WindowWidthSizeClass =
    widthSizeClassFor(LocalConfiguration.current.screenWidthDp)

@Composable
fun currentLayoutMode(): LayoutMode {
    val configuration = LocalConfiguration.current
    return layoutModeFor(
        widthSizeClassFor(configuration.screenWidthDp),
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    )
}
