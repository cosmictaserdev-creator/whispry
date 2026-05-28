package com.example.whispry.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val filledIcon: ImageVector? = null
) {
    data object Home : Screen(
        route = "home",
        label = "Home",
        icon = Icons.Outlined.Home,
        filledIcon = Icons.Rounded.Home
    )
    
    data object Library : Screen(
        route = "library",
        label = "Library",
        icon = Icons.Outlined.Search,
        filledIcon = Icons.Rounded.Search
    )

    data object FavoriteDetails : Screen(
        route = "favorite_details",
        label = "Favorites"
    )

    data object RecentDetails : Screen(
        route = "recent_details",
        label = "Recents"
    )
    
    data object Settings : Screen(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Tune,
        filledIcon = Icons.Rounded.Tune
    )
    
    data object About : Screen(
        route = "about",
        label = "About",
        icon = Icons.Outlined.Info,
        filledIcon = Icons.Rounded.Info
    )
}
