package com.example.whispry.features.expander.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.graphicsLayer
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.ui.components.HeaderActionButton
import com.example.whispry.ui.components.ScreenHeader
import com.example.whispry.ui.components.SheetPrimaryButton
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
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
fun TextExpanderScreen(
    viewModel: TextExpanderViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    val expanders by viewModel.expanders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (showAddDialog) 1f - 0.08f * sheetProgress else 1f
                    scaleX = s; scaleY = s
                }
                .padding(horizontal = 24.dp)
        ) {
            // Header Row
            ScreenHeader(
                title = "Text Expander",
                onBack = { navController.popBackStack() },
                actions = {
                    HeaderActionButton(Icons.Rounded.Add, "Add shortcut") { showAddDialog = true }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Say the shortcut word during recording to paste the full expanded text automatically.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (expanders.isEmpty()) {
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
                            text = "No shortcuts added yet",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to create your first expander rule",
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
                    items(expanders, key = { it.id }) { expander ->
                        ExpanderCard(
                            expander = expander,
                            onDelete = { viewModel.deleteExpander(expander) },
                            backdrop = backdrop
                        )
                    }
                }
            }
        }

        // Add Expander bottom sheet
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
}

@Composable
private fun ExpanderCard(
    expander: TextExpanderEntity,
    onDelete: () -> Unit,
    backdrop: LayerBackdrop
) {
    val accentColor = androidx.compose.ui.graphics.Color.White
    val backdropState = rememberLayerBackdrop()

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
                // Shortcut Pill badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = expander.shortcut,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Expansion Text
                Text(
                    text = expander.expansion,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Delete Icon
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
