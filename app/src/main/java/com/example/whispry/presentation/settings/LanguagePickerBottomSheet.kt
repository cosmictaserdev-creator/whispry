package com.example.whispry.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.rememberCachedBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun LanguagePickerBottomSheet(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    backdrop: LayerBackdrop
) {
    val languages = remember {
        listOf(
            "Auto" to "Detect Language", "en" to "English", "es" to "Spanish", "fr" to "French",
            "de" to "German", "it" to "Italian", "pt" to "Portuguese", "nl" to "Dutch",
            "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ru" to "Russian",
            "tr" to "Turkish", "ar" to "Arabic", "hi" to "Hindi", "vi" to "Vietnamese",
            "pl" to "Polish", "uk" to "Ukrainian", "id" to "Indonesian", "th" to "Thai"
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        languages.filter { 
            it.second.contains(searchQuery, ignoreCase = true) || it.first.contains(searchQuery, ignoreCase = true)
        }
    }

    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val sheetHeightPx = screenHeightPx * 0.85f
    
    // Use Animatable for the sheet offset
    val offsetY = remember { Animatable(screenHeightPx) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Optimization: Notify progress in a side effect to avoid blocking the main thread
    LaunchedEffect(offsetY.value) {
        val progress = (1f - (offsetY.value / screenHeightPx)).coerceIn(0f, 1f)
        onDragProgress(progress)
    }
    
    // Initial entrance animation
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }

    val dismiss = {
        scope.launch {
            focusManager.clearFocus()
            offsetY.animateTo(screenHeightPx, spring(stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
        Unit
    }

    // Drag handling logic - more efficient than detectVerticalDragGestures for sheets
    val draggableState = rememberDraggableState { delta ->
        if (!isSearchActive) {
            scope.launch {
                val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
                offsetY.snapTo(newOffset)
            }
        }
    }

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "SearchProgress"
    )

    BackHandler { 
        if (isSearchActive) isSearchActive = false
        else dismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, 
                indication = null, 
                onClick = { dismiss() }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .graphicsLayer { 
                    alpha = (1f - (offsetY.value / screenHeightPx)).coerceIn(0f, 1f)
                }
        )

        // The Glass Sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (offsetY.value > screenHeightPx * 0.25f || velocity > 1000f) {
                            dismiss()
                        } else {
                            scope.launch {
                                offsetY.animateTo(0f, spring(dampingRatio = 0.8f))
                            }
                        }
                    }
                )
                .clickable(enabled = false) {} // Consume clicks to avoid dismissing when tapping sheet content
                .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(topStart = 44.dp, topEnd = 44.dp))
                .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(topStart = 44.dp, topEnd = 44.dp))
        ) {
            // Content List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 1f - (0.01f * searchProgress)
                        scaleX = s; scaleY = s
                        alpha = 1f - (0.2f * searchProgress)
                    },
                contentPadding = PaddingValues(top = 180.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = !isSearchActive
            ) {
                items(filteredLanguages, key = { it.first }) { (code, name) ->
                    val isSelected = selectedLanguage == code
                    LanguageItem(name, code, isSelected) { onLanguageSelected(code) }
                }
            }

            // Fixed Header Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .requiredWidth(LocalConfiguration.current.screenWidthDp.dp + 40.dp)
            ) {
                // Header Mask
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(bottomStart = 24.dp, bottomEnd = 24.dp))
                )

                // Header Content
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .padding(horizontal = 20.dp + 24.dp, vertical = 16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(36.dp, 4.dp).background(Color.White.copy(0.2f), CircleShape))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = -30f * searchProgress
                                alpha = 1f - searchProgress
                            }
                            .height(lerp(44.dp, 0.dp, searchProgress).coerceAtLeast(0.dp))
                    ) {
                        if (searchProgress < 0.9f) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Language", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { dismiss() }) { Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.5f)) }
                            }
                        }
                    }

                    RubberySheetSearchBar(
                        query = searchQuery,
                        progress = searchProgress,
                        onQueryChange = { searchQuery = it },
                        onSearchActiveChange = { isSearchActive = it },
                        backdrop = backdrop
                    )
                }
            }
        }
    }
}

@Composable
fun RubberySheetSearchBar(
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
    var localQuery by remember(query) { mutableStateOf(query) }

    val pressScale by animateFloatAsState(if (isPressed) 0.94f else 1f, spring(0.5f, Spring.StiffnessLow))

    Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f).fillMaxHeight()
                .graphicsLayer { scaleX = pressScale; scaleY = 1f / pressScale }
                .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(100.dp))
                .border(1.dp, com.example.whispry.ui.theme.WhispryTokens.GlassBorder, ContinuousRoundedRectangle(100.dp))
                .clickable(enabled = progress < 0.5f, interactionSource = interactionSource, indication = null) {
                    onSearchActiveChange(true)
                    focusRequester.requestFocus()
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = 0.5f + (0.5f * progress) }) {
                Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = localQuery,
                    onValueChange = { localQuery = it; onQueryChange(it) },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { if (it.isFocused) onSearchActiveChange(true) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        if (localQuery.isEmpty()) Text("Search languages...", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.35f))
                        inner()
                    }
                )
            }
        }
        if (progress > 0.01f) {
            Text("Cancel", modifier = Modifier.padding(start = 16.dp).alpha(progress).clickable {
                onSearchActiveChange(false)
                focusManager.clearFocus()
            }, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LanguageItem(name: String, code: String, isSelected: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = if (isSelected) androidx.compose.ui.graphics.Color.White else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                Text(code.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            }
            if (isSelected) Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color.White)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
    }
}
