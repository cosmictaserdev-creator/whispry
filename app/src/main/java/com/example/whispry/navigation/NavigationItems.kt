package com.example.whispry.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val filledIcon: ImageVector = icon
)

val mainNavigationItems = listOf(
    NavigationItem(
        route = Route.Home,
        label = "Home",
        icon = Icons.Rounded.Home
    ),
    NavigationItem(
        route = Route.Library,
        label = "Library",
        icon = Icons.Rounded.LibraryBooks
    ),
    NavigationItem(
        route = Route.Presets,
        label = "Presets",
        icon = Icons.Rounded.AutoFixHigh
    ),
    NavigationItem(
        route = Route.Settings,
        label = "Settings",
        icon = Icons.Rounded.Settings
    ),
    NavigationItem(
        route = Route.About,
        label = "About",
        icon = Icons.Rounded.Info
    )
)
