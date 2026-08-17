// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.ui.components.WhispryCard
import com.example.whispry.ui.components.WhispryDetailScaffold
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.example.whispry.ui.components.WhispryPill

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val state by viewModel.state.collectAsState()

    WhispryDetailScaffold(
        title = "Updates",
        onBack = { navController.popBackStack() },
        subtitle = "Whispry checks GitHub Releases for new versions — no Play Store required.",
        hero = hero,
        heroKey = WhispryHeroKeys.Updates,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                WhispryCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Current version",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            WhispryPill(state.currentVersion)
                        }
                        Spacer(Modifier.height(16.dp))
                        UpdateStatusRow(state) { viewModel.onIntent(UpdateScreenIntent.CheckForUpdate) }
                    }
                }
            }

            val release = when (val phase = state.phase) {
                is UpdatePhase.Available -> phase.release
                is UpdatePhase.Downloading -> phase.release
                is UpdatePhase.ReadyToInstall -> phase.release
                else -> null
            }

            if (release != null) {
                item {
                    WhispryCard {
                        Column {
                            Text(release.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(release.tagName, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = release.notes.ifBlank { "No release notes provided." },
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
                item {
                    when (val phase = state.phase) {
                        is UpdatePhase.Available -> Button(
                            onClick = { viewModel.onIntent(UpdateScreenIntent.DownloadAndInstall) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Download & Install") }

                        is UpdatePhase.Downloading -> Column {
                            LinearProgressIndicator(
                                progress = { state.downloadProgressPct / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.downloadProgressPct}%",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }

                        is UpdatePhase.ReadyToInstall -> Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Opening installer…") }

                        else -> Unit
                    }
                }
            }

            val error = (state.phase as? UpdatePhase.Error)?.message
            if (error != null) {
                item {
                    Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp)
                }
            }
        }
    }

    if (state.needsInstallPermission) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(UpdateScreenIntent.InstallPermissionDialogDismissed) },
            title = { Text("Allow installing updates") },
            text = {
                Text(
                    "Whispry needs permission to install app updates. You'll be taken to " +
                        "system settings — turn on \"Allow from this source\" for Whispry, then " +
                        "come back and tap Download & Install again."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(UpdateScreenIntent.OpenInstallPermissionSettings) }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(UpdateScreenIntent.InstallPermissionDialogDismissed) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UpdateStatusRow(state: UpdateScreenState, onCheck: () -> Unit) {
    when (val phase = state.phase) {
        UpdatePhase.Idle -> TextButton(onClick = onCheck) { Text("Check for updates") }

        UpdatePhase.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Checking for updates…", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }

        UpdatePhase.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "You're up to date",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCheck) {
                Icon(Icons.Rounded.Refresh, "Check again", tint = Color.White.copy(alpha = 0.5f))
            }
        }

        is UpdatePhase.Available -> Text(
            "Update available: ${phase.release.tagName}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        is UpdatePhase.Downloading -> Text(
            "Downloading ${phase.release.tagName}…",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        is UpdatePhase.ReadyToInstall -> Text(
            "Ready to install ${phase.release.tagName}",
            color = Color.White,
            fontSize = 13.sp
        )

        is UpdatePhase.Error -> TextButton(onClick = onCheck) { Text("Retry check") }
    }
}
