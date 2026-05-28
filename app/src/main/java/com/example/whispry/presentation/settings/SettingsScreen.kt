package com.example.whispry.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.example.whispry.ui.util.liquid.components.LiquidSlider
import com.example.whispry.ui.util.liquid.components.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    backdrop: Backdrop,
    onShowLanguagePicker: () -> Unit,
    onRevisitTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(SettingsIntent.RefreshStatus)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            start = 24.dp, 
            end = 24.dp
        )
    ) {
        item {
            Box(modifier = Modifier.animateItem()) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "Voice Recognition", backdrop = backdrop) {
                    ApiKeyField(
                        apiKey = state.apiKey,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateApiKey(it)) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingsRow(
                        icon = Icons.Rounded.Language,
                        title = "Language",
                        value = state.language.uppercase(),
                        onClick = onShowLanguagePicker
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LiquidSettingsSlider(
                        title = "Temperature",
                        value = state.temperature,
                        onValueChange = { viewModel.onIntent(SettingsIntent.SetTemperature(it)) },
                        valueRange = 0f..1f,
                        backdrop = backdrop,
                        startLabel = "Precise",
                        endLabel = "Creative"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "Appearance", backdrop = backdrop) {
                    AccentColorSelector(
                        selectedPreset = AccentPreset.entries.find { it.name == state.accentColor } ?: AccentPreset.Purple,
                        onPresetSelected = { viewModel.onIntent(SettingsIntent.SetAccentColor(it.name)) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "Trigger", backdrop = backdrop) {
                    LiquidSettingsSlider(
                        title = "Double Press Interval",
                        value = state.doublePressInterval.toFloat(),
                        onValueChange = { viewModel.onIntent(SettingsIntent.SetDoublePressInterval(it.toLong())) },
                        valueRange = 200f..600f,
                        steps = 7, // 50ms increments
                        backdrop = backdrop,
                        valueLabel = "${state.doublePressInterval}ms"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LiquidSettingsToggle(
                        icon = Icons.Rounded.Vibration,
                        title = "Haptic Feedback",
                        checked = state.hapticFeedback,
                        onCheckedChange = { viewModel.onIntent(SettingsIntent.SetHapticFeedback(it)) },
                        backdrop = backdrop
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "Service", backdrop = backdrop) {
                    StatusRow(
                        title = "Accessibility Service",
                        status = if (state.isAccessibilityEnabled) "Running" else "Disabled - Action Required",
                        isRunning = state.isAccessibilityEnabled,
                        onClick = { viewModel.onIntent(SettingsIntent.OpenAccessibilitySettings) },
                        backdrop = backdrop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LiquidSettingsToggle(
                        icon = Icons.Rounded.PowerSettingsNew,
                        title = "Auto-start on Boot",
                        checked = state.autoStartBoot,
                        onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAutoStartBoot(it)) },
                        backdrop = backdrop
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "Tutorial", backdrop = backdrop) {
                    SettingsRow(
                        icon = Icons.Rounded.HelpOutline,
                        title = "Revisit Tutorial",
                        value = "Show guide again",
                        onClick = onRevisitTutorial
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.animateItem()) {
                SettingsSection(title = "About", backdrop = backdrop) {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        value = "1.0.0 (Build 12)",
                        showChevron = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun SliderMeter(
    steps: Int,
    range: ClosedFloatingPointRange<Float>,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().height(20.dp).padding(horizontal = 4.dp)) {
        val totalSteps = if (steps > 0) steps + 2 else 11 // Default 10 segments
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { i ->
                val stepValue = range.start + (range.endInclusive - range.start) * (i.toFloat() / (totalSteps - 1))
                val isSelected = if (steps > 0) {
                    kotlin.math.abs(stepValue - currentValue) < 0.001f
                } else {
                    false
                }
                
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(if (i % (if (steps > 0) 1 else 5) == 0) 6.dp else 3.dp)
                        .background(
                            if (isSelected) WhispryTheme.colors.accent 
                            else Color.White.copy(alpha = if (i % (if (steps > 0) 1 else 5) == 0) 0.15f else 0.08f),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    backdrop: Backdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = WhispryTheme.colors.accent,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        if (showChevron) {
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun LiquidSettingsToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop
        )
    }
}

@Composable
fun LiquidSettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    backdrop: Backdrop,
    startLabel: String? = null,
    endLabel: String? = null,
    valueLabel: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (valueLabel != null) {
                Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = WhispryTheme.colors.accent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LiquidSlider(
            value = { value },
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            visibilityThreshold = 0.01f,
            backdrop = backdrop
        )
        Spacer(modifier = Modifier.height(4.dp))
        SliderMeter(
            steps = steps,
            range = valueRange,
            currentValue = value
        )
        if (startLabel != null && endLabel != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(startLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                Text(endLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun ApiKeyField(
    apiKey: String,
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onValueChange,
            label = { Text("Groq API Key", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) "Hide API Key" else "Show API Key",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WhispryTheme.colors.accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun AccentColorSelector(
    selectedPreset: AccentPreset,
    onPresetSelected: (AccentPreset) -> Unit
) {
    Column {
        Text(
            text = "Accent Color",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val cardWidth = screenWidth - 48.dp
        
        LazyRow(
            modifier = Modifier
                .requiredWidth(cardWidth),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
        ) {
            items(AccentPreset.entries) { preset ->
                val isSelected = preset == selectedPreset
                
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "ColorScale"
                )
                
                val contentDistortion by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
                    label = "ContentDistortion"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = 1f / scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onPresetSelected(preset) }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(preset.mainColor)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = contentDistortion
                                        scaleY = contentDistortion
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = preset.label.split(" ").last(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.graphicsLayer {
                            scaleX = contentDistortion
                            alpha = contentDistortion
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(
    title: String,
    status: String,
    isRunning: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isRunning) Color.Transparent 
                else Color(0xFFFF5252).copy(alpha = 0.05f)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(with(density) { 8.dp.toPx() })
                    },
                    onDrawSurface = {
                        val color = if (isRunning) Color.White.copy(alpha = 0.1f)
                        else Color(0xFFFF5252).copy(alpha = 0.2f)
                        drawRect(color)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isRunning) Color.White.copy(alpha = 0.1f) 
                            else Color(0xFFFF5252).copy(alpha = pulseAlpha),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.VerifiedUser else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (isRunning) Color(0xFF00E676) else Color(0xFFFF5252).copy(alpha = pulseAlpha),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) Color(0xFF00E676).copy(alpha = 0.8f) 
                        else Color(0xFFFF5252).copy(alpha = pulseAlpha)
            )
        }

        LiquidButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = Modifier.height(36.dp),
            tint = if (isRunning) Color.White.copy(alpha = 0.08f) else Color(0xFFFF5252)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                if (!isRunning) {
                    Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    if (isRunning) "Settings" else "Enable Now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
