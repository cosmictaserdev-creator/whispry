package com.example.whispry.features.tone.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.ScreenHeader
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToneScreen(
    viewModel: AppToneViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val appTones by viewModel.appTones.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header Row
            ScreenHeader(
                title = "App-Aware Tones",
                onBack = { navController.popBackStack() },
                actions = {
                    HeaderActionButton(Icons.Rounded.Add, "Add mapping") { showAddDialog = true }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Automatically adjust transcription formatting and tone based on the active foreground application (e.g. professional for Slack, casual for WhatsApp).",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (appTones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No app tone mappings yet",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to customize formatting for an app",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(appTones, key = { it.packageName }) { mapping ->
                        AppToneCard(
                            mapping = mapping,
                            onDelete = { viewModel.deleteAppTone(mapping.packageName) }
                        )
                    }
                }
            }
        }

        // Add Mapping Dialog
        if (showAddDialog) {
            AddAppToneDialog(
                installedApps = installedApps,
                alreadyMappedPackages = appTones.map { it.packageName }.toSet(),
                onDismiss = { showAddDialog = false },
                onSave = { packageName, appName, preset, customPrompt ->
                    viewModel.saveAppTone(packageName, appName, preset.name, customPrompt)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun AppToneCard(
    mapping: AppToneEntity,
    onDelete: () -> Unit
) {
    val accentColor = androidx.compose.ui.graphics.Color.White
    val backdropState = rememberLayerBackdrop()
    val preset = remember(mapping.presetName) {
        OutputPreset.values().find { it.name == mapping.presetName } ?: OutputPreset.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                clip = true
                shape = ContinuousRoundedRectangle(16.dp)
            }
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(16.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(16.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), ContinuousRoundedRectangle(16.dp))
            .padding(16.dp)
    ) {
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

                // Mapped Preset Pill Badge
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
                        color = androidx.compose.ui.graphics.Color.White,
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

            // Delete Button
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppToneDialog(
    installedApps: List<AppInfo>,
    alreadyMappedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (packageName: String, appName: String, preset: OutputPreset, customPrompt: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, alreadyMappedPackages, searchQuery) {
        installedApps.filter { 
            !alreadyMappedPackages.contains(it.packageName) &&
            (it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedPreset by remember { mutableStateOf(OutputPreset.INTELLIGENT_FORMAT) }
    var customPrompt by remember { mutableStateOf("") }
    var expandedPresetMenu by remember { mutableStateOf(false) }
    var expandedAppMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Map App Formatting",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = Color(0xFF14141E),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Application Picker Selection
                Column {
                    Text(
                        text = "Target App",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { expandedAppMenu = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedApp?.appName ?: "Select an App...",
                                color = if (selectedApp != null) Color.White else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (expandedAppMenu) {
                        DropdownMenu(
                            expanded = expandedAppMenu,
                            onDismissRequest = { expandedAppMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .heightIn(max = 280.dp)
                                .background(Color(0xFF1F1F2E))
                        ) {
                            // Search Box in dropdown
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.4f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (filteredApps.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No apps found", color = Color.White.copy(alpha = 0.5f)) },
                                    onClick = {}
                                )
                            } else {
                                filteredApps.forEach { app ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(app.appName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                Text(app.packageName, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedApp = app
                                            expandedAppMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Preset Selection
                Column {
                    Text(
                        text = "Formatting Preset / Tone",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { expandedPresetMenu = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${selectedPreset.emoji}  ${selectedPreset.displayName}",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedPresetMenu,
                        onDismissRequest = { expandedPresetMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(Color(0xFF1F1F2E))
                    ) {
                        OutputPreset.values().forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${preset.emoji} ${preset.displayName}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(preset.description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedPreset = preset
                                    expandedPresetMenu = false
                                }
                            )
                        }
                    }
                }

                // Custom Prompt Override Input (shown if Custom Preset is selected)
                if (selectedPreset == OutputPreset.CUSTOM) {
                    Column {
                        Text(
                            text = "Custom Instructions for this App",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            placeholder = {
                                Text(
                                    text = "e.g. Write a brief professional reply, start with hello...",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedApp?.let {
                        onSave(it.packageName, it.appName, selectedPreset, customPrompt)
                    }
                },
                enabled = selectedApp != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.whispry.ui.theme.WhispryTheme.colors.accent,
                    disabledContentColor = com.example.whispry.ui.theme.WhispryTheme.colors.accent.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
