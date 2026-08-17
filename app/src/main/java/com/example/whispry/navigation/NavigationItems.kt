// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.whispry.R

data class NavigationItem(
    val route: Route,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val filledIcon: ImageVector = icon
)

val mainNavigationItems = listOf(
    NavigationItem(
        route = Route.Home,
        labelRes = R.string.nav_home,
        icon = Icons.Rounded.Home
    ),
    NavigationItem(
        route = Route.Library,
        labelRes = R.string.nav_library,
        icon = Icons.Rounded.LibraryBooks
    ),
    NavigationItem(
        route = Route.Presets,
        labelRes = R.string.nav_presets,
        icon = Icons.Rounded.AutoFixHigh
    ),
    NavigationItem(
        route = Route.Settings,
        labelRes = R.string.nav_settings,
        icon = Icons.Rounded.Settings
    ),
    NavigationItem(
        route = Route.About,
        labelRes = R.string.nav_about,
        icon = Icons.Rounded.Info
    )
)
