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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
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
import com.example.whispry.presentation.common.CoachMark
import com.example.whispry.presentation.common.CoachMarkOverlay
import com.example.whispry.presentation.common.CoachMarkViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whispry.domain.model.FormattingProviderPreset
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.model.PressAction
import com.example.whispry.domain.model.RetentionPolicy
import com.example.whispry.domain.model.TranscriptionProviderPreset
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.service.TriggerSound
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.components.liquidExpand
import com.example.whispry.ui.components.liquidGlow
import com.example.whispry.ui.components.rememberLiquidTouch
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.example.whispry.ui.components.heroSharedBounds
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onNavigateToMyInfo: () -> Unit = {},
    onNavigateToVoiceCommands: () -> Unit = {},
    hero: WhispryHero? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coachVm: CoachMarkViewModel = hiltViewModel()
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
                        onNavigateToMyInfo = onNavigateToMyInfo,
                        onNavigateToVoiceCommands = onNavigateToVoiceCommands,
                        hero = hero,
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
                item { AiProviderSection(state, viewModel, backdrop) }
                item { TriggerMethodSection(state, viewModel, backdrop) }
                item { PressActionsSection(state, viewModel, backdrop) }
                item { FloatingWidgetSection(state, viewModel, backdrop) }
                item { InterfaceSoundsSection(state, viewModel, backdrop) }
                item {
                    ProductivitySection(
                        state, viewModel, backdrop,
                        onNavigateToTextExpander, onNavigateToAppTones, onNavigateToMemory,
                        onNavigateToMyInfo, onNavigateToVoiceCommands,
                        hero = hero
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

        CoachMarkOverlay(
            visible = coachVm.shouldShow(CoachMark.SETTINGS),
            title = "Make Whispry yours",
            message = "This is your control room — set your trigger key and gesture, sounds, privacy, and more. The trigger settings up top are the place to start.",
            onDismiss = { coachVm.markSeen(CoachMark.SETTINGS) }
        )
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
    val touch = rememberLiquidTouch(intensity = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidExpand(touch)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .liquidGlow(touch, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
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
    onNavigateToMyInfo: () -> Unit,
    onNavigateToVoiceCommands: () -> Unit,
    hero: WhispryHero? = null,
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
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        VoiceRecognitionSection(state, viewModel, backdrop, onShowLanguagePicker)
                        AiProviderSection(state, viewModel, backdrop)
                    }
                SettingsCategory.Trigger ->
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        TriggerMethodSection(state, viewModel, backdrop)
                        PressActionsSection(state, viewModel, backdrop)
                    }
                SettingsCategory.Interface ->
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        FloatingWidgetSection(state, viewModel, backdrop)
                        InterfaceSoundsSection(state, viewModel, backdrop)
                    }
                SettingsCategory.Productivity ->
                    ProductivitySection(
                        state, viewModel, backdrop,
                        onNavigateToTextExpander, onNavigateToAppTones, onNavigateToMemory,
                        onNavigateToMyInfo, onNavigateToVoiceCommands,
                        hero = hero
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
            helpText = "Sets the spoken language Whispry expects when transcribing. \"Auto\" detects it for you — pick a specific language only if detection ever gets it wrong.",
            onClick = onShowLanguagePicker
        )

        AnimatedVisibility(visible = state.language == "hi") {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                LiquidSettingsToggle(
                    icon = Icons.Rounded.Translate,
                    title = "Hinglish output",
                    checked = state.hinglishOutputEnabled,
                    onCheckedChange = { viewModel.onIntent(SettingsIntent.SetHinglishOutputEnabled(it)) },
                    backdrop = backdrop,
                    helpText = "Romanizes the Hindi transcript into Latin letters (\"kaise ho\" instead of \"कैसे हो\") instead of translating it. Whisper transcribes Hindi in Devanagari script by default; this runs it through the formatting AI to romanize it afterward."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LiquidSettingsSlider(
            title = "Temperature",
            value = state.temperature,
            onValueChange = { viewModel.onIntent(SettingsIntent.SetTemperature(it)) },
            valueRange = 0f..1f,
            backdrop = backdrop,
            startLabel = "Precise",
            endLabel = "Creative",
            helpText = "Controls how creative the AI is when formatting your text. Lower is more literal and predictable; higher is more varied. Keep it low for accurate transcripts."
        )
    }
}

@Composable
private fun AiProviderSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    SettingsSectionOptimized(title = "AI Providers", backdrop = backdrop) {
        Text(
            text = "Choose which AI handles transcription and formatting, independently. Defaults to Groq — change only if you want a different provider.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ProviderPickerRow(
            label = "Transcription",
            options = TranscriptionProviderPreset.entries,
            selected = state.transcriptionProviderPreset,
            optionLabel = { it.displayName },
            onSelect = { viewModel.onIntent(SettingsIntent.SetTranscriptionProviderPreset(it)) }
        )
        AnimatedVisibility(visible = state.transcriptionProviderPreset == TranscriptionProviderPreset.CUSTOM) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                ProviderCustomFields(
                    baseUrl = state.transcriptionCustomBaseUrl,
                    model = state.transcriptionCustomModel,
                    onBaseUrlChange = { viewModel.onIntent(SettingsIntent.SetTranscriptionCustomBaseUrl(it)) },
                    onModelChange = { viewModel.onIntent(SettingsIntent.SetTranscriptionCustomModel(it)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ApiKeyField(
            label = "Transcription API Key",
            apiKey = state.transcriptionApiKey,
            onValueChange = { viewModel.onIntent(SettingsIntent.SetTranscriptionApiKey(it)) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(20.dp))

        ProviderPickerRow(
            label = "Formatting",
            options = FormattingProviderPreset.entries,
            selected = state.formattingProviderPreset,
            optionLabel = { it.displayName },
            onSelect = { viewModel.onIntent(SettingsIntent.SetFormattingProviderPreset(it)) }
        )
        AnimatedVisibility(visible = state.formattingProviderPreset == FormattingProviderPreset.CUSTOM) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                ProviderCustomFields(
                    baseUrl = state.formattingCustomBaseUrl,
                    model = state.formattingCustomModel,
                    onBaseUrlChange = { viewModel.onIntent(SettingsIntent.SetFormattingCustomBaseUrl(it)) },
                    onModelChange = { viewModel.onIntent(SettingsIntent.SetFormattingCustomModel(it)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ApiKeyField(
            label = "Formatting API Key",
            apiKey = state.formattingApiKey,
            onValueChange = { viewModel.onIntent(SettingsIntent.SetFormattingApiKey(it)) }
        )

        AnimatedVisibility(
            visible = state.transcriptionProviderPreset == TranscriptionProviderPreset.CUSTOM ||
                    state.formattingProviderPreset == FormattingProviderPreset.CUSTOM
        ) {
            Text(
                text = "Custom endpoints need HTTPS, or HTTP to a server on the same WiFi network as your phone — a local server becomes unreachable the moment you switch to cellular data.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun <T> ProviderPickerRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(optionLabel(selected), color = Color.White, fontSize = 14.sp)
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCustomFields(
    baseUrl: String,
    model: String,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL (e.g. https://api.example.com/v1/)", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("Model", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
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

            Spacer(modifier = Modifier.height(16.dp))

            if (state.pressActionsEnabled) {
                Text(
                    "Press Actions (below) has taken over the trigger key — Hands-free and the hold/double-press settings here don't apply until you turn it off.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                LiquidSettingsToggle(
                    icon = Icons.Rounded.PanTool,
                    title = "Hands-free",
                    checked = state.handsFreeMode,
                    onCheckedChange = { viewModel.onIntent(SettingsIntent.SetHandsFreeMode(it)) },
                    backdrop = backdrop,
                    helpText = "No need to hold the key down. Trigger once to start recording, speak as long as you like, then trigger again to stop. Respects your single/double-press choice — single mode toggles on one press; double mode needs a quick double press to start, but just one press to stop."
                )
                Text(
                    "Trigger to start, trigger again to stop — no holding.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 36.dp, top = 4.dp)
                )

                AnimatedVisibility(
                    visible = state.handsFreeMode && state.singlePressTrigger,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        LiquidSettingsSlider(
                            title = "Arming delay",
                            value = state.handsFreeArmingDelayMs.toFloat(),
                            onValueChange = { viewModel.onIntent(SettingsIntent.SetHandsFreeArmingDelay(it.toLong())) },
                            valueRange = 150f..800f,
                            steps = 12,
                            backdrop = backdrop,
                            startLabel = "Snappy",
                            endLabel = "Deliberate",
                            valueLabel = "${state.handsFreeArmingDelayMs} ms",
                            helpText = "How long you must hold the key before it starts recording. A quick tap under this delay acts as a normal key press instead — so you can still use the volume key normally."
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !state.handsFreeMode && state.singlePressTrigger,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        LiquidSettingsSlider(
                            title = "Hold delay",
                            value = state.singlePressHoldDelayMs.toFloat(),
                            onValueChange = { viewModel.onIntent(SettingsIntent.SetSinglePressHoldDelay(it.toLong())) },
                            valueRange = 150f..1500f,
                            steps = 26,
                            backdrop = backdrop,
                            startLabel = "Snappy",
                            endLabel = "Deliberate",
                            valueLabel = "${state.singlePressHoldDelayMs} ms",
                            helpText = "How long you must hold the key before recording starts. A quick tap under this delay passes through as a normal volume press instead — this is the plain press-and-hold trigger, separate from Hands-free's own arming delay above."
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !state.singlePressTrigger,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        LiquidSettingsSlider(
                            title = "Double-press speed",
                            value = state.doublePressInterval.toFloat(),
                            onValueChange = { viewModel.onIntent(SettingsIntent.SetDoublePressInterval(it.toLong())) },
                            valueRange = 200f..1500f,
                            steps = 25,
                            backdrop = backdrop,
                            startLabel = "Fast",
                            endLabel = "Relaxed",
                            valueLabel = "${state.doublePressInterval} ms",
                            helpText = "How quickly the two presses of a double-press must follow each other. Lower is snappier but easier to miss; higher gives you more time between presses."
                        )
                    }
                }
            }
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

/**
 * Universal Press Actions (opt-in). Lets the user assign what a single press and a double press
 * of the trigger key each do — normal transcribe-and-paste, format with a chosen preset, or open
 * an app and copy the text to the clipboard. Off by default so the proven hold-to-record trigger is
 * untouched until the user opts in.
 */
@Composable
private fun PressActionsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    // null = closed, "single"/"double" = which slot's picker is open.
    var openPicker by remember { mutableStateOf<String?>(null) }

    SettingsSectionOptimized(title = "Press Actions", backdrop = backdrop) {
        LiquidSettingsToggle(
            icon = Icons.Rounded.Tune,
            title = "Custom Press Actions",
            checked = state.pressActionsEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetPressActionsEnabled(it)) },
            backdrop = backdrop,
            helpText = "Take over the trigger key with your own actions. Assign a single press and a double press separately — each can transcribe-and-paste, format with a preset, or open an app with your spoken text copied to the clipboard. Recording becomes tap-to-toggle (press once to start, again to stop), so it's fully hands-free. Turn this off to restore the normal double-press-and-hold trigger."
        )

        AnimatedVisibility(
            visible = state.pressActionsEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Single press",
                    value = PressAction.parse(state.singlePressAction).displayLabel(),
                    onClick = { openPicker = "single" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Double press",
                    value = PressAction.parse(state.doublePressAction).displayLabel(),
                    onClick = { openPicker = "double" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsSlider(
                    title = "Double-press speed",
                    value = state.doublePressInterval.toFloat(),
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetDoublePressInterval(it.toLong())) },
                    valueRange = 200f..1500f,
                    steps = 25,
                    backdrop = backdrop,
                    startLabel = "Fast",
                    endLabel = "Relaxed",
                    valueLabel = "${state.doublePressInterval} ms",
                    helpText = "How quickly the two presses of a double-press must follow each other. Lower is snappier but easier to miss; higher gives you more time between presses."
                )
            }
        }
    }

    openPicker?.let { slot ->
        val current = PressAction.parse(
            if (slot == "single") state.singlePressAction else state.doublePressAction
        )
        PressActionPicker(
            title = if (slot == "single") "Single press" else "Double press",
            current = current,
            onSelect = { action ->
                val intent = if (slot == "single")
                    SettingsIntent.SetSinglePressAction(action.serialize())
                else
                    SettingsIntent.SetDoublePressAction(action.serialize())
                viewModel.onIntent(intent)
                openPicker = null
            },
            onDismiss = { openPicker = null }
        )
    }
}

/** Bottom-sheet picker for a single press-action slot: Normal / a preset / open an installed app. */
@Composable
private fun PressActionPicker(
    title: String,
    current: PressAction,
    onSelect: (PressAction) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Launchable apps for the "open app" options, loaded off the main thread while the sheet is open.
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    ri.loadLabel(pm).toString() to pkg
                }
                .distinctBy { it.second }
                .sortedBy { it.first.lowercase() }
        }
    }

    WhispryBottomSheet(title = title, onDismiss = onDismiss) {
        PressActionGroupLabel("Transcribe")
        PressActionOptionRow(
            title = "Transcribe & paste",
            subtitle = "Normal — clean up and paste the text",
            selected = current is PressAction.Normal,
            onClick = { onSelect(PressAction.Normal) }
        )

        OutputPreset.byGroup().forEach { (group, presets) ->
            val items = presets.filter { it != OutputPreset.NONE }
            if (items.isEmpty()) return@forEach
            Spacer(modifier = Modifier.height(20.dp))
            PressActionGroupLabel(group.displayName)
            items.forEach { preset ->
                PressActionOptionRow(
                    title = "${preset.emoji}  ${preset.displayName}",
                    subtitle = preset.description,
                    selected = current is PressAction.Preset && current.preset == preset,
                    onClick = { onSelect(PressAction.Preset(preset)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        PressActionGroupLabel("Open an app + copy to clipboard")
        if (apps.isEmpty()) {
            Text(
                "Loading apps…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            apps.forEach { (label, pkg) ->
                PressActionOptionRow(
                    title = label,
                    subtitle = null,
                    selected = current is PressAction.OpenApp && current.packageName == pkg,
                    onClick = { onSelect(PressAction.OpenApp(pkg, label)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PressActionGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.4f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun PressActionOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val touch = rememberLiquidTouch(intensity = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidExpand(touch)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .liquidGlow(touch, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * The floating physical-switch widget: always-available press-to-talk surface that coexists
 * with every other trigger. Enable toggle routes through the overlay permission; everything
 * else (shape, idle behavior, taps, edit mode) lives behind it.
 */
@Composable
private fun FloatingWidgetSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    val context = LocalContext.current
    val cfg = state.widgetConfig

    SettingsSectionOptimized(title = "Floating Switch", backdrop = backdrop) {
        LiquidSettingsToggle(
            icon = Icons.Rounded.OpenInNew,
            title = "Floating Switch",
            checked = state.floatingWidgetEnabled,
            onCheckedChange = { enabled ->
                if (enabled && !android.provider.Settings.canDrawOverlays(context)) {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
                viewModel.onIntent(SettingsIntent.SetFloatingWidgetEnabled(enabled))
            },
            backdrop = backdrop,
            helpText = "A small accent-colored switch that stays on screen. Press and hold it to talk, release to send, or flick it down to cancel. It works alongside your other triggers and fades out of the way when idle."
        )

        AnimatedVisibility(
            visible = state.floatingWidgetEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Category,
                    title = "Shape",
                    value = if (cfg.shapeMode == com.example.whispry.service.WidgetShapeMode.RAMP) "Edge ramp" else "Corner arch",
                    helpText = "Edge ramp is a slim wedge hugging the left or right edge. Corner arch wraps around a screen corner and follows its curve — adjust the arch in edit mode to match your device.",
                    onClick = {
                        val next = if (cfg.shapeMode == com.example.whispry.service.WidgetShapeMode.RAMP)
                            com.example.whispry.service.WidgetShapeMode.CORNER
                        else com.example.whispry.service.WidgetShapeMode.RAMP
                        viewModel.onIntent(SettingsIntent.SetWidgetShapeMode(next.name))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsRow(
                    icon = Icons.Rounded.OpenWith,
                    title = "Adjust position & size",
                    value = "",
                    helpText = "Opens a live preview over your home screen. Drag the switch anywhere (it snaps to edges or corners), fine-tune its size, then tap Done.",
                    onClick = { viewModel.onIntent(SettingsIntent.EnterWidgetEditMode) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsSlider(
                    title = "Anti-accident delay",
                    value = cfg.armingDelayMs.toFloat(),
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetWidgetArmingDelay(it.toLong())) },
                    valueRange = 150f..800f,
                    steps = 12,
                    backdrop = backdrop,
                    startLabel = "Fast",
                    endLabel = "Delayed",
                    valueLabel = "${cfg.armingDelayMs} ms",
                    helpText = "How long you must hold before recording actually starts. The switch fills up while you wait, then clicks when it arms — accidental brushes never fire."
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsSlider(
                    title = "Idle transparency",
                    value = cfg.idleOpacityPct.toFloat(),
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetWidgetIdleOpacity(it.toInt())) },
                    valueRange = 10f..80f,
                    backdrop = backdrop,
                    startLabel = "Subtle",
                    endLabel = "Visible",
                    valueLabel = "${cfg.idleOpacityPct}%",
                    helpText = "How visible the switch stays after it fades. The touch area never shrinks — a faded switch is just as easy to grab."
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsSlider(
                    title = "Fade delay",
                    value = (cfg.fadeDelayMs / 1000f),
                    onValueChange = { viewModel.onIntent(SettingsIntent.SetWidgetFadeDelay((it * 1000).toLong())) },
                    valueRange = 1f..10f,
                    steps = 8,
                    backdrop = backdrop,
                    startLabel = "Quick",
                    endLabel = "Patient",
                    valueLabel = "${cfg.fadeDelayMs / 1000}s",
                    helpText = "Seconds of no use before the switch shrinks and fades out of the way."
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsToggle(
                    icon = Icons.Rounded.Tune,
                    title = "Custom taps",
                    checked = cfg.customTriggers,
                    onCheckedChange = { viewModel.onIntent(SettingsIntent.SetWidgetCustomTriggers(it)) },
                    backdrop = backdrop,
                    helpText = "Off = same as default: a single tap starts and stops a hands-free recording, holding is always press-to-talk. On = choose what single and double taps do."
                )

                AnimatedVisibility(
                    visible = cfg.customTriggers,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        SettingsRow(
                            icon = Icons.Rounded.TouchApp,
                            title = "Single tap",
                            value = widgetTapActionLabel(cfg.singleTapAction),
                            onClick = {
                                viewModel.onIntent(
                                    SettingsIntent.SetWidgetSingleTapAction(nextWidgetTapAction(cfg.singleTapAction).serialize())
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsRow(
                            icon = Icons.Rounded.Bolt,
                            title = "Double tap",
                            value = widgetTapActionLabel(cfg.doubleTapAction),
                            onClick = {
                                viewModel.onIntent(
                                    SettingsIntent.SetWidgetDoubleTapAction(nextWidgetTapAction(cfg.doubleTapAction).serialize())
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LiquidSettingsToggle(
                    icon = Icons.AutoMirrored.Rounded.VolumeOff,
                    title = "Mute switch sounds",
                    checked = cfg.soundMuted,
                    onCheckedChange = { viewModel.onIntent(SettingsIntent.SetWidgetSoundMuted(it)) },
                    backdrop = backdrop,
                    helpText = "Silences the switch's own start cue. Recording sounds from the pill follow your Trigger Sounds setting."
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Animation,
                    title = "Springy animations",
                    value = when (cfg.motion) {
                        com.example.whispry.service.WidgetMotionSetting.AUTO -> "Follow system"
                        com.example.whispry.service.WidgetMotionSetting.ON -> "Always on"
                        com.example.whispry.service.WidgetMotionSetting.OFF -> "Off"
                    },
                    helpText = "The bouncy press-and-cancel animations. \"Follow system\" turns them off automatically when your device's remove-animations accessibility setting is on.",
                    onClick = {
                        val next = when (cfg.motion) {
                            com.example.whispry.service.WidgetMotionSetting.AUTO -> com.example.whispry.service.WidgetMotionSetting.ON
                            com.example.whispry.service.WidgetMotionSetting.ON -> com.example.whispry.service.WidgetMotionSetting.OFF
                            com.example.whispry.service.WidgetMotionSetting.OFF -> com.example.whispry.service.WidgetMotionSetting.AUTO
                        }
                        viewModel.onIntent(SettingsIntent.SetWidgetMotion(next.name))
                    }
                )
            }
        }
    }
}

private fun widgetTapActionLabel(action: com.example.whispry.service.WidgetTapAction): String =
    when (action) {
        com.example.whispry.service.WidgetTapAction.None -> "Nothing"
        is com.example.whispry.service.WidgetTapAction.ToggleRecord -> "Start & stop recording"
    }

private fun nextWidgetTapAction(current: com.example.whispry.service.WidgetTapAction): com.example.whispry.service.WidgetTapAction =
    when (current) {
        com.example.whispry.service.WidgetTapAction.None -> com.example.whispry.service.WidgetTapAction.ToggleRecord()
        is com.example.whispry.service.WidgetTapAction.ToggleRecord -> com.example.whispry.service.WidgetTapAction.None
    }

@Composable
private fun InterfaceSoundsSection(
    state: SettingsState,
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop
) {
    SettingsSectionOptimized(title = "Interface & Sounds", backdrop = backdrop) {
        LiquidSettingsToggle(
            icon = Icons.Rounded.BlurOn,
            title = "Glass Navbar",
            checked = state.glassNavbar,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetGlassNavbar(it)) },
            backdrop = backdrop,
            helpText = "Gives the bottom navigation bar a frosted, see-through glass look. Turn it off for a plain solid bar that's slightly lighter on performance."
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
            backdrop = backdrop,
            helpText = "Plays a short sound when recording starts and stops, so you get audio confirmation that the trigger worked."
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
    onNavigateToMemory: () -> Unit,
    onNavigateToMyInfo: () -> Unit,
    onNavigateToVoiceCommands: () -> Unit,
    hero: WhispryHero? = null
) {
    SettingsSectionOptimized(title = "Productivity", backdrop = backdrop) {
        LiquidSettingsToggle(
            icon = Icons.Rounded.Bolt,
            title = "Voice Commands & Shortcuts",
            checked = state.voiceCommandsEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetVoiceCommandsEnabled(it)) },
            backdrop = backdrop,
            helpText = "Lets the first word you speak run an action instead of being transcribed — \"expand\" for snippets, \"insert\" for saved info, or your own app commands like \"chrome\". A miss just transcribes normally."
        )
        Text(
            "Enables \"expand\", \"insert\" and your app commands as the first spoken word.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 4.dp)
        )

        AnimatedVisibility(
            visible = state.voiceCommandsEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsRow(
                    icon = Icons.Rounded.Bolt,
                    title = "Voice Commands",
                    value = "Word → open / search app",
                    helpText = "Create trigger words that open apps or run searches. Say the word, then your query — e.g. \"chrome best laptops\" searches the web, \"note buy milk\" opens a notes app.",
                    hero = hero,
                    heroKey = WhispryHeroKeys.VoiceCommands,
                    onClick = onNavigateToVoiceCommands
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = "My Info",
                    value = "insert address, email…",
                    helpText = "Save details like your address, email or phone once, then paste any of them by voice with \"insert <name>\" (e.g. \"insert address\").",
                    hero = hero,
                    heroKey = WhispryHeroKeys.MyInfo,
                    onClick = onNavigateToMyInfo
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            icon = Icons.Rounded.TextFields,
            title = "Text Expander",
            value = "expand shortcut → full text",
            helpText = "Save short shortcuts that expand into longer text. Say \"expand <shortcut>\" while recording to paste the full snippet (e.g. \"expand sig\" → your email signature).",
            hero = hero,
            heroKey = WhispryHeroKeys.TextExpander,
            onClick = onNavigateToTextExpander
        )

        Spacer(modifier = Modifier.height(16.dp))

        LiquidSettingsToggle(
            icon = Icons.Rounded.AutoAwesome,
            title = "App-Aware Tones",
            checked = state.appAwareToneEnabled,
            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAppAwareToneEnabled(it)) },
            backdrop = backdrop,
            helpText = "Automatically changes the formatting and tone based on the app you're typing in — e.g. professional in email, casual in chat. Configure the mappings below."
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            icon = Icons.Rounded.Memory,
            title = "Memory Bank",
            value = "Personalize your context",
            helpText = "Store personal facts (names, projects, preferences) that the AI can use to make your transcripts more accurate and personalized.",
            hero = hero,
            heroKey = WhispryHeroKeys.Memory,
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
                    helpText = "Map specific apps to a preset, so transcripts are formatted that way whenever that app is in the foreground.",
                    hero = hero,
                    heroKey = WhispryHeroKeys.AppTones,
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
            helpText = "How long Whispry keeps your saved transcripts before automatically deleting them. Everything stays on this device — nothing is uploaded.",
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
            backdrop = backdrop,
            helpText = "Restarts Whispry's background service automatically after you reboot your phone, so the trigger keeps working without opening the app first."
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            icon = Icons.Rounded.HelpOutline,
            title = "Revisit Tutorial",
            value = "Show guide",
            helpText = "Replays the first-time setup guide that explains how triggers and permissions work.",
            onClick = onRevisitTutorial
        )

        Spacer(modifier = Modifier.height(16.dp))

        val coachVm: CoachMarkViewModel = hiltViewModel()
        SettingsRow(
            icon = Icons.Rounded.Lightbulb,
            title = "Replay Tips",
            value = "Show again",
            helpText = "Shows the first-visit tips on the Presets and Settings screens again.",
            onClick = { coachVm.replayAll() }
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
                val touch = rememberLiquidTouch(intensity = 0.35f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidExpand(touch)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) else Color.Transparent)
                        .liquidGlow(touch, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onPolicySelected(policy) }
                        )
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
            backdrop = backdrop,
            helpText = "Temporarily lowers other audio (music, video) while you're recording so your voice is captured clearly, then restores it afterwards."
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
    val touch = rememberLiquidTouch(intensity = 0.35f)
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
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                // Glow blooms wherever the card is touched; no scale/stretch so tapping an inner
                // control (toggle/slider/row) doesn't make the whole section lurch.
                .liquidGlow(touch, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
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
    val touch = rememberLiquidTouch(intensity = 0.35f)

    Box(
        modifier = Modifier
            .height(36.dp)
            .liquidExpand(touch)
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
            .liquidGlow(touch, CircleShape)
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

    // Phone State is requested just-in-time here — only when the user turns on call suppression.
    val context = LocalContext.current
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Only persist "on" if actually granted — otherwise the toggle would claim to be
        // enabled while call suppression has no permission to act on (denial degrades
        // gracefully: everything else keeps working, this feature just stays off).
        onIntent(SettingsIntent.SetSmartTriggerSuppression(granted))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        availableModes.forEach { mode ->
            val isSupported = if (mode is TriggerMode.ActionButton) state.isActionButtonSupported else true
            val touch = rememberLiquidTouch(intensity = 0.35f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidExpand(touch, enabled = isSupported)
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
                    .liquidGlow(touch, RoundedCornerShape(16.dp), enabled = isSupported)
                    .clickable(
                        enabled = isSupported,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (isSupported) onIntent(SettingsIntent.SetTriggerMode(mode)) }
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
                        onCheckedChange = { enabled ->
                            if (enabled && ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.READ_PHONE_STATE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                // Persisting happens in the launcher's callback once the grant
                                // result is actually known — not here.
                                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            } else {
                                onIntent(SettingsIntent.SetSmartTriggerSuppression(enabled))
                            }
                        },
                        backdrop = backdrop,
                        helpText = "Ignores the trigger when it's likely accidental — e.g. while you're on a call or media is actively playing — to avoid false recordings."
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
                        backdrop = backdrop,
                        helpText = "When on, the trigger key press won't also change the volume (no volume dialog). Turn off if you'd rather the volume still adjusts normally."
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
                                backdrop = backdrop,
                                helpText = "Start recording with a single press of the volume key instead of a double press. Faster, but more prone to accidental triggers."
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
    helpText: String? = null,
    hero: com.example.whispry.ui.components.WhispryHero? = null,
    heroKey: Any? = null,
    onClick: (() -> Unit)? = null
) {
    val touch = rememberLiquidTouch(intensity = 0.35f)
    val interactive = onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heroSharedBounds(hero, heroKey)
            .liquidExpand(touch, enabled = interactive)
            .liquidGlow(touch, RoundedCornerShape(12.dp), enabled = interactive)
            .clickable(
                enabled = interactive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = Color.White)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        if (helpText != null) HelpDot(title, helpText)
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
    backdrop: Backdrop,
    helpText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = Color.White)
        if (helpText != null) HelpDot(title, helpText)
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop
        )
    }
}

/** Small "?" affordance that opens a plain-language explanation for a setting. */
@Composable
private fun HelpDot(title: String, text: String) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Rounded.HelpOutline, "What is $title?", tint = Color.White.copy(alpha = 0.32f), modifier = Modifier.size(17.dp))
    }
    if (show) SettingHelpPopup(title, text) { show = false }
}

@Composable
private fun SettingHelpPopup(title: String, text: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(24.dp))
                .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, ContinuousRoundedRectangle(24.dp))
                .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.HelpOutline, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f), lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    shape = ContinuousRoundedRectangle(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Got it", fontWeight = FontWeight.Bold) }
            }
        }
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
    valueLabel: String? = null,
    helpText: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                if (helpText != null) HelpDot(title, helpText)
            }
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
    onValueChange: (String) -> Unit,
    label: String = "Groq API Key"
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
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

    val touch = rememberLiquidTouch(intensity = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidExpand(touch)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isRunning) Color.Transparent
                else Color(0xFFFF5252).copy(alpha = 0.05f)
            )
            .liquidGlow(touch, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
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
