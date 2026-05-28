package com.example.whispry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.presentation.main.MainScreen
import com.example.whispry.presentation.main.MainViewModel
import com.example.whispry.presentation.onboarding.OnboardingNavGraph
import com.example.whispry.ui.theme.WhispryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        splashScreen.setKeepOnScreenCondition {
            viewModel.onboardingCompleted.value == null
        }

        setContent {
            val onboardingCompleted = viewModel.onboardingCompleted.collectAsStateWithLifecycle().value
            val onboardingStartDestination by viewModel.onboardingStartDestination.collectAsStateWithLifecycle()
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
            
            WhispryTheme(accentPreset = accentColor) {
                when (onboardingCompleted) {
                    true -> MainScreen(onRevisitTutorial = { viewModel.revisitTutorial() })
                    false -> OnboardingNavGraph(
                        onComplete = { viewModel.completeOnboarding() },
                        startDestination = onboardingStartDestination
                    )
                    null -> {}
                }
            }
        }
    }
}
