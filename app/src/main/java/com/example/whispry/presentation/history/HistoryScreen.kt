package com.example.whispry.presentation.history

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.whispry.util.TranscriptExporter
import com.example.whispry.util.ExportFormat
import kotlinx.coroutines.Dispatchers
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.example.whispry.domain.model.Transcript
import com.example.whispry.R
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.ui.components.WhispryBottomSheet
import com.example.whispry.ui.util.adaptive.currentWidthSizeClass
import com.example.whispry.ui.util.adaptive.gridColumnsFor
import com.example.whispry.ui.util.gridItems
import com.example.whispry.ui.util.TopFadeScrim
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LocalStorageNotice(
    transcriptCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Your transcripts stay on this device",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "Deleting the app permanently removes all $transcriptCount saved transcripts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    navController: NavController,
    backdrop: LayerBackdrop,
    onSearchActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridColumns = gridColumnsFor(currentWidthSizeClass())
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var showFilterMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    // Measured header height so the list never slides under the search bar (fixes landscape overlap).
    var headerHeightPx by remember { mutableIntStateOf(0) }
    // Whether the user is actively filtering — drives showing the flat results list vs the sections.
    val searching = state.searchQuery.isNotBlank()
    val listState = rememberLazyListState()
    // When a new transcript appears (a trigger just finished), jump back to the top to show it.
    val newestId = state.transcripts.firstOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId != null && !searching) listState.animateScrollToItem(0)
    }

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

    // Smooth, jitter-free spring for both opening and closing (iOS-like, no overshoot wobble).
    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        label = "SearchProgress"
    )

    // Handle back gesture to close search
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        focusManager.clearFocus()
        // Clear query after a small delay to keep animation smooth
    }

    // Reset query when search is fully closed to avoid list jump during animation
    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            kotlinx.coroutines.delay(350)
            viewModel.onIntent(HistoryIntent.Search(""))
        }
    }

    // Reset global state on navigate/dispose
    DisposableEffect(isSearchActive) {
        onSearchActiveChange(isSearchActive)
        onDispose {
            if (isSearchActive) onSearchActiveChange(false)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Content Layer - Blurs and recedes when search is active
        Box(modifier = Modifier.fillMaxSize()) {
            // Content recedes/blurs only while the search field is focused and EMPTY (the inviting
            // "start typing" state). Once a query exists we keep it crisp and scrollable so the
            // changing results are clearly visible.
            val recede = searchProgress * (if (state.searchQuery.isEmpty()) 1f else 0f)
            val fallbackTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                dimensionResource(R.dimen.library_header_top_offset)
            val topPadding = if (headerHeightPx > 0) with(density) { headerHeightPx.toDp() } + 12.dp else fallbackTop
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (0.02f * recede)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - (0.3f * recede)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && recede > 0.01f) {
                            val blurPx = (lerp(0.dp, 12.dp, recede)).toPx()
                            if (blurPx > 0.1f) {
                                renderEffect = RenderEffect.createBlurEffect(
                                    blurPx, blurPx, Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                    },
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = dimensionResource(R.dimen.screen_bottom_padding),
                    start = dimensionResource(R.dimen.screen_horizontal_padding),
                    end = dimensionResource(R.dimen.screen_horizontal_padding)
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                userScrollEnabled = !(isSearchActive && state.searchQuery.isEmpty())
            ) {
                if (searching) {
                    // --- Search results: flat, full-width, animated reorder on sort/add/remove ---
                    items(state.filteredTranscripts, key = { it.id }) { transcript ->
                        LibraryTranscriptItemOptimized(
                            modifier = Modifier.animateItem(),
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
                    if (!state.isLoading && state.filteredTranscripts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text(text = "No results for \"${state.searchQuery}\"", color = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    }
                } else {
                    item {
                        LocalStorageNotice(transcriptCount = state.transcripts.size)
                    }

                    // Favorites Section
                    if (state.pinnedTranscripts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Favorites",
                                icon = Icons.Rounded.Star,
                                onSeeMore = { navController.navigate(com.example.whispry.navigation.Route.FavoriteDetails) }
                            )
                        }
                        gridItems(
                            items = state.pinnedTranscripts.take(3),
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
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Recent Section — same rich card as Favorites; reflects the sort filter.
                    val recents = state.filteredTranscripts.take(8)
                    if (recents.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Recents",
                                icon = Icons.Rounded.History,
                                onSeeMore = { navController.navigate(com.example.whispry.navigation.Route.RecentDetails) }
                            )
                        }
                        gridItems(
                            items = recents,
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

                    if (!state.isLoading && state.transcripts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text(text = "No transcripts yet", color = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        // Top bar: a darkening fade behind a full-width header (24dp margins), so the title and
        // search bar are as wide as the Recents screen and aligned with the list below.
        TopFadeScrim(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 180.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Library",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { showFilterMenu = !showFilterMenu },
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
                            ) {
                                Icon(Icons.Rounded.FilterList, null, tint = Color.White)
                            }
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

                // Local, on-device autocomplete: word completions mined from existing transcripts.
                val suggestions = remember(state.searchQuery, state.transcripts) {
                    val q = state.searchQuery.trim().lowercase()
                    if (q.length < 2) emptyList()
                    else state.transcripts.asSequence()
                        .flatMap { it.text.split(Regex("\\s+")).asSequence() }
                        .map { it.trim('.', ',', '!', '?', ';', ':', '"', '\'', '(', ')').lowercase() }
                        .filter { it.length > q.length && it.startsWith(q) }
                        .distinct()
                        .take(6)
                        .toList()
                }
                AnimatedVisibility(
                    visible = isSearchActive && suggestions.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { word ->
                            SuggestionChip(word) { viewModel.onIntent(HistoryIntent.Search(word)) }
                        }
                    }
                }
        }

        if (showFilterMenu) {
            FilterMenu(
                currentOrder = state.sortOrder,
                onOrderSelect = {
                    viewModel.onIntent(HistoryIntent.ChangeSortOrder(it))
                    showFilterMenu = false
                },
                onDismiss = { showFilterMenu = false },
                backdrop = backdrop
            )
        }

        // Detail View Overlay
        if (state.selectedTranscript != null) {
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

@Composable
fun RubberySearchBar(
    query: String,
    progress: Float,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    backdrop: Backdrop
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Local state for immediate typing feedback
    var localQuery by remember(query) { mutableStateOf(query) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "SearchPressScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(100.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(100.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(100.dp))
                .clickable(
                    enabled = progress < 0.5f, // Disable clickable area when expanded to allow text field focus
                    interactionSource = interactionSource, 
                    indication = null,
                    onClick = {
                        onSearchActiveChange(true)
                        focusRequester.requestFocus()
                    }
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.graphicsLayer {
                    alpha = 0.5f + (0.5f * progress)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = localQuery,
                    onValueChange = { 
                        localQuery = it
                        onQueryChange(it) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { 
                            if (it.isFocused) onSearchActiveChange(true) 
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.White),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    decorationBox = { inner ->
                        if (localQuery.isEmpty()) {
                            Text(
                                "Search transcripts...", 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                )
            }
        }

        if (progress > 0.01f) {
            Text(
                "Cancel",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .graphicsLayer { 
                        alpha = progress
                        translationX = 20f * (1f - progress)
                    }
                    .clickable {
                        onSearchActiveChange(false)
                        focusManager.clearFocus()
                    },
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ContinuousRoundedRectangle(100.dp))
            .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, ContinuousRoundedRectangle(100.dp))
            .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.85f))
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, onSeeMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
        }
        Text(
            "See All",
            modifier = Modifier.clickable { onSeeMore() },
            style = MaterialTheme.typography.labelLarge,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LibraryTranscriptItemOptimized(
    transcript: Transcript,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenDetail: () -> Unit,
    isReformatting: Boolean = false,
    onCopyOriginal: () -> Unit = {},
    onChangePreset: (com.example.whispry.domain.model.OutputPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "CardScale"
    )

    var showPresetPicker by remember { mutableStateOf(false) }

    // Swipe: drag right → favorite, drag left → delete.
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 88.dp.toPx() }
    val maxSwipePx = thresholdPx * 1.4f
    val swipeOffset = remember(transcript.id) { Animatable(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Reveal layer behind the card: yellow + star when swiping right (favorite),
        // red + trash when swiping left (delete). Dark icons for contrast.
        val darkIcon = Color(0xFF1C1C1E)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousRoundedRectangle(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = (swipeOffset.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(Color(0xFFFFD60A))
            ) {
                Icon(
                    Icons.Rounded.Star, null, tint = darkIcon,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp).size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = (-swipeOffset.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(Color(0xFFFF453A))
            ) {
                Icon(
                    Icons.Rounded.Delete, null, tint = darkIcon,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp).size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                .pointerInput(transcript.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch { swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)) }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    swipeOffset.value <= -thresholdPx -> {
                                        swipeOffset.animateTo(-maxSwipePx, spring(stiffness = Spring.StiffnessMedium))
                                        onDelete()
                                    }
                                    swipeOffset.value >= thresholdPx -> {
                                        // Swipe right always favorites — never toggles back off.
                                        if (!transcript.isPinned) onTogglePin()
                                        swipeOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
                                    }
                                    else -> swipeOffset.animateTo(0f, spring(dampingRatio = 0.7f))
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(4.dp, ContinuousRoundedRectangle(20.dp), spotColor = Color.Black)
                .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, ContinuousRoundedRectangle(20.dp))
                .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onOpenDetail
                )
                .padding(16.dp)
        ) {
            Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isReformatting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    } else {
                        Text(
                            text = transcript.text,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentPreset = try { com.example.whispry.domain.model.OutputPreset.valueOf(transcript.preset) } catch (e: Exception) { com.example.whispry.domain.model.OutputPreset.NONE }
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showPresetPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentPreset.emoji} ${currentPreset.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.ExpandMore, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                
                Row {
                    val starScale = remember { Animatable(1f) }
                    IconButton(
                        onClick = {
                            scope.launch {
                                starScale.animateTo(1.35f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh))
                                starScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                            }
                            onTogglePin()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (transcript.isPinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            null,
                            tint = if (transcript.isPinned) Color(0xFFFFD60A) else Color.White.copy(0.25f),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { scaleX = starScale.value; scaleY = starScale.value }
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(transcript.relativeTime, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                    if (transcript.isPinned) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(4.dp).background(androidx.compose.ui.graphics.Color.White, CircleShape))
                    }
                }
                Text(String.format("%.1fs", transcript.durationMs / 1000f), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
            }
            }
        }
    }

    if (showPresetPicker) {
        PresetPickerBottomSheet(
            currentPreset = try { com.example.whispry.domain.model.OutputPreset.valueOf(transcript.preset) } catch (e: Exception) { com.example.whispry.domain.model.OutputPreset.NONE },
            onPresetSelected = {
                onChangePreset(it)
                showPresetPicker = false
            },
            onDismiss = { showPresetPicker = false }
        )
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f),
            ),
            start = Offset(10f, 10f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

@Composable
fun TranscriptDetailView(
    transcript: Transcript,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onExport: () -> Unit,
    backdrop: Backdrop
) {
    // Tap-to-open detail now uses the shared app bottom sheet for a consistent feel.
    WhispryBottomSheet(
        title = "Detail",
        onDismiss = onDismiss,
        heightFraction = 0.9f
    ) {
        TranscriptDetailBody(
            transcript = transcript,
            onCopy = onCopy,
            onExport = onExport,
            showHeader = false
        )
    }
}

/**
 * Master-detail side-pane panel (Expanded width). Single glass border — no dual outline.
 */
@Composable
fun TranscriptDetailContent(
    transcript: Transcript,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(20.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
            .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
            .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(20.dp))
            .padding(24.dp)
    ) {
        TranscriptDetailBody(
            transcript = transcript,
            onCopy = onCopy,
            onExport = onExport,
            showHeader = true,
            onDismiss = onDismiss,
            scroll = true
        )
    }
}

/**
 * The transcript detail content. Rendered headerless inside the bottom sheet (the sheet supplies
 * its own title/close) and with a header + own scroll inside the side-pane panel.
 */
@Composable
private fun TranscriptDetailBody(
    transcript: Transcript,
    onCopy: (String) -> Unit,
    onExport: () -> Unit,
    showHeader: Boolean,
    onDismiss: () -> Unit = {},
    scroll: Boolean = false,
    modifier: Modifier = Modifier
) {
        Column(
            modifier = modifier.then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
                // Header (side-pane only; the bottom sheet renders its own)
                if (showHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Description, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Detail",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.3f))
                    }
                }
                }

                // Remixed Section (The Pretty version)
                Column {
                    val currentPreset = try { com.example.whispry.domain.model.OutputPreset.valueOf(transcript.preset) } catch (e: Exception) { com.example.whispry.domain.model.OutputPreset.NONE }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentPreset.emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (transcript.preset == "NONE") "RAW TRANSCRIPTION" else "AI ENHANCED (${currentPreset.displayName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (transcript.preset == "NONE") Color.White.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onExport,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Share, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            Surface(
                                onClick = { onCopy(transcript.text) },
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ContentCopy, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = transcript.text,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                // Raw Section (Only if enhanced)
                if (transcript.preset != "NONE") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ORIGINAL AUDIO CAPTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.3f),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            
                            Surface(
                                onClick = { onCopy(transcript.rawText) },
                                color = Color.White.copy(alpha = 0.05f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(20.dp)
                        ) {
                            Text(
                                text = if (transcript.rawText.isBlank()) transcript.text else transcript.rawText,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                // Meta Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetaItem(Icons.Rounded.Timer, String.format("%.1fs", transcript.durationMs / 1000f))
                    MetaItem(Icons.Rounded.CalendarToday, transcript.relativeTime)
                    MetaItem(Icons.Rounded.Language, transcript.languageCode.uppercase())
                }
                
                Spacer(Modifier.height(8.dp))
            }
}

@Composable
fun MetaItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun PresetPickerBottomSheet(
    currentPreset: com.example.whispry.domain.model.OutputPreset,
    onPresetSelected: (com.example.whispry.domain.model.OutputPreset) -> Unit,
    onDismiss: () -> Unit
) {
    WhispryBottomSheet(
        title = "Change Format",
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        scrollableContent = false
    ) {
        Text(
            "AI will re-process the raw text with a new format.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(com.example.whispry.domain.model.OutputPreset.entries) { preset ->
                val isSelected = preset == currentPreset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(16.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { onPresetSelected(preset) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(preset.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preset.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterMenu(
    currentOrder: HistorySortOrder,
    onOrderSelect: (HistorySortOrder) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop
) {
    val menuOffset = remember { Animatable(50f) }
    var rubberOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        menuOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp, end = 24.dp)
                .width(200.dp)
                .offset { IntOffset(rubberOffset.x.roundToInt(), (menuOffset.value + rubberOffset.y).roundToInt()) }
                .graphicsLayer {
                    scaleX = 1f + (rubberOffset.x.coerceIn(-20f, 20f) / 200f)
                    scaleY = 1f + (rubberOffset.y.coerceIn(-20f, 20f) / 400f)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            rubberOffset += dragAmount * 0.3f 
                        },
                        onDragEnd = {
                            rubberOffset = Offset.Zero
                        }
                    )
                }
                .shadow(4.dp, com.kyant.capsule.ContinuousRoundedRectangle(24.dp), spotColor = androidx.compose.ui.graphics.Color.Black)
                    .background(com.example.whispry.ui.theme.WhispryTokens.SurfaceElevated, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                    .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, com.kyant.capsule.ContinuousRoundedRectangle(24.dp))
                .padding(8.dp)
        ) {
            FilterMenuItem("Newest First", currentOrder == HistorySortOrder.DATE_DESC) { onOrderSelect(HistorySortOrder.DATE_DESC) }
            FilterMenuItem("Oldest First", currentOrder == HistorySortOrder.DATE_ASC) { onOrderSelect(HistorySortOrder.DATE_ASC) }
            HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(vertical = 4.dp))
            FilterMenuItem("A - Z", currentOrder == HistorySortOrder.ALPHA_ASC) { onOrderSelect(HistorySortOrder.ALPHA_ASC) }
            FilterMenuItem("Z - A", currentOrder == HistorySortOrder.ALPHA_DESC) { onOrderSelect(HistorySortOrder.ALPHA_DESC) }
        }
    }
}

@Composable
fun FilterMenuItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "FilterScale")

    Row(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (isSelected) androidx.compose.ui.graphics.Color.White else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    transcript: Transcript,
    onSaveToFile: (filename: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.TXT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D0D14),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Export Transcript",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Select a format to save or share your transcript.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(ExportFormat.entries) { format ->
                    val isSelected = format == selectedFormat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedFormat = format }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (format) {
                            ExportFormat.TXT -> Icons.Rounded.Description
                            ExportFormat.SRT -> Icons.Rounded.Subtitles
                            ExportFormat.VTT -> Icons.Rounded.ClosedCaption
                            ExportFormat.JSON -> Icons.Rounded.Code
                            ExportFormat.CSV -> Icons.Rounded.GridOn
                        }
                        Icon(
                            icon,
                            null,
                            tint = if (isSelected) androidx.compose.ui.graphics.Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                format.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else Color.White
                            )
                            Text(
                                format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        val content = when (selectedFormat) {
                            ExportFormat.TXT -> TranscriptExporter.toTxt(transcript)
                            ExportFormat.SRT -> TranscriptExporter.toSrt(transcript)
                            ExportFormat.VTT -> TranscriptExporter.toVtt(transcript)
                            ExportFormat.JSON -> TranscriptExporter.toJson(transcript)
                            ExportFormat.CSV -> TranscriptExporter.toCsv(transcript)
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, content)
                            putExtra(Intent.EXTRA_TITLE, "Share Transcript")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Share", color = Color.White)
                }

                Button(
                    onClick = {
                        val content = when (selectedFormat) {
                            ExportFormat.TXT -> TranscriptExporter.toTxt(transcript)
                            ExportFormat.SRT -> TranscriptExporter.toSrt(transcript)
                            ExportFormat.VTT -> TranscriptExporter.toVtt(transcript)
                            ExportFormat.JSON -> TranscriptExporter.toJson(transcript)
                            ExportFormat.CSV -> TranscriptExporter.toCsv(transcript)
                        }
                        val filename = "transcript_${transcript.timestampMs}.${selectedFormat.extension}"
                        onSaveToFile(filename, content)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhispryTheme.colors.accent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Rounded.Save, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Files", color = Color.White)
                }
            }
        }
    }
}
