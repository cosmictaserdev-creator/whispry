// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.navigation.Route
import com.example.whispry.presentation.main.MainScreen
import com.example.whispry.presentation.main.MainViewModel
import com.example.whispry.presentation.onboarding.OnboardingNavGraph
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.CachedGlassProvider
import com.example.whispry.ui.util.liquid.GlassBackdropCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var glassBackdropCache: GlassBackdropCache

    private var isUiVisible by mutableStateOf(true)
    private var deepLinkRoute: Route? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestHighestRefreshRate()
        parseDeepLink(intent)

        splashScreen.setKeepOnScreenCondition {
            viewModel.onboardingCompleted.value == null
        }

        setContent {
            if (isUiVisible) {
                val onboardingCompleted = viewModel.onboardingCompleted.collectAsStateWithLifecycle().value
                val onboardingStartDestinationState = viewModel.onboardingStartDestination.collectAsStateWithLifecycle()
                val onboardingStartDestination = onboardingStartDestinationState.value
                val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

                CachedGlassProvider(cache = glassBackdropCache) {
                    WhispryTheme(accentColors = accentColor) {
                        when (onboardingCompleted) {
                            true -> MainScreen(
                                onRevisitTutorial = { viewModel.revisitTutorial() },
                                deepLinkRoute = deepLinkRoute
                            )
                            false -> if (onboardingStartDestination != null) OnboardingNavGraph(
                                onComplete = { viewModel.completeOnboarding() },
                                startDestination = onboardingStartDestination
                            )
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parseDeepLink(intent)
    }

    /**
     * Opt the window into the panel's highest refresh rate (e.g. 120Hz). Without this, Compose
     * commonly renders capped at 60/90Hz even on a high-refresh display. Picks the fastest mode that
     * keeps the current resolution, so we never trade sharpness for frame rate.
     */
    private fun requestHighestRefreshRate() {
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        display ?: return
        val current = display.mode ?: return
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (best.modeId != current.modeId) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
        }
    }

    private fun parseDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "whispry") {
            deepLinkRoute = Route.fromDeepLinkHost(data.host ?: "")
        }
    }

    override fun onStart() {
        super.onStart()
        isUiVisible = true
        glassBackdropCache.init()
    }

    override fun onStop() {
        super.onStop()
        isUiVisible = false
        glassBackdropCache.release()
    }
}
