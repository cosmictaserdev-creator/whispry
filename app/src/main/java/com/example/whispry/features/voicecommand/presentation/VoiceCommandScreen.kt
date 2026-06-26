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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import com.example.whispry.features.voicecommand.domain.model.VoiceCommandAction
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun VoiceCommandScreen(
    viewModel: VoiceCommandViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val commands by viewModel.commands.collectAsState()
    var editTarget by remember { mutableStateOf<VoiceCommandEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f))
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Voice Commands", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { editTarget = null; showDialog = true },
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) { Icon(Icons.Rounded.Add, "Add", tint = Color.White) }
            }

            Text(
                text = "Say a trigger word, then your query — e.g. \"chrome best football player\" searches the web. Open-App launches an app and copies the rest for you to paste.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(commands, key = { it.id }) { cmd ->
                    VoiceCommandCard(
                        command = cmd,
                        onEdit = { editTarget = cmd; showDialog = true },
                        onDelete = { viewModel.delete(cmd) }
                    )
                }
            }
        }

        if (showDialog) {
            VoiceCommandDialog(
                existing = editTarget,
                viewModel = viewModel,
                onDismiss = { showDialog = false },
                onSave = { trigger, action, pkg, label ->
                    viewModel.save(trigger, action, pkg, label)
                    showDialog = false
                }
            )
        }
    }
}

private fun actionOf(name: String): VoiceCommandAction =
    try { VoiceCommandAction.valueOf(name) } catch (e: Exception) { VoiceCommandAction.WEB_SEARCH }

@Composable
private fun VoiceCommandCard(
    command: VoiceCommandEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val action = actionOf(command.action)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(16.dp))
            .background(WhispryTokens.SurfaceElevated, ContinuousRoundedRectangle(16.dp))
            .border(1.dp, WhispryTokens.GlassBorder, ContinuousRoundedRectangle(16.dp))
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(command.triggerWord, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
private fun VoiceCommandDialog(
    existing: VoiceCommandEntity?,
    viewModel: VoiceCommandViewModel,
    onDismiss: () -> Unit,
    onSave: (trigger: String, action: VoiceCommandAction, pkg: String, label: String) -> Unit
) {
    var trigger by remember { mutableStateOf(existing?.triggerWord ?: "") }
    var action by remember { mutableStateOf(existing?.let { actionOf(it.action) } ?: VoiceCommandAction.WEB_SEARCH) }
    var pkg by remember { mutableStateOf(existing?.targetPackage ?: "") }
    var label by remember { mutableStateOf(existing?.targetAppLabel ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    val accent = Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Command" else "Edit Command", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it.replace(" ", ""); error = null },
                    label = { Text("Trigger word") },
                    placeholder = { Text("e.g. chrome") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Action", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VoiceCommandAction.entries.forEach { opt ->
                        val selected = opt == action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .border(1.dp, if (selected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { action = opt; error = null }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opt.label, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                                Text(opt.description, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (action.needsTargetApp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.loadInstalledApps(); showAppPicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (label.isBlank()) "Choose an app…" else label,
                            color = if (label.isBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        trigger.isBlank() -> error = "Trigger word cannot be empty"
                        viewModel.isReserved(trigger) -> error = "\"${trigger.lowercase()}\" is reserved"
                        action.needsTargetApp && pkg.isBlank() -> error = "Choose an app to open"
                        else -> onSave(trigger, action, pkg, label)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) } },
        containerColor = Color(0xFF12121A),
        shape = RoundedCornerShape(20.dp)
    )

    if (showAppPicker) {
        AppPickerDialog(
            viewModel = viewModel,
            onDismiss = { showAppPicker = false },
            onPick = { app -> pkg = app.packageName; label = app.label; showAppPicker = false }
        )
    }
}

@Composable
private fun AppPickerDialog(
    viewModel: VoiceCommandViewModel,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (apps.isEmpty()) {
                    Text("Loading apps…", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Text(
                                app.label,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onPick(app) }
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color.White.copy(alpha = 0.6f)) } },
        containerColor = Color(0xFF12121A),
        shape = RoundedCornerShape(20.dp)
    )
}
