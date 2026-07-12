package com.example.whispry.features.memory.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whispry.data.local.db.MemoryEntity
import com.example.whispry.ui.components.HeaderActionButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    hero: WhispryHero? = null
) {
    val memories by viewModel.memories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    WhispryDetailScaffold(
        title = "Memory Bank",
        onBack = { navController.popBackStack() },
        subtitle = "Personal facts (e.g., 'Rahul is my boss') stored here are used by AI to provide smarter, context-aware transcriptions.",
        pushBackProgress = if (showAddDialog) sheetProgress else 0f,
        hero = hero,
        heroKey = WhispryHeroKeys.Memory,
        modifier = modifier,
        headerActions = {
            HeaderActionButton(Icons.Rounded.Add, "Add memory") { showAddDialog = true }
        }
    ) {
        if (memories.isEmpty()) {
            WhispryEmptyState(
                title = "Your Memory Bank is empty",
                hint = "Tap + to add your first personal fact"
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(
                        modifier = Modifier.animateItem(),
                        memory = memory,
                        onDelete = { viewModel.deleteMemory(memory) },
                        onToggleActive = { viewModel.toggleMemoryActive(memory) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemorySheet(
            onDismiss = { showAddDialog = false },
            onDragProgress = { sheetProgress = it },
            onSave = { key, value ->
                viewModel.saveMemory(key, value)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryEntity,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color.White
    WhispryCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f).alpha(if (memory.isActive) 1f else 0.4f)) {
                Text(
                    text = memory.key,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.value,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onToggleActive,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (memory.isActive) accentColor.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = if (memory.isActive) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = "Toggle Active",
                    tint = if (memory.isActive) accentColor else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddMemorySheet(
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onSave: (key: String, value: String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    WhispryBottomSheet(
        title = "Add to Memory Bank",
        onDismiss = onDismiss,
        heightFraction = 0.8f,
        onDragProgress = onDragProgress
    ) {
        Text(
            text = "Stored facts give the AI context for smarter, personalized transcriptions.",
            color = WhispryTokens.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SheetTextField(
            value = key,
            onValueChange = { key = it; if (it.isNotEmpty()) errorText = null },
            label = "Topic / Key",
            placeholder = "e.g. My Boss",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        SheetTextField(
            value = value,
            onValueChange = { value = it; if (it.isNotEmpty()) errorText = null },
            label = "Personal Fact / Value",
            placeholder = "e.g. Rahul",
            minLines = 3
        )
        errorText?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SheetPrimaryButton {
            when {
                key.isBlank() -> errorText = "Topic cannot be empty"
                value.isBlank() -> errorText = "Fact cannot be empty"
                else -> onSave(key, value)
            }
        }
    }
}
