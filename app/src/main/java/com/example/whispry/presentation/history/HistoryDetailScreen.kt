// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.history

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.whispry.util.TranscriptExporter
import com.example.whispry.util.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.example.whispry.domain.model.Transcript
import com.example.whispry.ui.util.adaptive.MasterDetailScaffold
import com.example.whispry.ui.util.adaptive.currentWidthSizeClass
import com.example.whispry.ui.util.adaptive.gridColumnsFor
import com.example.whispry.ui.util.adaptive.masterDetailEnabledFor
import com.example.whispry.ui.util.gridItems
import com.example.whispry.ui.util.TopFadeScrim

@Composable
fun HistoryDetailScreen(
    title: String,
    isPinnedOnly: Boolean,
    viewModel: HistoryViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val gridColumns = gridColumnsFor(currentWidthSizeClass())
    val isMasterDetail = masterDetailEnabledFor(currentWidthSizeClass())
    var isSearchActive by remember { mutableStateOf(false) }

    var exportText by remember { mutableStateOf("") }
    var exportingTranscript by remember { mutableStateOf<Transcript?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            val textToWrite = exportText
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(textToWrite.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
        label = "SearchProgress"
    )

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.onIntent(HistoryIntent.Search(""))
        focusManager.clearFocus()
    }

    DisposableEffect(isSearchActive) {
        onSearchActiveChange(isSearchActive)
        onDispose {
            if (isSearchActive) onSearchActiveChange(false)
        }
    }

    val filteredList = remember(state.filteredTranscripts, state.pinnedTranscripts, isPinnedOnly) {
        if (isPinnedOnly) state.pinnedTranscripts
        else state.filteredTranscripts
    }

    // The list + header/search overlay. Filled into either the master pane (Expanded)
    // or the whole screen (compact).
    val masterContent: @Composable () -> Unit = {
      Box(modifier = Modifier.fillMaxSize()) {
        // Content Layer
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (0.02f * searchProgress)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - (0.3f * searchProgress)

                        // Render Effect blur
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && searchProgress > 0f && state.searchQuery.isEmpty()) {
                            val blurPx = (lerp(0.dp, 12.dp, searchProgress)).toPx()
                            if (blurPx > 0.1f) {
                                renderEffect = RenderEffect.createBlurEffect(
                                    blurPx, blurPx, Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                    },
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 195.dp,
                    bottom = 40.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = !isSearchActive 
            ) {
                item {
                    LocalStorageNotice(transcriptCount = state.transcripts.size)
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                            Text("No items found", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    gridItems(
                        items = filteredList,
                        columns = gridColumns,
                        horizontalSpacing = 16.dp
                    ) { transcript ->
                        LibraryTranscriptItemOptimized(
                            transcript = transcript,
                            onDelete = { viewModel.onIntent(HistoryIntent.DeleteTranscript(transcript.id)) },
                            onCopy = { viewModel.onIntent(HistoryIntent.CopyToClipboard(transcript.text)) },
                            onTogglePin = { viewModel.onIntent(HistoryIntent.TogglePin(transcript.id)) },
                            onOpenDetail = { viewModel.onIntent(HistoryIntent.OpenDetail(transcript)) },
                            isReformatting = state.reformattingIds.contains(transcript.id),
                            onCopyOriginal = { viewModel.onIntent(HistoryIntent.CopyToClipboard(transcript.rawText)) },
                            onChangePreset = { viewModel.onIntent(HistoryIntent.ChangePreset(transcript.id, it)) }
                        )
                    }
                }
            }
        }

        // Darkening top bar consistent with the other screens.
        TopFadeScrim(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 175.dp)
        )

        // Header & Search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 48.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = -40f * searchProgress
                            alpha = 1f - searchProgress
                        }
                        .height(lerp(56.dp, 0.dp, searchProgress).coerceAtLeast(0.dp))
                ) {
                    if (searchProgress < 0.9f) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.background(Color.White.copy(0.05f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                RubberySearchBar(
                    query = state.searchQuery,
                    progress = searchProgress,
                    onQueryChange = { viewModel.onIntent(HistoryIntent.Search(it)) },
                    onSearchActiveChange = { isSearchActive = it },
                    backdrop = backdrop
                )
            }
        }
      } // end masterContent box
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // At Expanded width the list and detail sit side by side; otherwise the list
        // fills the screen and selection opens the tap-to-open dialog below.
        if (isMasterDetail) {
            MasterDetailScaffold(
                master = { masterContent() },
                detail = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        val selected = state.selectedTranscript
                        if (selected != null) {
                            TranscriptDetailContent(
                                transcript = selected,
                                onDismiss = { viewModel.onIntent(HistoryIntent.OpenDetail(null)) },
                                onCopy = { viewModel.onIntent(HistoryIntent.CopyToClipboard(it)) },
                                onExport = { exportingTranscript = selected },
                                modifier = Modifier.fillMaxHeight()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a transcript to view it here", color = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            )
        } else {
            masterContent()
        }

        // Detail overlay — tap-to-open dialog, compact widths only.
        if (!isMasterDetail && state.selectedTranscript != null) {
            TranscriptDetailView(
                transcript = state.selectedTranscript!!,
                onDismiss = { viewModel.onIntent(HistoryIntent.OpenDetail(null)) },
                onCopy = { viewModel.onIntent(HistoryIntent.CopyToClipboard(it)) },
                onExport = { exportingTranscript = state.selectedTranscript },
                backdrop = backdrop
            )
        }

        val transcriptToExport = exportingTranscript
        if (transcriptToExport != null) {
            ExportBottomSheet(
                transcript = transcriptToExport,
                onSaveToFile = { filename, content ->
                    exportText = content
                    exportLauncher.launch(filename)
                },
                onDismiss = { exportingTranscript = null }
            )
        }
    }
}
