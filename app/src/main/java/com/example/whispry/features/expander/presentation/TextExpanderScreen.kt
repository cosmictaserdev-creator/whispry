// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.SheetPrimaryButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.components.WhispryCard
import com.example.whispry.ui.components.WhispryDetailScaffold
import com.example.whispry.ui.components.WhispryEmptyState
import com.example.whispry.ui.components.WhispryHero
import com.example.whispry.ui.components.WhispryHeroKeys
import com.example.whispry.ui.components.WhispryPill
import com.kyant.backdrop.backdrops.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextExpanderScreen(
    viewModel: TextExpanderViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val expanders by viewModel.expanders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    WhispryDetailScaffold(
        title = "Text Expander",
        onBack = { navController.popBackStack() },
        subtitle = "Say the shortcut word during recording to paste the full expanded text automatically.",
        pushBackProgress = if (showAddDialog) sheetProgress else 0f,
        hero = hero,
        heroKey = WhispryHeroKeys.TextExpander,
        modifier = modifier,
        headerActions = {
            HeaderActionButton(Icons.Rounded.Add, "Add shortcut") { showAddDialog = true }
        }
    ) {
        if (expanders.isEmpty()) {
            WhispryEmptyState(
                title = "No shortcuts added yet",
                hint = "Tap + to create your first expander rule"
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(expanders, key = { it.id }) { expander ->
                    ExpanderCard(
                        modifier = Modifier.animateItem(),
                        expander = expander,
                        onDelete = { viewModel.deleteExpander(expander) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpanderSheet(
            onDismiss = { showAddDialog = false },
            onDragProgress = { sheetProgress = it },
            onSave = { shortcut, expansion ->
                viewModel.saveExpander(shortcut, expansion)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ExpanderCard(
    expander: TextExpanderEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    WhispryCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WhispryPill(expander.shortcut)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = expander.expansion,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 4
                )
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
private fun AddExpanderSheet(
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit
) {
    var shortcut by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    WhispryBottomSheet(
        title = "New Shortcut",
        onDismiss = onDismiss,
        heightFraction = 0.8f,
        onDragProgress = onDragProgress
    ) {
        Text(
            text = "Say the shortcut word during recording to paste the full text.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SheetTextField(
            value = shortcut,
            onValueChange = { shortcut = it.replace(" ", ""); if (it.isNotEmpty()) errorText = null },
            label = "Shortcut word",
            placeholder = "e.g. sig",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        SheetTextField(
            value = expansion,
            onValueChange = { expansion = it; if (it.isNotEmpty()) errorText = null },
            label = "Expanded text",
            placeholder = "e.g. Best regards, Cosmic",
            minLines = 4
        )
        errorText?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SheetPrimaryButton {
            when {
                shortcut.isBlank() -> errorText = "Shortcut word cannot be empty"
                expansion.isBlank() -> errorText = "Expanded text cannot be empty"
                else -> onSave(shortcut, expansion)
            }
        }
    }
}
