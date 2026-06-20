package com.example.whispry.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.whispry.presentation.settings.LanguagePickerBottomSheet
import com.example.whispry.presentation.settings.SettingsIntent
import com.example.whispry.presentation.settings.SettingsViewModel
import com.example.whispry.ui.theme.WhispryTheme
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
import com.example.whispry.navigation.mainNavigationItems
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * MainScreen is the primary entry point for the application's main UI.
 */
@Composable
fun MainScreen(onRevisitTutorial: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    // 1. Initialize the Backdrop State
    val backdrop = rememberLayerBackdrop { 
        // Draw a solid background color first to avoid transparent pixels
        drawRect(Color(0xFF121212)) 
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

    // --- Hide on Scroll Logic ---
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
            
            // Decorative background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val radius = size.width * 0.7f
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(themeAccent.copy(alpha = 0.15f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                radius = radius
                            ),
                            radius = radius,
                            center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                        )
                    }
            )
            
            // 2. The content to be captured (the background)
            Box(
                modifier = Modifier
                    .fillMaxSize() 
                    .layerBackdrop(backdrop) // Capture ONLY this layer
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
                    onShowLanguagePicker = { showLanguagePicker = true },
                    onRevisitTutorial = onRevisitTutorial,
                    onSearchActiveChange = { isSearchActiveGlobal = it }
                )
            }

            // 3. The Glass Bottom Bar (Consume blur)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { 
                        translationY = navbarScrollOffset.toPx() 
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
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
                                    targetTabIndex = index
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

            // 4. Overlays (Modals)
            if (showLanguagePicker) {
                val settingsStateData by settingsViewModel.state.collectAsStateWithLifecycle()
                
                LanguagePickerBottomSheet(
                    selectedLanguage = settingsStateData.language,
                    onLanguageSelected = { 
                        settingsViewModel.onIntent(SettingsIntent.SetLanguage(it))
                        showLanguagePicker = false
                    },
                    onDismiss = { showLanguagePicker = false },
                    onDragProgress = { progress -> sheetProgress = progress },
                    backdrop = backdrop
                )
            }
        }
    }
}
