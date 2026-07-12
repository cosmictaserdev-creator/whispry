package com.example.whispry.features.voicecommand.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import com.example.whispry.features.voicecommand.domain.model.VoiceCommandAction
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.SheetPrimaryButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.components.WhispryCard
import com.example.whispry.ui.components.WhispryDetailScaffold
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.example.whispry.ui.components.WhispryPill
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun VoiceCommandScreen(
    viewModel: VoiceCommandViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val commands by viewModel.commands.collectAsState()
    var editTarget by remember { mutableStateOf<VoiceCommandEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    WhispryDetailScaffold(
        title = "Voice Commands",
        onBack = { navController.popBackStack() },
        subtitle = "Say a trigger word, then your query — e.g. \"chrome best football player\" searches the web. Open-App launches an app and copies the rest for you to paste.",
        pushBackProgress = if (showDialog) sheetProgress else 0f,
        hero = hero,
        heroKey = WhispryHeroKeys.VoiceCommands,
        modifier = modifier,
        headerActions = {
            HeaderActionButton(Icons.Rounded.Add, "Add") { editTarget = null; showDialog = true }
        }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(commands, key = { it.id }) { cmd ->
                VoiceCommandCard(
                    modifier = Modifier.animateItem(),
                    command = cmd,
                    onEdit = { editTarget = cmd; showDialog = true },
                    onDelete = { viewModel.delete(cmd) }
                )
            }
        }
    }

    if (showDialog) {
        VoiceCommandSheet(
                existing = editTarget,
                viewModel = viewModel,
                onDismiss = { showDialog = false },
                onDragProgress = { sheetProgress = it },
                onSave = { trigger, action, pkg, label ->
                    viewModel.save(trigger, action, pkg, label)
                    showDialog = false
                }
            )
    }
}

private fun actionOf(name: String): VoiceCommandAction =
    try { VoiceCommandAction.valueOf(name) } catch (e: Exception) { VoiceCommandAction.WEB_SEARCH }

@Composable
private fun VoiceCommandCard(
    command: VoiceCommandEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val action = actionOf(command.action)
    WhispryCard(modifier = modifier, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                WhispryPill(command.triggerWord)
                Spacer(modifier = Modifier.height(10.dp))
                val subtitle = if (action == VoiceCommandAction.OPEN_APP && command.targetAppLabel.isNotBlank())
                    "Open ${command.targetAppLabel}" else action.label
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
                Text(action.description, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.Red.copy(alpha = 0.1f))
            ) { Icon(Icons.Rounded.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun VoiceCommandSheet(
    existing: VoiceCommandEntity?,
    viewModel: VoiceCommandViewModel,
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onSave: (trigger: String, action: VoiceCommandAction, pkg: String, label: String) -> Unit
) {
    var trigger by remember { mutableStateOf(existing?.triggerWord ?: "") }
    var action by remember { mutableStateOf(existing?.let { actionOf(it.action) } ?: VoiceCommandAction.WEB_SEARCH) }
    var pkg by remember { mutableStateOf(existing?.targetPackage ?: "") }
    var label by remember { mutableStateOf(existing?.targetAppLabel ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    WhispryBottomSheet(
        title = if (existing == null) "New Command" else "Edit Command",
        onDismiss = onDismiss,
        heightFraction = 0.9f,
        onDragProgress = onDragProgress
    ) {
        SheetTextField(
            value = trigger,
            onValueChange = { trigger = it.replace(" ", ""); error = null },
            label = "Trigger word",
            placeholder = "e.g. chrome",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("Action", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceCommandAction.entries.forEach { opt ->
                val selected = opt == action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(14.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .border(1.dp, if (selected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f), ContinuousRoundedRectangle(14.dp))
                        .clickable { action = opt; error = null }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opt.label, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp)
                        Text(opt.description, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                    if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (action.supportsTargetApp) {
            Spacer(modifier = Modifier.height(14.dp))
            val placeholderText = if (action.needsTargetApp) "Choose an app…" else "Choose a note app (optional)"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), ContinuousRoundedRectangle(14.dp))
                    .clickable { viewModel.loadInstalledApps(); showAppPicker = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (label.isBlank()) placeholderText else label,
                    color = if (label.isBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (label.isNotBlank() && !action.needsTargetApp) {
                    Text("Clear", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                        modifier = Modifier.clickable { pkg = ""; label = "" })
                }
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SheetPrimaryButton {
            when {
                trigger.isBlank() -> error = "Trigger word cannot be empty"
                viewModel.isReserved(trigger) -> error = "\"${trigger.lowercase()}\" is reserved"
                action.needsTargetApp && pkg.isBlank() -> error = "Choose an app to open"
                else -> onSave(trigger, action, pkg, label)
            }
        }
    }

    if (showAppPicker) {
        AppPickerSheet(
            viewModel = viewModel,
            onDismiss = { showAppPicker = false },
            onPick = { app -> pkg = app.packageName; label = app.label; showAppPicker = false }
        )
    }
}

@Composable
private fun AppPickerSheet(
    viewModel: VoiceCommandViewModel,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
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
        if (apps.isEmpty()) {
            Text("Loading apps…", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Text(
                        app.label,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(12.dp))
                            .clickable { onPick(app) }
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}
