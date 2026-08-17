// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.hiddenapps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
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
import com.example.whispry.features.tone.presentation.AppInfo
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.components.WhispryCard
import com.example.whispry.ui.components.WhispryDetailScaffold
import com.example.whispry.ui.components.WhispryEmptyState
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(
    viewModel: HiddenAppsViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    WhispryDetailScaffold(
        title = "Hidden Apps",
        onBack = { navController.popBackStack() },
        subtitle = "While any app on this list is open, Whispry's widgets stay out of the way — the keyboard button and the side switch both hide until you leave the app. Useful for games and other fullscreen apps.",
        hero = hero,
        heroKey = WhispryHeroKeys.HiddenApps,
        modifier = modifier,
        headerActions = {
            HeaderActionButton(Icons.Rounded.Add, "Add app") { showAddDialog = true }
        }
    ) {
        if (hiddenApps.isEmpty()) {
            WhispryEmptyState(
                title = "No hidden apps yet",
                hint = "Tap + to hide Whispry's widgets in an app"
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(hiddenApps.sorted(), key = { it }) { packageName ->
                    HiddenAppCard(
                        packageName = packageName,
                        displayName = installedApps.firstOrNull { it.packageName == packageName }?.appName ?: packageName,
                        onRemove = { viewModel.setHidden(packageName, false) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddHiddenAppSheet(
            installedApps = installedApps,
            alreadyHiddenPackages = hiddenApps,
            onDismiss = { showAddDialog = false },
            onAdd = { packageName ->
                viewModel.setHidden(packageName, true)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HiddenAppCard(
    packageName: String,
    displayName: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    WhispryCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = packageName,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Widgets hidden while this app is open",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddHiddenAppSheet(
    installedApps: List<AppInfo>,
    alreadyHiddenPackages: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(installedApps, alreadyHiddenPackages, query) {
        val available = installedApps.filter { it.packageName !in alreadyHiddenPackages }
        if (query.isBlank()) available
        else available.filter {
            it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    WhispryBottomSheet(
        title = "Add Hidden App",
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
            Text(
                if (installedApps.all { it.packageName in alreadyHiddenPackages }) "All apps are hidden"
                else "No apps found",
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAdd(app.packageName) }
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(app.packageName, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                        Icon(
                            Icons.Rounded.Add,
                            null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
