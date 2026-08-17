// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.tone.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.liquidExpand
import com.example.whispry.ui.components.liquidGlow
import com.example.whispry.ui.components.rememberLiquidTouch
import com.example.whispry.ui.components.SheetPrimaryButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.components.WhispryCard
import com.example.whispry.ui.components.WhispryDetailScaffold
import com.example.whispry.ui.components.WhispryEmptyState
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToneScreen(
    viewModel: AppToneViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val appTones by viewModel.appTones.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    WhispryDetailScaffold(
        title = "App-Aware Tones",
        onBack = { navController.popBackStack() },
        subtitle = "Automatically adjust transcription formatting and tone based on the active foreground application (e.g. professional for Slack, casual for WhatsApp).",
        pushBackProgress = if (showAddDialog) sheetProgress else 0f,
        hero = hero,
        heroKey = WhispryHeroKeys.AppTones,
        modifier = modifier,
        headerActions = {
            HeaderActionButton(Icons.Rounded.Add, "Add mapping") { showAddDialog = true }
        }
    ) {
        if (appTones.isEmpty()) {
            WhispryEmptyState(
                title = "No app tone mappings yet",
                hint = "Tap + to customize formatting for an app"
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(appTones, key = { it.packageName }) { mapping ->
                    AppToneCard(
                        modifier = Modifier.animateItem(),
                        mapping = mapping,
                        onDelete = { viewModel.deleteAppTone(mapping.packageName) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppToneSheet(
            installedApps = installedApps,
            alreadyMappedPackages = appTones.map { it.packageName }.toSet(),
            onDismiss = { showAddDialog = false },
            onDragProgress = { sheetProgress = it },
            onSave = { packageName, appName, preset, customPrompt ->
                viewModel.saveAppTone(packageName, appName, preset.name, customPrompt)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AppToneCard(
    mapping: AppToneEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color.White
    val preset = remember(mapping.presetName) {
        OutputPreset.values().find { it.name == mapping.presetName } ?: OutputPreset.NONE
    }

    WhispryCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mapping.appName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = mapping.packageName,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${preset.emoji}  ${preset.displayName}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                if (mapping.presetName == "CUSTOM" && mapping.customPromptOverride.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mapping.customPromptOverride,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddAppToneSheet(
    installedApps: List<AppInfo>,
    alreadyMappedPackages: Set<String>,
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onSave: (packageName: String, appName: String, preset: OutputPreset, customPrompt: String) -> Unit
) {
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedPreset by remember { mutableStateOf(OutputPreset.INTELLIGENT_FORMAT) }
    var customPrompt by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }

    WhispryBottomSheet(
        title = "Map App Formatting",
        onDismiss = onDismiss,
        heightFraction = 0.9f,
        onDragProgress = onDragProgress
    ) {
        SelectorField(
            label = "Target App",
            value = selectedApp?.appName,
            placeholder = "Select an App…",
            onClick = { showAppPicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SelectorField(
            label = "Formatting Preset / Tone",
            value = "${selectedPreset.emoji}  ${selectedPreset.displayName}",
            placeholder = "Select a preset…",
            onClick = { showPresetPicker = true }
        )

        if (selectedPreset == OutputPreset.CUSTOM) {
            Spacer(modifier = Modifier.height(16.dp))
            SheetTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                label = "Custom Instructions for this App",
                placeholder = "e.g. Write a brief professional reply, start with hello…",
                minLines = 4
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SheetPrimaryButton(enabled = selectedApp != null) {
            selectedApp?.let { onSave(it.packageName, it.appName, selectedPreset, customPrompt) }
        }
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = installedApps.filter { !alreadyMappedPackages.contains(it.packageName) },
            onDismiss = { showAppPicker = false },
            onPick = { selectedApp = it; showAppPicker = false }
        )
    }

    if (showPresetPicker) {
        PresetPickerSheet(
            current = selectedPreset,
            onDismiss = { showPresetPicker = false },
            onPick = { selectedPreset = it; showPresetPicker = false }
        )
    }
}

/** A tappable field row that opens a picker — matches the SheetTextField glass style. */
@Composable
private fun SelectorField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            color = WhispryTokens.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        val touch = rememberLiquidTouch()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .liquidExpand(touch)
                .clip(ContinuousRoundedRectangle(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(16.dp))
                .liquidGlow(touch, ContinuousRoundedRectangle(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value ?: placeholder,
                color = if (value != null) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.ExpandMore, null, tint = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AppPickerSheet(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onPick: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    WhispryBottomSheet(
        title = "Choose App",
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        scrollableContent = false
    ) {
        SheetTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search apps",
            placeholder = "Search apps",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text("No apps found", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val touch = rememberLiquidTouch()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidExpand(touch)
                            .liquidGlow(touch, ContinuousRoundedRectangle(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPick(app) }
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(app.appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(app.packageName, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetPickerSheet(
    current: OutputPreset,
    onDismiss: () -> Unit,
    onPick: (OutputPreset) -> Unit
) {
    WhispryBottomSheet(
        title = "Choose Preset",
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        scrollableContent = false
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            OutputPreset.byGroup().forEach { (group, presets) ->
                item(key = "header_${group.name}") {
                    Text(
                        group.displayName,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }
                items(presets, key = { it.name }) { preset ->
                    val selected = preset == current
                    val touch = rememberLiquidTouch()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidExpand(touch)
                            .clip(ContinuousRoundedRectangle(14.dp))
                            .background(if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .liquidGlow(touch, ContinuousRoundedRectangle(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPick(preset) }
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                preset.displayName,
                                color = Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(preset.description, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                        if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
