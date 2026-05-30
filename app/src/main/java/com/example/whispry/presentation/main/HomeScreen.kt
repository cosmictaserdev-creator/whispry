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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.service.BubbleService
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun HomeScreen(
    backdrop: Backdrop,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    color = WhispryTokens.TextPrimary,
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
            LiquidButton(
                onClick = { /* Manual trigger */ },
                backdrop = backdrop,
                modifier = Modifier.size(160.dp),
                tint = WhispryTheme.colors.accent
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Record",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
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
                                if (state.missingPermissions.isEmpty()) WhispryTheme.colors.accent else Color.Gray,
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
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(state.recentTranscripts, key = { it.id }) { transcript ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(20.dp) },
                        effects = {
                            vibrancy()
                            blur(2.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.05f))
                        }
                    )
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

@Composable
fun StatCard(
    label: String,
    value: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.05f))
                }
            )
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
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(24.dp) },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color.copy(alpha = 0.15f))
                }
            )
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
