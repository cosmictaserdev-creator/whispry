package com.example.whispry.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.whispry.features.expander.presentation.TextExpanderScreen
import com.example.whispry.features.expander.presentation.TextExpanderViewModel
import com.example.whispry.features.memory.presentation.MemoryScreen
import com.example.whispry.features.memory.presentation.MemoryViewModel
import com.example.whispry.features.tone.presentation.AppToneScreen
import com.example.whispry.features.tone.presentation.AppToneViewModel
import com.example.whispry.presentation.about.AboutScreen
import com.example.whispry.presentation.history.HistoryDetailScreen
import com.example.whispry.presentation.history.HistoryScreen
import com.example.whispry.presentation.history.HistoryViewModel
import com.example.whispry.presentation.main.HomeScreen
import com.example.whispry.presentation.presets.PresetsScreen
import com.example.whispry.presentation.settings.SettingsScreen
import com.example.whispry.presentation.settings.SettingsViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun WhispryNavHost(
    navController: NavHostController,
    globalGlassBackdrop: LayerBackdrop,
    settingsViewModel: SettingsViewModel,
    onShowLanguagePicker: () -> Unit,
    onRevisitTutorial: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(tween(200)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
        },
        exitTransition = {
            fadeOut(tween(200)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
        },
        popEnterTransition = {
            fadeIn(tween(200)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
        },
        popExitTransition = {
            fadeOut(tween(200)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300, easing = FastOutLinearInEasing)
                    )
        }
    ) {
        composable<Route.Home> {
            HomeScreen(backdrop = globalGlassBackdrop)
        }
        composable<Route.Library> {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                navController = navController,
                backdrop = globalGlassBackdrop ,
                onSearchActiveChange = onSearchActiveChange
            )
        }
        composable<Route.FavoriteDetails> {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryDetailScreen(
                title = "Favorites",
                isPinnedOnly = true,
                viewModel = viewModel,
                navController = navController,
                backdrop = globalGlassBackdrop,
                onSearchActiveChange = onSearchActiveChange
            )
        }
        composable<Route.RecentDetails> {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryDetailScreen(
                title = "Recents",
                isPinnedOnly = false,
                viewModel = viewModel,
                navController = navController,
                backdrop = globalGlassBackdrop,
                onSearchActiveChange = onSearchActiveChange
            )
        }
        composable<Route.Presets> {
            PresetsScreen(backdrop = globalGlassBackdrop)
        }
        composable<Route.Settings> {
            SettingsScreen(
                viewModel = settingsViewModel,
                backdrop = globalGlassBackdrop,
                onShowLanguagePicker = onShowLanguagePicker,
                onRevisitTutorial = onRevisitTutorial,
                onNavigateToTextExpander = { navController.navigate(Route.TextExpander) },
                onNavigateToAppTones = { navController.navigate(Route.AppTones) },
                onNavigateToMemory = { navController.navigate(Route.Memory) }
            )
        }
        composable<Route.About> {
            AboutScreen(backdrop = globalGlassBackdrop)
        }
        composable<Route.TextExpander> {
            val vm: TextExpanderViewModel = hiltViewModel()
            TextExpanderScreen(
                viewModel = vm,
                navController = navController,
                backdrop = globalGlassBackdrop
            )
        }
        composable<Route.AppTones> {
            val vm: AppToneViewModel = hiltViewModel()
            AppToneScreen(
                viewModel = vm,
                navController = navController,
                backdrop = globalGlassBackdrop
            )
        }
        composable<Route.Memory> {
            val vm: MemoryViewModel = hiltViewModel()
            MemoryScreen(
                viewModel = vm,
                navController = navController,
                backdrop = globalGlassBackdrop
            )
        }
    }
}
