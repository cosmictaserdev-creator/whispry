package com.example.whispry.features.myinfo.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import com.example.whispry.ui.components.SheetPrimaryButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun MyInfoScreen(
    viewModel: MyInfoViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    var editTarget by remember { mutableStateOf<MyInfoEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (showDialog) 1f - 0.08f * sheetProgress else 1f
                    scaleX = s; scaleY = s
                }
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "My Info",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { editTarget = null; showDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.Add, "Add", tint = Color.White)
                }
            }

            Text(
                text = "Save personal details, then paste any of them by voice with \"insert <name>\" (e.g. \"insert address\").",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    MyInfoCard(
                        item = item,
                        onEdit = { editTarget = item; showDialog = true },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }
        }

        if (showDialog) {
            MyInfoSheet(
                existing = editTarget,
                isReserved = viewModel::isReserved,
                onDismiss = { showDialog = false },
                onDragProgress = { sheetProgress = it },
                onSave = { key, value ->
                    viewModel.save(key, value)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun MyInfoCard(
    item: MyInfoEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color.White
    val hasValue = item.value.isNotBlank()
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
                        .background(accent.copy(alpha = 0.1f))
                        .border(0.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(item.key, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (hasValue) item.value else "Tap to add your ${item.key}",
                    color = if (hasValue) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
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
                Icon(Icons.Rounded.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MyInfoSheet(
    existing: MyInfoEntity?,
    isReserved: (String) -> Boolean,
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onSave: (key: String, value: String) -> Unit
) {
    var key by remember { mutableStateOf(existing?.key ?: "") }
    var value by remember { mutableStateOf(existing?.value ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    WhispryBottomSheet(
        title = if (existing == null) "New Info" else "Edit Info",
        onDismiss = onDismiss,
        heightFraction = 0.82f,
        onDragProgress = onDragProgress
    ) {
        Text(
            text = "Paste this later by voice with \"insert ${key.ifBlank { "<name>" }}\".",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SheetTextField(
            value = key,
            onValueChange = { key = it.replace(" ", ""); error = null },
            label = "Name (one word)",
            placeholder = "e.g. address",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        SheetTextField(
            value = value,
            onValueChange = { value = it; error = null },
            label = "Value",
            placeholder = "e.g. 123 Main St, Springfield",
            minLines = 4
        )
        error?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SheetPrimaryButton {
            when {
                key.isBlank() -> error = "Name cannot be empty"
                isReserved(key) -> error = "\"${key.lowercase()}\" is a reserved word"
                else -> onSave(key, value)
            }
        }
    }
}
