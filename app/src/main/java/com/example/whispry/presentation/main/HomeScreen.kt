package com.example.whispry.presentation.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whispry.R
import com.example.whispry.ui.util.adaptive.currentWidthSizeClass
import com.example.whispry.ui.util.adaptive.gridColumnsFor
import com.example.whispry.ui.util.gridItems
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.service.BubbleService
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.kyant.backdrop.Backdrop

@Composable
fun HomeScreen(
    backdrop: LayerBackdrop,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val gridColumns = gridColumnsFor(currentWidthSizeClass())
    val gridSpacing = dimensionResource(R.dimen.spacing_sm)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
        label = "MicScale"
    )

    var hasPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            hasPressed = true
            viewModel.onIntent(HomeIntent.StartManualRecording)
        } else if (hasPressed) {
            viewModel.onIntent(HomeIntent.StopManualRecording)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(HomeIntent.CheckPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .fillMaxWidth(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            bottom = 140.dp, 
            start = 24.dp, 
            end = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Whispry",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
        }

        // Service Status Banners
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Accessibility Revoked Banner (Red)
                AnimatedVisibility(
                    visible = state.serviceState == ServiceState.Stopped && state.missingPermissions.contains("Accessibility"),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StatusBanner(
                        title = "Accessibility disabled",
                        description = "Whispry won't work without it",
                        actionLabel = "Fix",
                        color = Color.Red,
                        icon = Icons.Rounded.ErrorOutline,
                        backdrop = backdrop,
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }

                // Service Stopped Banner (Amber)
                AnimatedVisibility(
                    visible = state.serviceState == ServiceState.Stopped && !state.missingPermissions.contains("Accessibility"),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StatusBanner(
                        title = "Service stopped",
                        description = "Tap to restart the voice trigger",
                        actionLabel = "Restart",
                        color = Color(0xFFFFA000), // Amber
                        icon = Icons.Rounded.PowerSettingsNew,
                        backdrop = backdrop,
                        onClick = {
                            val intent = Intent(context, BubbleService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    )
                }

                // General Permissions Warning
                AnimatedVisibility(
                    visible = state.missingPermissions.any { it != "Accessibility" },
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StatusBanner(
                        title = "Action Required",
                        description = "Missing: ${state.missingPermissions.filter { it != "Accessibility" }.joinToString(", ")}",
                        actionLabel = "Grant",
                        color = WhispryTokens.ErrorSoft,
                        icon = Icons.Rounded.ErrorOutline,
                        backdrop = backdrop,
                        onClick = {
                            // Link to app settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // Hero Mic Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                LiquidButton(
                    onClick = { /* handled by interactionSource */ },
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxSize(),
                    tint = WhispryTheme.colors.accent,
                    interactionSource = interactionSource
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Record",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Status Chip
        item {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (state.missingPermissions.isEmpty()) WhispryTokens.SuccessGreen else Color.Gray,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.missingPermissions.isEmpty()) "Service Active" else "Service Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Stats Row
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Transcripts",
                    value = state.totalTranscripts.toString(),
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Words",
                    value = state.totalWords.toString(),
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recent Transcripts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }

        gridItems(
            items = state.recentTranscripts,
            columns = gridColumns,
            horizontalSpacing = gridSpacing
        ) { transcript ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = transcript.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transcript.createdAtFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
}

@Composable
fun StatCard(
    label: String,
    value: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun StatusBanner(
    title: String,
    description: String,
    actionLabel: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backdrop: LayerBackdrop,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(24.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Text(
                text = actionLabel,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
