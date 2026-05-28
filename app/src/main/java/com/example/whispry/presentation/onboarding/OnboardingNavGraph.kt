package com.example.whispry.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.whispry.presentation.onboarding.components.WhispryBackground
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun OnboardingNavGraph(
    onComplete: () -> Unit,
    startDestination: String = "intro",
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val config = LocalConfiguration.current
    
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Screen-specific optimal background positions
    val glowPosition = remember(currentRoute, config.screenWidthDp, config.screenHeightDp) {
        val width = config.screenWidthDp.dp.value
        val height = config.screenHeightDp.dp.value
        when (currentRoute) {
            "intro" -> Offset(width / 2, height / 2)
            "welcome" -> Offset(width * 0.15f, height * 0.2f)
            "permissions" -> Offset(width * 0.85f, height * 0.8f)
            "apiKey" -> Offset(width * 0.85f, height * 0.2f)
            "howItWorks" -> Offset(width * 0.5f, height * 0.85f)
            "tutorial" -> Offset(width * 0.5f, height * 0.5f)
            else -> Offset(width / 2, height / 2)
        }
    }

    val glowIntensityValue = remember(currentRoute) {
        when (currentRoute) {
            "intro" -> 0.4f
            "welcome" -> 0.5f
            "permissions" -> 0.45f
            "apiKey" -> 0.55f
            "howItWorks" -> 0.5f
            "tutorial" -> 0.7f
            else -> 0.4f
        }
    }

    val animatedGlowOffset by animateOffsetAsState(
        targetValue = glowPosition,
        animationSpec = spring(stiffness = 40f, dampingRatio = 0.75f),
        label = "BackgroundGlowOffset"
    )

    val animatedGlowIntensity by animateFloatAsState(
        targetValue = glowIntensityValue,
        animationSpec = tween(1200, easing = EaseInOutSine),
        label = "BackgroundGlowIntensity"
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.checkPermissions()
        }
    )

    val onboardingBackdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(onboardingBackdrop)
        ) {
            WhispryBackground(
                glowIntensity = animatedGlowIntensity,
                glowOffset = animatedGlowOffset,
                particleAlpha = 0.3f
            )
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(600)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(stiffness = 250f, dampingRatio = 0.85f),
                    initialOffset = { it / 4 }
                )
            },
            exitTransition = {
                fadeOut(tween(300)) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                )
            }
        ) {
            composable("intro") {
                IntroScreen(
                    onTransition = {
                        navController.navigate("welcome")
                    },
                    backdrop = onboardingBackdrop
                )
            }

            composable("welcome") {
                WelcomeScreen(
                    onContinue = {
                        navController.navigate("permissions")
                    },
                    backdrop = onboardingBackdrop
                )
            }

            composable("permissions") {
                PermissionsScreen(
                    state = state,
                    onGrantMic = { 
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onGrantOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onGrantAccessibility = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onContinue = {
                        navController.navigate("apiKey")
                    },
                    onRefresh = { viewModel.checkPermissions() },
                    backdrop = onboardingBackdrop
                )
            }

            composable("apiKey") {
                ApiKeyScreen(
                    state = state,
                    onApiKeyChange = { viewModel.updateApiKey(it) },
                    onValidate = { viewModel.validateAndSaveApiKey() },
                    onGetApiKey = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                        context.startActivity(intent)
                    },
                    onComplete = {
                        navController.navigate("howItWorks")
                    },
                    backdrop = onboardingBackdrop
                )
            }

            composable("howItWorks") {
                // Permission Guard: Redirect to permissions if missing
                LaunchedEffect(state.allPermissionsGranted) {
                    if (!state.allPermissionsGranted) {
                        navController.navigate("permissions") {
                            popUpTo("howItWorks") { inclusive = true }
                        }
                    }
                }

                HowItWorksScreen(
                    onContinue = {
                        navController.navigate("tutorial")
                    },
                    backdrop = onboardingBackdrop
                )
            }

            composable("tutorial") {
                // Permission Guard: Redirect to permissions if missing
                LaunchedEffect(state.allPermissionsGranted) {
                    if (!state.allPermissionsGranted) {
                        navController.navigate("permissions") {
                            popUpTo("tutorial") { inclusive = true }
                        }
                    }
                }

                TutorialScreen(
                    state = state,
                    onStart = { viewModel.startTutorial() },
                    onContinue = {
                        viewModel.completeOnboarding()
                    },
                    backdrop = onboardingBackdrop
                )

                LaunchedEffect(state.isCompleted) {
                    if (state.isCompleted) {
                        onComplete()
                    }
                }
            }
        }
    }
}
