package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.service.OemBatteryOptimization
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

@Composable
fun PermissionsScreen(
    state: OnboardingState,
    onGrantMic: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit,
    backdrop: Backdrop
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tighten the vertical gutters on short screens so all permission cards fit with less
        // scrolling.
        val compact = maxHeight < 640.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(if (compact) 32.dp else 64.dp))

                StaggeredTextReveal(
                    text = "A few quick things.",
                    style = TextStyle(
                        color = WhispryTokens.TextPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredTextReveal(
                    text = "Whispry is a hands-free voice input tool. Microphone and Accessibility are required so you can talk-to-type in any app. The bubble is optional.",
                    style = TextStyle(
                        color = WhispryTokens.TextSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 26.sp
                    ),
                    delayMs = 200
                )

                Spacer(modifier = Modifier.height(if (compact) 16.dp else 32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionCard(
                        title = "Microphone",
                        description = "To capture and transcribe your voice",
                        isGranted = state.micPermissionGranted,
                        onClick = onGrantMic,
                        delayMs = 400,
                        icon = Icons.Rounded.Mic,
                        backdrop = backdrop,
                        isRequired = true
                    )

                    PermissionCard(
                        title = "Accessibility",
                        description = "So a volume-key press starts recording and your text is pasted into the field you're typing in. Whispry only reads the volume keys and the focused text field — nothing else.",
                        isGranted = state.accessibilityEnabled,
                        onClick = onGrantAccessibility,
                        delayMs = 500,
                        icon = Icons.Rounded.Gesture,
                        backdrop = backdrop,
                        isRequired = true
                    )

                    PermissionCard(
                        title = "Bubble (optional)",
                        description = "Shows a floating recording bubble with a live waveform. Skip it and voice-to-text still works — you'll just get sound and haptics instead of the visual bubble.",
                        isGranted = state.overlayPermissionGranted,
                        onClick = onGrantOverlay,
                        delayMs = 600,
                        icon = Icons.Rounded.Layers,
                        backdrop = backdrop,
                        isRequired = false
                    )

                    if (OemBatteryOptimization.shouldShowPrompt(context)) {
                        PermissionCard(
                            title = "Protect from battery killers",
                            description = "Some phones (Realme, Poco, Oppo, Xiaomi and more) aggressively kill background services, which can stop Whispry mid-recording. Tap Allow to open the battery settings and exempt Whispry.",
                            isGranted = false,
                            onClick = {
                                OemBatteryOptimization.getSettingsIntent(context)?.let { context.startActivity(it) }
                            },
                            delayMs = 700,
                            icon = Icons.Rounded.BatterySaver,
                            backdrop = backdrop,
                            isRequired = false
                        )
                    }
                }
            }

            Column {
                LiquidButton(
                    onClick = { if (state.allPermissionsGranted) onContinue() },
                    enabled = state.allPermissionsGranted,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text(
                        if (state.allPermissionsGranted) "Continue" else "Permissions Required",
                        color = if (state.allPermissionsGranted) androidx.compose.ui.graphics.Color.White else Color.White.copy(0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(if (compact) 24.dp else 48.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    delayMs: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backdrop: Backdrop,
    isRequired: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }

    val offset by animateDpAsState(
        targetValue = if (visible) 0.dp else 40.dp,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.8f),
        label = "CardOffset"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "CardAlpha"
    )

    GlassCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .graphicsLayer {
                translationX = offset.toPx()
                this.alpha = alpha
            },
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = WhispryTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRequired && !isGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "REQUIRED",
                                color = Color(0xFFFF5252),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    color = WhispryTokens.TextTertiary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 5
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = WhispryTokens.SuccessGreen,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Allow",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
