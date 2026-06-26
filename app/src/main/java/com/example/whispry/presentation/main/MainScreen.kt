package com.example.whispry.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.whispry.navigation.mainNavigationItems
import com.example.whispry.presentation.settings.LanguagePickerBottomSheet
import com.example.whispry.presentation.settings.SettingsIntent
import com.example.whispry.presentation.settings.SettingsViewModel
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.adaptive.LayoutMode
import com.example.whispry.ui.util.adaptive.LiquidNavigationRail
import com.example.whispry.ui.util.adaptive.RailNavigationItem
import com.example.whispry.ui.util.adaptive.currentLayoutMode
import com.example.whispry.ui.util.accentGlow
import com.example.whispry.ui.util.liquid.components.LiquidBottomTab
import com.example.whispry.ui.util.liquid.components.LiquidBottomTabs
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavDestination.Companion.hasRoute
import com.example.whispry.navigation.Route
import com.example.whispry.navigation.WhispryNavHost
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun MainScreen(
    onRevisitTutorial: () -> Unit,
    deepLinkRoute: Route? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    val glowAccent = WhispryTheme.colors.accent
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF121212))
        // Bake the accent glow into the backdrop so floating glass (the bottom bar and the
        // landscape rail) refracts it even where screen content isn't directly beneath them.
        val glowRadius = size.width * 0.7f
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(glowAccent.copy(alpha = 0.15f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = androidx.compose.ui.geometry.Offset(size.width, 0f)
        )
        drawContent()
    }

    val currentTabIndex = remember(currentDestination) {
        mainNavigationItems.indexOfFirst { item ->
            currentDestination?.hasRoute(item.route::class) == true
        }.let { if (it == -1) 0 else it }
    }

    var targetTabIndex by remember(currentTabIndex) { mutableIntStateOf(currentTabIndex) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }
    var isSearchActiveGlobal by remember { mutableStateOf(false) }

    val deepLinkHandled = remember { mutableStateOf(false) }
    LaunchedEffect(deepLinkRoute, deepLinkHandled.value) {
        if (deepLinkRoute != null && !deepLinkHandled.value) {
            deepLinkHandled.value = true
            navController.navigate(deepLinkRoute) {
                popUpTo<Route.Home> { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    var isNavbarVisible by remember { mutableStateOf(true) }
    val navbarScrollOffset by animateDpAsState(
        targetValue = if (isNavbarVisible && !isSearchActiveGlobal) 0.dp else 130.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
        label = "NavbarScrollOffset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f && isNavbarVisible) {
                    isNavbarVisible = false
                } else if (available.y > 25f && !isNavbarVisible) {
                    isNavbarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val layoutMode = currentLayoutMode()

    if (layoutMode != LayoutMode.PhonePortrait) {
        RailLayout(
            navController = navController,
            backdrop = backdrop,
            settingsViewModel = settingsViewModel,
            currentTabIndex = currentTabIndex,
            targetTabIndex = targetTabIndex,
            onTargetTabIndexChange = { targetTabIndex = it },
            showLanguagePicker = showLanguagePicker,
            onShowLanguagePickerChange = { showLanguagePicker = it },
            sheetProgress = sheetProgress,
            onSheetProgressChange = { sheetProgress = it },
            isSearchActiveGlobal = isSearchActiveGlobal,
            onSearchActiveChange = { isSearchActiveGlobal = it },
            onRevisitTutorial = onRevisitTutorial,
            deepLinkRoute = deepLinkRoute,
            settingsState = settingsState
        )
    } else {
        TabLayout(
            navController = navController,
            backdrop = backdrop,
            settingsViewModel = settingsViewModel,
            currentTabIndex = currentTabIndex,
            targetTabIndex = targetTabIndex,
            onTargetTabIndexChange = { targetTabIndex = it },
            showLanguagePicker = showLanguagePicker,
            onShowLanguagePickerChange = { showLanguagePicker = it },
            sheetProgress = sheetProgress,
            onSheetProgressChange = { sheetProgress = it },
            isSearchActiveGlobal = isSearchActiveGlobal,
            onSearchActiveChange = { isSearchActiveGlobal = it },
            onRevisitTutorial = onRevisitTutorial,
            deepLinkRoute = deepLinkRoute,
            settingsState = settingsState,
            navbarScrollOffset = navbarScrollOffset,
            nestedScrollConnection = nestedScrollConnection,
            isNavbarVisible = isNavbarVisible
        )
    }
}

@Composable
private fun TabLayout(
    navController: androidx.navigation.NavHostController,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    settingsViewModel: SettingsViewModel,
    currentTabIndex: Int,
    targetTabIndex: Int,
    onTargetTabIndexChange: (Int) -> Unit,
    showLanguagePicker: Boolean,
    onShowLanguagePickerChange: (Boolean) -> Unit,
    sheetProgress: Float,
    onSheetProgressChange: (Float) -> Unit,
    isSearchActiveGlobal: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onRevisitTutorial: () -> Unit,
    deepLinkRoute: Route?,
    settingsState: com.example.whispry.presentation.settings.SettingsState,
    navbarScrollOffset: androidx.compose.ui.unit.Dp,
    nestedScrollConnection: NestedScrollConnection,
    isNavbarVisible: Boolean
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) { innerPadding ->
        val themeAccent = WhispryTheme.colors.accent
        val padding = innerPadding

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .accentGlow(themeAccent)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .graphicsLayer {
                        val s = if (showLanguagePicker) 0.94f + (0.06f * (1f - sheetProgress)) else 1f
                        scaleX = s
                        scaleY = s
                    }
            ) {
                WhispryNavHost(
                    navController = navController,
                    globalGlassBackdrop = backdrop,
                    settingsViewModel = settingsViewModel,
                    onShowLanguagePicker = { onShowLanguagePickerChange(true) },
                    onRevisitTutorial = onRevisitTutorial,
                    onSearchActiveChange = onSearchActiveChange
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { translationY = navbarScrollOffset.toPx() }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = dimensionResource(com.example.whispry.R.dimen.spacing_lg),
                        end = dimensionResource(com.example.whispry.R.dimen.spacing_lg),
                        bottom = dimensionResource(com.example.whispry.R.dimen.spacing_sm)
                    )
                    .fillMaxWidth()
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { targetTabIndex },
                    tabsCount = mainNavigationItems.size,
                    backdrop = backdrop,
                    accentColor = themeAccent,
                    useGlass = settingsState.glassNavbar
                ) {
                    mainNavigationItems.forEachIndexed { index, item ->
                        LiquidBottomTab(
                            selected = currentTabIndex == index,
                            onClick = {
                                if (currentTabIndex != index) {
                                    onTargetTabIndexChange(index)
                                    navController.navigate(item.route) {
                                        popUpTo<Route.Home> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = item.icon,
                            filledIcon = item.filledIcon,
                            label = item.label
                        )
                    }
                }
            }

            if (showLanguagePicker) {
                val settingsStateData by settingsViewModel.state.collectAsStateWithLifecycle()

                LanguagePickerBottomSheet(
                    selectedLanguage = settingsStateData.language,
                    onLanguageSelected = {
                        settingsViewModel.onIntent(SettingsIntent.SetLanguage(it))
                        onShowLanguagePickerChange(false)
                    },
                    onDismiss = { onShowLanguagePickerChange(false) },
                    onDragProgress = { progress -> onSheetProgressChange(progress) },
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
private fun RailLayout(
    navController: androidx.navigation.NavHostController,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    settingsViewModel: SettingsViewModel,
    currentTabIndex: Int,
    targetTabIndex: Int,
    onTargetTabIndexChange: (Int) -> Unit,
    showLanguagePicker: Boolean,
    onShowLanguagePickerChange: (Boolean) -> Unit,
    sheetProgress: Float,
    onSheetProgressChange: (Float) -> Unit,
    isSearchActiveGlobal: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onRevisitTutorial: () -> Unit,
    deepLinkRoute: Route?,
    settingsState: com.example.whispry.presentation.settings.SettingsState
) {
    val themeAccent = WhispryTheme.colors.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Full-bleed accent glow behind everything.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .accentGlow(themeAccent)
        )

        // Content is inset from the rail so text/cards aren't clipped beneath it, while the
        // backdrop (with its baked-in glow) still gives the rail's glass something to refract.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Inset both horizontal edges so content clears the left system bar / cutout in
                // landscape; the end padding additionally reserves room for the floating rail.
                .windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Horizontal)
                )
                .padding(end = dimensionResource(com.example.whispry.R.dimen.rail_content_end_padding))
                .layerBackdrop(backdrop)
                .graphicsLayer {
                    val s = if (showLanguagePicker) 0.94f + (0.06f * (1f - sheetProgress)) else 1f
                    scaleX = s
                    scaleY = s
                }
        ) {
            WhispryNavHost(
                navController = navController,
                globalGlassBackdrop = backdrop,
                settingsViewModel = settingsViewModel,
                onShowLanguagePicker = { onShowLanguagePickerChange(true) },
                onRevisitTutorial = onRevisitTutorial,
                onSearchActiveChange = onSearchActiveChange
            )
        }

        // Floating rail on the right edge, overlaying the content the same way the bottom
        // bar floats over it in portrait. Clears the system bars and display cutout.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                .padding(
                    top = dimensionResource(com.example.whispry.R.dimen.spacing_md),
                    bottom = dimensionResource(com.example.whispry.R.dimen.spacing_md),
                    end = dimensionResource(com.example.whispry.R.dimen.spacing_sm)
                )
        ) {
            LiquidNavigationRail(
                selectedIndex = targetTabIndex,
                items = mainNavigationItems.map { item ->
                    RailNavigationItem(
                        label = item.label,
                        icon = item.icon,
                        filledIcon = item.filledIcon
                    )
                },
                backdrop = backdrop,
                accentColor = themeAccent,
                useGlass = settingsState.glassNavbar,
                onItemClick = { index ->
                    if (currentTabIndex != index) {
                        onTargetTabIndexChange(index)
                        navController.navigate(mainNavigationItems[index].route) {
                            popUpTo<Route.Home> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .width(dimensionResource(com.example.whispry.R.dimen.tablet_nav_rail_width))
                    .fillMaxHeight()
            )
        }

        if (showLanguagePicker) {
            val settingsStateData by settingsViewModel.state.collectAsStateWithLifecycle()

            LanguagePickerBottomSheet(
                selectedLanguage = settingsStateData.language,
                onLanguageSelected = {
                    settingsViewModel.onIntent(SettingsIntent.SetLanguage(it))
                    onShowLanguagePickerChange(false)
                },
                onDismiss = { onShowLanguagePickerChange(false) },
                onDragProgress = { progress -> onSheetProgressChange(progress) },
                backdrop = backdrop
            )
        }
    }
}
