package com.example.whispry.presentation.history

import androidx.activity.compose.BackHandler
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
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun HistoryDetailScreen(
    title: String,
    isPinnedOnly: Boolean,
    viewModel: HistoryViewModel,
    navController: NavController,
    backdrop: Backdrop,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var isSearchActive by remember { mutableStateOf(false) }

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
        label = "SearchProgress"
    )

    // Liquid Glass: Capture list content
    val listBackdrop = rememberLayerBackdrop { drawContent() }
    val glassBackdrop = rememberCombinedBackdrop(backdrop, listBackdrop)

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

    val filteredList = remember(state.filteredTranscripts, isPinnedOnly) {
        if (isPinnedOnly) state.filteredTranscripts.filter { it.isPinned }
        else state.filteredTranscripts
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content Layer
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(listBackdrop)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (0.02f * searchProgress)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - (0.3f * searchProgress)
                    }
                    .blur(
                        radius = (lerp(0.dp, 12.dp, searchProgress))
                            .times(if (state.searchQuery.isEmpty()) 1f else 0.2f)
                            .coerceAtLeast(0.dp)
                    ),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 140.dp,
                    bottom = 40.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = !isSearchActive 
            ) {
                items(filteredList, key = { it.id }) { transcript ->
                    Box(modifier = Modifier.animateItem()) {
                        LibraryTranscriptItem(
                            transcript = transcript,
                            backdrop = backdrop,
                            onDelete = { viewModel.onIntent(HistoryIntent.DeleteTranscript(transcript.id)) },
                            onCopy = { viewModel.onIntent(HistoryIntent.CopyToClipboard(transcript.text)) },
                            onTogglePin = { viewModel.onIntent(HistoryIntent.TogglePin(transcript.id)) }
                        )
                    }
                }
                
                if (filteredList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                            Text("No items found", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Header & Search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
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
                    backdrop = glassBackdrop
                )
            }
        }
    }
}
