package com.example.whispry.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.domain.model.RetentionPolicy
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.service.TriggerSound
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.WhispryTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.whispry.ui.util.adaptive.MasterDetailScaffold
import com.example.whispry.ui.util.adaptive.currentWidthSizeClass
import com.example.whispry.ui.util.adaptive.masterDetailEnabledFor
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.example.whispry.ui.util.liquid.components.LiquidSlider
import com.example.whispry.ui.util.liquid.components.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings categories. Drives the tablet/Expanded master-detail: the list on the left,
 * the selected category's section rendered on the right. On phones every section is shown
 * inline in one scroll and the category is implicit.
 */
enum class SettingsCategory(val title: String, val icon: ImageVector) {
    Voice("Voice Recognition", Icons.Rounded.Language),
    Trigger("Trigger Method", Icons.Rounded.TouchApp),
    Interface("Interface & Sounds", Icons.Rounded.Palette),
    Productivity("Productivity", Icons.Rounded.AutoAwesome),
    Data("Data & Privacy", Icons.Rounded.Security),
    Service("Service & Maintenance", Icons.Rounded.Build),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onShowLanguagePicker: () -> Unit,
    onRevisitTutorial: () -> Unit,
    onNavigateToTextExpander: () -> Unit = {},
    onNavigateToAppTones: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var showRetentionPicker by remember { mutableStateOf(false) }
    val isMasterDetail = masterDetailEnabledFor(currentWidthSizeClass())
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.Voice) }

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

    Box(modifier = modifier.fillMaxSize()) {
        if (isMasterDetail) {
            // Tablet / Expanded: fixed category list on the left, selected detail on the right.
            MasterDetailScaffold(
                masterPaneWidth = 300.dp,
                detailPaneWidth = null,
                master = {
                    SettingsCategoryList(
                        selectedCategory = selectedCategory,
                        onSelect = { selectedCategory = it },
                        footer = { SettingsFooter(viewModel = viewModel, backdrop = backdrop) }
                    )
                },
                detail = {
                    SettingsDetailPane(
                        category = selectedCategory,
                        state = state,
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onShowLanguagePicker = onShowLanguagePicker,
                        onRevisitTutorial = onRevisitTutorial,
                        onNavigateToTextExpander = onNavigateToTextExpander,
                        onNavigateToAppTones = onNavigateToAppTones,
                        onNavigateToMemory = onNavigateToMemory,
                        onShowRetentionPicker = { showRetentionPicker = true }
                    )
                }
            )
        } else {
            // Phone: every section inline in one readable-width scroll.
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                item { VoiceRecognitionSection(state, viewModel, backdrop, onShowLanguagePicker) }
                item { TriggerMethodSection(state, viewModel, backdrop) }
                item { InterfaceSoundsSection(state, viewModel, backdrop) }
                item {
                    ProductivitySection(
                        state, viewModel, backdrop,
                        onNavigateToTextExpander, onNavigateToAppTones, onNavigateToMemory
                    )
                }
                item { DataPrivacySection(state, viewModel, backdrop) { showRetentionPicker = true } }
                item { ServiceMaintenanceSection(state, viewModel, backdrop, onRevisitTutorial) }
                item { SettingsFooter(viewModel = viewModel, backdrop = backdrop) }
            }
        }

        if (showRetentionPicker) {
            RetentionPolicyBottomSheet(
                currentPolicy = state.retentionPolicy,
                onPolicySelected = {
                    viewModel.onIntent(SettingsIntent.SetRetentionPolicy(it))
                    showRetentionPicker = false
                },
                onDismiss = { showRetentionPicker = false },
                backdrop = backdrop
            )
        }
    }
}

/** Master pane: the tappable list of settings categories plus the global footer. */
@Composable
private fun SettingsCategoryList(
    selectedCategory: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
    footer: @Composable () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            bottom = 140.dp,
            start = 16.dp,
            end = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
        }
        items(SettingsCategory.entries) { category ->
            SettingsCategoryRow(
                category = category,
                selected = category == selectedCategory,
                onClick = { onSelect(category) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            footer()
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            category.icon,
            null,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            category.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = Color.White.copy(alpha = if (selected) 0.4f else 0.15f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Detail pane: the selected category's section, scrollable. */
@Composable
private fun SettingsDetailPane(
    category: SettingsCategory,
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onShowLanguagePicker: () -> Unit,
    onRevisitTutorial: () -> Unit,
    onNavigateToTextExpander: () -> Unit,
    onNavigateToAppTones: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onShowRetentionPicker: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            bottom = 140.dp,
            start = 8.dp,
            end = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            when (category) {
                SettingsCategory.Voice ->
                    VoiceRecognitionSection(state, viewModel, backdrop, onShowLanguagePicker)
                SettingsCategory.Trigger ->
                    TriggerMethodSection(state, viewModel, backdrop)
                SettingsCategory.Interface ->
                    InterfaceSoundsSection(state, viewModel, backdrop)
                SettingsCategory.Productivity ->
                    ProductivitySection(
                        state, viewModel, backdrop,
                        onNavigateToTextExpander, onNavigateToAppTones, onNavigateToMemory
                    )
                SettingsCategory.Data ->
                    DataPrivacySection(state, viewModel, backdrop, onShowRetentionPicker)
                SettingsCategory.Service ->
                    ServiceMaintenanceSection(state, viewModel, backdrop, onRevisitTutorial)
            }
        }
    }
}

@Composable
private fun VoiceRecognitionSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onShowLanguagePicker: () -> Unit
) {
    SettingsSectionOptimized(title = "Voice Recognition", backdrop = backdrop) {
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

@Composable
private fun TriggerMethodSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    SettingsSectionOptimized(title = "Trigger Method", backdrop = backdrop) {
        TriggerPickerSection(
            state = state,
            onIntent = { viewModel.onIntent(it) },
            backdrop = backdrop
        )

        if (state.triggerMode is TriggerMode.VolumeButton) {
            Spacer(modifier = Modifier.height(16.dp))
            VolumeKeyToggle(
                selectedKey = state.triggerVolumeKey,
                onKeySelected = { viewModel.onIntent(SettingsIntent.SetTriggerVolumeKey(it)) },
                backdrop = backdrop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DuckingSection(
            enabled = state.duckingEnabled,
            duckPercent = state.duckPercent,
            onEnabledChanged = { viewModel.onIntent(SettingsIntent.SetDuckingEnabled(it)) },
            onPercentChanged = { viewModel.onIntent(SettingsIntent.SetDuckingPercent(it)) },
            backdrop = backdrop
        )
    }
}

@Composable
private fun InterfaceSoundsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    SettingsSectionOptimized(title = "Interface & Sounds", backdrop = backdrop) {
        LiquidSettingsToggle(
            icon = Icons.Rounded.OpenInNew,
            title = "Floating Widget",
            checked = state.floatingWidgetEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetFloatingWidgetEnabled(it)) },
            backdrop = backdrop
        )

        Spacer(modifier = Modifier.height(16.dp))

        LiquidSettingsToggle(
            icon = Icons.Rounded.BlurOn,
            title = "Glass Navbar",
            checked = state.glassNavbar,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetGlassNavbar(it)) },
            backdrop = backdrop
        )

        Spacer(modifier = Modifier.height(16.dp))

        AccentColorSelector(
            selectedPreset = AccentPreset.entries.find { it.name == state.accentColor } ?: AccentPreset.Purple,
            onPresetSelected = { viewModel.onIntent(SettingsIntent.SetAccentColor(it.name)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LiquidSettingsToggle(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = "Trigger Sounds",
            checked = state.soundEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetSoundEnabled(it)) },
            backdrop = backdrop
        )

        AnimatedVisibility(
            visible = state.soundEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                SoundSelectorRow(
                    title = "Sound Pack",
                    selectedSound = state.selectedSound,
                    onSoundSelected = { viewModel.onIntent(SettingsIntent.SetSoundPack(it)) }
                )
            }
        }
    }
}

@Composable
private fun ProductivitySection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onNavigateToTextExpander: () -> Unit,
    onNavigateToAppTones: () -> Unit,
    onNavigateToMemory: () -> Unit
) {
    SettingsSectionOptimized(title = "Productivity", backdrop = backdrop) {
        SettingsRow(
            icon = Icons.Rounded.TextFields,
            title = "Text Expander",
            value = "Shortcuts → full text",
            onClick = onNavigateToTextExpander
        )

        Spacer(modifier = Modifier.height(16.dp))

        LiquidSettingsToggle(
            icon = Icons.Rounded.AutoAwesome,
            title = "App-Aware Tones",
            checked = state.appAwareToneEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAppAwareToneEnabled(it)) },
            backdrop = backdrop
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            icon = Icons.Rounded.Memory,
            title = "Memory Bank",
            value = "Personalize your context",
            onClick = onNavigateToMemory
        )

        AnimatedVisibility(
            visible = state.appAwareToneEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.SettingsApplications,
                    title = "Configure App Tones",
                    value = "Map apps to presets",
                    onClick = onNavigateToAppTones
                )
            }
        }
    }
}

@Composable
private fun DataPrivacySection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onShowRetentionPicker: () -> Unit
) {
    SettingsSectionOptimized(title = "Data & Privacy", backdrop = backdrop) {
        SettingsRow(
            icon = Icons.Rounded.History,
            title = "Retention Policy",
            value = state.retentionPolicy.displayName,
            onClick = onShowRetentionPicker
        )

        Spacer(modifier = Modifier.height(16.dp))

        LiquidButton(
            onClick = { viewModel.onIntent(SettingsIntent.ClearAudioCache) },
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            tint = Color.White.copy(alpha = 0.05f)
        ) {
            Text("Clear audio cache", color = Color.White.copy(alpha = 0.8f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        HoldToConfirmButton(
            text = "Clear all transcripts",
            onConfirmed = { viewModel.onIntent(SettingsIntent.ClearAllTranscripts) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ServiceMaintenanceSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop,
    onRevisitTutorial: () -> Unit
) {
    SettingsSectionOptimized(title = "Service & Maintenance", backdrop = backdrop) {
        StatusRow(
            title = "Accessibility Service",
            status = if (state.isAccessibilityEnabled) "Running" else "Disabled",
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

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            icon = Icons.Rounded.HelpOutline,
            title = "Revisit Tutorial",
            value = "Show guide",
            onClick = onRevisitTutorial
        )
    }
}

@Composable
private fun SettingsFooter(
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(SettingsIntent.ResetToDefaults)
                    showResetDialog = false
                }) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Reset to Defaults?") },
            text = { Text("This will clear all settings and your API key.") },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF1A1A1A),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }

    LiquidButton(
        onClick = { showResetDialog = true },
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        tint = Color.White.copy(alpha = 0.05f)
    ) {
        Text("Reset to defaults", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Version 1.2.0",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(40.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionPolicyBottomSheet(
    currentPolicy: RetentionPolicy,
    onPolicySelected: (RetentionPolicy) -> Unit,
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D0D14),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Data Retention",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Pinned transcripts are never deleted automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            RetentionPolicy.entries.forEach { policy ->
                val isSelected = policy == currentPolicy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) else Color.Transparent)
                        .clickable { onPolicySelected(policy) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            policy.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else Color.White
                        )
                        Text(
                            policy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun VolumeKeyToggle(
    selectedKey: String,
    onKeySelected: (String) -> Unit,
    backdrop: Backdrop
) {
    val themeAccent = androidx.compose.ui.graphics.Color.White

    Column {
        Text(
            text = "Trigger Key",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            val options = listOf("VOLUME_DOWN" to "Volume Down ↓", "VOLUME_UP" to "Volume Up ↑")
            options.forEach { (key, label) ->
                val isSelected = selectedKey == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) themeAccent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onKeySelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) themeAccent else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Crossfade(targetState = selectedKey, label = "TriggerDesc") { key ->
            Text(
                text = if (key == "VOLUME_UP") "Double press and hold ↑ to record" else "Double press and hold ↓ to record",
                style = MaterialTheme.typography.labelSmall,
                color = themeAccent.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DuckingSection(
    enabled: Boolean,
    duckPercent: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onPercentChanged: (Int) -> Unit,
    backdrop: Backdrop
) {
    Column {
        LiquidSettingsToggle(
            icon = Icons.Rounded.MusicNote,
            title = "Lower music while recording",
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            backdrop = backdrop
        )

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Volume reduction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "$duckPercent%",
                        style = MaterialTheme.typography.labelLarge,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LiquidSlider(
                    value = { duckPercent / 100f },
                    onValueChange = { onPercentChanged((it * 100).toInt()) },
                    valueRange = 0f..1f,
                    steps = 9,
                    visibilityThreshold = 0.01f,
                    backdrop = backdrop
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("No change", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                    Text("Mute fully", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun HoldToConfirmButton(
    text: String,
    holdDurationMs: Long = 1500L,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        val startTime = System.currentTimeMillis()
                        val job = scope.launch {
                            while (isHolding) {
                                val elapsed = System.currentTimeMillis() - startTime
                                holdProgress = (elapsed / holdDurationMs.toFloat()).coerceIn(0f, 1f)
                                if (holdProgress >= 1f) {
                                    onConfirmed()
                                    isHolding = false
                                    holdProgress = 0f
                                    break
                                }
                                delay(16)
                            }
                        }
                        tryAwaitRelease()
                        isHolding = false
                        holdProgress = 0f
                        job.cancel()
                    }
                )
            }
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFFF5252).copy(alpha = 0.1f),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                if (holdProgress > 0f) {
                    drawRect(
                        color = Color(0xFFFF5252).copy(alpha = 0.2f),
                        size = size.copy(width = size.width * holdProgress)
                    )
                }
            }
            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isHolding) "Hold to confirm..." else text,
            color = Color(0xFFFF5252),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsSectionOptimized(
    title: String,
    backdrop: Backdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(24.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                .padding(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SoundSelectorRow(
    title: String,
    selectedSound: TriggerSound,
    onSoundSelected: (TriggerSound) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TriggerSound.entries) { sound ->
                SoundChip(
                    sound = sound,
                    isSelected = sound == selectedSound,
                    onClick = { onSoundSelected(sound) }
                )
            }
        }
    }
}

@Composable
fun SoundChip(
    sound: TriggerSound,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f)

    Box(
        modifier = Modifier
            .height(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                CircleShape
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sound.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TriggerPickerSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    backdrop: Backdrop
) {
    val selectedMode = state.triggerMode
    val availableModes = state.availableTriggerModes
    val smartSuppression = state.smartTriggerSuppression

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        availableModes.forEach { mode ->
            val isSupported = if (mode is TriggerMode.ActionButton) state.isActionButtonSupported else true

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (isSelected(selectedMode, mode)) 1.5.dp else 1.dp,
                        color = when {
                            isSelected(selectedMode, mode) -> androidx.compose.ui.graphics.Color.White
                            else -> Color.White.copy(alpha = 0.05f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        if (isSelected(selectedMode, mode)) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
                        else Color.White.copy(alpha = 0.02f)
                    )
                    .clickable(enabled = isSupported) { if (isSupported) onIntent(SettingsIntent.SetTriggerMode(mode)) }
                    .padding(12.dp)
                    .graphicsLayer { alpha = if (isSupported) 1f else 0.4f }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected(selectedMode, mode)) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)
                                else Color.White.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTriggerIcon(mode),
                            contentDescription = null,
                            tint = if (isSelected(selectedMode, mode)) androidx.compose.ui.graphics.Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getTriggerTitle(mode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected(selectedMode, mode)) Color.White else Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = getTriggerDescription(mode, isSupported, state.triggerVolumeKey),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    if (isSelected(selectedMode, mode)) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelected(selectedMode, mode) && mode is TriggerMode.VolumeButton,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                    LiquidSettingsToggle(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Smart Suppression",
                        checked = smartSuppression,
                        onCheckedChange = { onIntent(SettingsIntent.SetSmartTriggerSuppression(it)) },
                        backdrop = backdrop
                    )
                    Text(
                        "Prevents activation while music or calls are active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 36.dp, bottom = 8.dp)
                    )

                    LiquidSettingsToggle(
                        icon = Icons.Rounded.Block,
                        title = "Consume Volume Keys",
                        checked = state.consumeVolumeKeys,
                        onCheckedChange = { onIntent(SettingsIntent.SetConsumeVolumeKeys(it)) },
                        backdrop = backdrop
                    )
                    Text(
                        "Suppresses system volume dialog while active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 36.dp, bottom = 8.dp)
                    )

                    AnimatedVisibility(
                        visible = state.consumeVolumeKeys,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            LiquidSettingsToggle(
                                icon = Icons.Rounded.TouchApp,
                                title = "Single Press Trigger",
                                checked = state.singlePressTrigger,
                                onCheckedChange = { onIntent(SettingsIntent.SetSinglePressTrigger(it)) },
                                backdrop = backdrop
                            )
                            Text(
                                "Use single long-press instead of double-press",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isSelected(current: TriggerMode, target: TriggerMode): Boolean {
    return current::class == target::class
}

private fun getTriggerTitle(mode: TriggerMode) = when (mode) {
    is TriggerMode.VolumeButton -> "Volume Button"
    is TriggerMode.ActionButton -> "Action Button"
    is TriggerMode.FloatingWidget -> "Floating Widget"
    is TriggerMode.Manual -> "Manual Only"
    else -> "Unknown"
}

private fun getTriggerDescription(mode: TriggerMode, isSupported: Boolean, triggerVolumeKey: String) = when (mode) {
    is TriggerMode.VolumeButton -> {
        val keyName = if (triggerVolumeKey == "VOLUME_UP") "volume up" else "volume down"
        "Double press and hold $keyName"
    }
    is TriggerMode.ActionButton -> if (isSupported) "Press and hold your device's action button" else "Hardware not detected on this device"
    is TriggerMode.FloatingWidget -> "Tap the floating bubble to record"
    is TriggerMode.Manual -> "Use the record button inside the app only"
    else -> ""
}

private fun getTriggerIcon(mode: TriggerMode) = when (mode) {
    is TriggerMode.VolumeButton -> Icons.Rounded.VolumeDown
    is TriggerMode.ActionButton -> Icons.Rounded.SmartButton
    is TriggerMode.FloatingWidget -> Icons.Rounded.OpenInNew
    is TriggerMode.Manual -> Icons.Rounded.Mic
    else -> Icons.Rounded.QuestionMark
}

@Composable
fun SliderMeter(
    steps: Int,
    range: ClosedFloatingPointRange<Float>,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().height(20.dp).padding(horizontal = 4.dp)) {
        val totalSteps = if (steps > 0) steps + 2 else 11

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
                            if (isSelected) androidx.compose.ui.graphics.Color.White
                            else Color.White.copy(alpha = if (i % (if (steps > 0) 1 else 5) == 0) 0.15f else 0.08f),
                            CircleShape
                        )
                )
            }
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
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = Color.White)
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
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = Color.White)
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
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            if (valueLabel != null) {
                Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
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
                focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
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
            modifier = Modifier.padding(bottom = 12.dp),
            color = Color.White
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
                .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
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
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
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
