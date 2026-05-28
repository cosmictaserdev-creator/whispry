package com.example.whispry.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.example.whispry.domain.model.Transcript
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.common.Screen
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
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
fun HistoryScreen(
    viewModel: HistoryViewModel,
    navController: NavController,
    backdrop: Backdrop,
    onSearchActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showFilterMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Unified animation progress with faster exit
    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = if (isSearchActive) 
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
        else 
            tween(350, easing = FastOutSlowInEasing),
        label = "SearchProgress"
    )

    // Liquid Glass: Capture list content for backdrops
    val listBackdrop = rememberLayerBackdrop { drawContent() }
    val glassBackdrop = rememberCombinedBackdrop(backdrop, listBackdrop)

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
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(listBackdrop)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (0.02f * searchProgress)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - (0.3f * searchProgress)
                        
                        // Render Effect blur (skip Recomposition)
                        // Use a stable check to prevent blur "jumps" when query clears
                        if (searchProgress > 0.01f && state.searchQuery.isEmpty()) {
                            val blurPx = (lerp(0.dp, 12.dp, searchProgress)).toPx()
                            if (blurPx > 0.1f) {
                                renderEffect = RenderEffect.createBlurEffect(
                                    blurPx, blurPx, Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                    },
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 150.dp,
                    bottom = 140.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                userScrollEnabled = !isSearchActive 
            ) {
                // Favorites Section
                if (state.pinnedTranscripts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Favorites", 
                            icon = Icons.Rounded.PushPin,
                            onSeeMore = { navController.navigate(Screen.FavoriteDetails.route) }
                        )
                    }
                    items(state.pinnedTranscripts.take(3), key = { "pinned_preview_${it.id}" }) { transcript ->
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
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Recent Section
                if (state.recentTranscripts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recents", 
                            icon = Icons.Rounded.History,
                            onSeeMore = { navController.navigate(Screen.RecentDetails.route) }
                        )
                    }
                    items(state.recentTranscripts.take(5), key = { "recent_preview_${it.id}" }) { transcript ->
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
                }

                if (!state.isLoading && state.filteredTranscripts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No results found", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Top Panel Container (Bleeds outside screen)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp + 100.dp)
        ) {
            // 1. Gradual Blur Layer (Samples glassBackdrop)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.7f to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .drawBackdrop(
                        backdrop = glassBackdrop,
                        shape = {
                            ContinuousRoundedRectangle(
                                bottomStart = lerp(32.dp, 0.dp, searchProgress).coerceAtLeast(0.dp),
                                bottomEnd = lerp(32.dp, 0.dp, searchProgress).coerceAtLeast(0.dp)
                            )
                        },
                        effects = {
                            vibrancy()
                            blur(lerp(12.dp, 24.dp, searchProgress).coerceAtLeast(0.dp).toPx())
                            lens(40f, 40f, depthEffect = true, chromaticAberration = true)
                        },
                        onDrawSurface = {
                            drawRect(Color.Black.copy(alpha = 0.15f + (0.05f * searchProgress)))
                        }
                    )
            )


            // 3. Content Panel
            Column(
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                    .padding(horizontal = 50.dp + 32.dp, vertical = 12.dp)
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
                                    .drawBackdrop(
                                        backdrop = glassBackdrop,
                                        shape = { CircleShape },
                                        effects = {
                                            vibrancy()
                                            blur(6.dp.toPx())
                                            lens(15f, 15f, depthEffect = true, chromaticAberration = true)
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.08f))
                                        }
                                    )
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
                    backdrop = glassBackdrop
                )
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
                backdrop = glassBackdrop
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
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
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
                    scaleY = 1f / pressScale
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(100.dp) },
                    effects = {
                        vibrancy()
                        blur(3.dp.toPx())
                        lens(20f , 20f , depthEffect = true , chromaticAberration = true)
                    },
                    onDrawSurface = {
                        drawRect(Color.Gray.copy(alpha = 0.1f * (1f - progress)))
                        drawRect(Color.Black.copy(alpha = 0.05f + (0.6f * progress)))
                    }
                )
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
                    val s = 1f - (0.05f * (1f - pressScale))
                    scaleX = s
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
                    cursorBrush = SolidColor(WhispryTheme.colors.accent),
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
                color = WhispryTheme.colors.accent,
                fontWeight = FontWeight.SemiBold
            )
        }
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
            Icon(icon, null, tint = WhispryTheme.colors.accent.copy(0.7f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
        }
        Text(
            "See All",
            modifier = Modifier.clickable { onSeeMore() },
            style = MaterialTheme.typography.labelLarge,
            color = WhispryTheme.colors.accent,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LibraryTranscriptItem(
    transcript: Transcript,
    backdrop: Backdrop,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit
) {
    GlassCard(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = transcript.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.PushPin, null, tint = if (transcript.isPinned) WhispryTheme.colors.accent else Color.White.copy(0.2f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(transcript.relativeTime, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                Text(String.format("%.1fs", transcript.durationMs / 1000f), style = MaterialTheme.typography.labelSmall, color = WhispryTheme.colors.accent)
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
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(3.dp.toPx())
                        lens(30f, 30f ,chromaticAberration = true , depthEffect = true)},
                    shadow = { Shadow(alpha = 0.5f, radius = 40.dp) },
                    onDrawSurface = { drawRect(Color.Black.copy(0.75f)) }
                )
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
        Text(label, color = if (isSelected) WhispryTheme.colors.accent else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) Icon(Icons.Rounded.Check, null, tint = WhispryTheme.colors.accent, modifier = Modifier.size(16.dp))
    }
}
