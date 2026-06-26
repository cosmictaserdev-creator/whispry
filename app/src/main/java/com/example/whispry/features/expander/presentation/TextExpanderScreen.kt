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
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Text Expander",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f))
                        .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add shortcut",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

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

        // Add Expander Popup Dialog
        if (showAddDialog) {
            AddExpanderDialog(
                onDismiss = { showAddDialog = false },
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
private fun AddExpanderDialog(
    onDismiss: () -> Unit,
    onSave: (shortcut: String, expansion: String) -> Unit
) {
    var shortcut by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    
    val accentColor = androidx.compose.ui.graphics.Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Shortcut",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = shortcut,
                    onValueChange = {
                        shortcut = it.replace(" ", "") // No spaces in shortcuts
                        if (shortcut.isNotEmpty()) errorText = null
                    },
                    label = { Text("Shortcut word") },
                    placeholder = { Text("e.g. sig") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = expansion,
                    onValueChange = {
                        expansion = it
                        if (expansion.isNotEmpty()) errorText = null
                    },
                    label = { Text("Expanded text") },
                    placeholder = { Text("e.g. Best regards, Cosmic") },

                    minLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shortcut.isBlank()) {
                        errorText = "Shortcut word cannot be empty"
                    } else if (expansion.isBlank()) {
                        errorText = "Expanded text cannot be empty"
                    } else {
                        onSave(shortcut, expansion)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF12121A),
        shape = RoundedCornerShape(20.dp)
    )
}
