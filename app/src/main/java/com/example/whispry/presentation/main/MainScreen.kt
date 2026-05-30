package com.example.whispry.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.whispry.presentation.about.AboutScreen
import com.example.whispry.presentation.common.Screen
import com.example.whispry.presentation.history.HistoryDetailScreen
import com.example.whispry.presentation.history.HistoryScreen
import com.example.whispry.presentation.history.HistoryViewModel
import com.example.whispry.presentation.settings.LanguagePickerBottomSheet
import com.example.whispry.presentation.settings.SettingsIntent
import com.example.whispry.presentation.settings.SettingsScreen
import com.example.whispry.presentation.settings.SettingsViewModel
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.components.LiquidBottomTab
import com.example.whispry.ui.util.liquid.components.LiquidBottomTabs
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * MainScreen is the primary entry point for the application's main UI.
 */
@Composable
fun MainScreen(onRevisitTutorial: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val bgBackdrop = rememberLayerBackdrop {
        drawContent()
    }

    // Cache the background backdrop when navigating to Settings or Detail pages
    // where the background content is essentially static.
    val cachedBgBackdrop = remember(bgBackdrop) { bgBackdrop }

    val sceneBackdrop = rememberLayerBackdrop {
        drawContent()
    }

    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Settings,
        Screen.About
    )
    
    val currentTabIndex = remember(currentRoute) {
        screens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    var targetTabIndex by remember(currentTabIndex) { mutableIntStateOf(currentTabIndex) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }
    var isSearchActiveGlobal by remember { mutableStateOf(false) }

    val bottomBarOffset by animateDpAsState(
        targetValue = if (isSearchActiveGlobal) 100.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
        label = "BottomBarOffset"
    )

    Scaffold(
        content = { innerPadding ->
            val themeAccent = WhispryTheme.colors.accent
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val s = if (showLanguagePicker) 0.94f + (0.06f * (1f - sheetProgress)) else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .layerBackdrop(sceneBackdrop)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(bgBackdrop)
                            .background(Color.Black)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        0.7f to Color.Transparent,
                                        1.0f to themeAccent.copy(alpha = 0.12f)
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.fillMaxSize(),
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
                            composable(Screen.Home.route) {
                                HomeScreen(backdrop = bgBackdrop)
                            }
                            composable(Screen.Library.route) {
                                val viewModel: HistoryViewModel = hiltViewModel()
                                HistoryScreen(
                                    viewModel = viewModel, 
                                    navController = navController,
                                    backdrop = bgBackdrop,
                                    onSearchActiveChange = { isSearchActiveGlobal = it }
                                )
                            }
                            composable(Screen.FavoriteDetails.route) {
                                val viewModel: HistoryViewModel = hiltViewModel()
                                HistoryDetailScreen(
                                    title = "Favorites",
                                    isPinnedOnly = true,
                                    viewModel = viewModel,
                                    navController = navController,
                                    backdrop = bgBackdrop,
                                    onSearchActiveChange = { isSearchActiveGlobal = it }
                                )
                            }
                            composable(Screen.RecentDetails.route) {
                                val viewModel: HistoryViewModel = hiltViewModel()
                                HistoryDetailScreen(
                                    title = "Recents",
                                    isPinnedOnly = false,
                                    viewModel = viewModel,
                                    navController = navController,
                                    backdrop = bgBackdrop,
                                    onSearchActiveChange = { isSearchActiveGlobal = it }
                                )
                            }
                            composable(Screen.Settings.route) {
                                val viewModel: SettingsViewModel = hiltViewModel()
                                SettingsScreen(
                                    viewModel = viewModel, 
                                    backdrop = bgBackdrop,
                                    onShowLanguagePicker = { showLanguagePicker = true },
                                    onRevisitTutorial = onRevisitTutorial,
                                    trainedModelMatcher = viewModel.trainedModelMatcher
                                )
                            }
                            composable(Screen.About.route) {
                                AboutScreen(backdrop = bgBackdrop)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { translationY = bottomBarOffset.toPx() }
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .fillMaxWidth()
                ) {
                    LiquidBottomTabs(
                        selectedTabIndex = { targetTabIndex },
                        tabsCount = screens.size,
                        backdrop = sceneBackdrop,
                        accentColor = themeAccent
                    ) {
                        screens.forEachIndexed { index, screen ->
                            LiquidBottomTab(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        targetTabIndex = index
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = screen.icon ?: Screen.Home.icon!!,
                                filledIcon = screen.filledIcon ?: screen.icon ?: Screen.Home.icon!!,
                                label = screen.label
                            )
                        }
                    }
                }

                if (showLanguagePicker) {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
                    
                    LanguagePickerBottomSheet(
                        selectedLanguage = settingsState.language,
                        onLanguageSelected = { 
                            settingsViewModel.onIntent(SettingsIntent.SetLanguage(it))
                            showLanguagePicker = false
                        },
                        onDismiss = { showLanguagePicker = false },
                        onDragProgress = { progress ->
                            sheetProgress = progress
                        },
                        backdrop = sceneBackdrop
                    )
                }
            }
        }
    )
}
