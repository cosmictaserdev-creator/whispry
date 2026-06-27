package com.example.whispry.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The single bottom-sheet style used across the app: a translucent glass panel with a rounded
 * top that slides up over a dimmed scrim, springs in with a gentle bounce, and can be flicked
 * or dragged down to dismiss (drag the handle/header — the content scrolls independently).
 *
 * [onDragProgress] reports 0f (off-screen) → 1f (fully open) so the host can shrink its own
 * content behind the sheet for the iOS-style "card pushed back" effect.
 *
 * Spacious by default ([heightFraction] = 0.9) so forms never feel cramped.
 */
@Composable
fun WhispryBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.9f,
    scrollableContent: Boolean = true,
    onDragProgress: (Float) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val focusManager = LocalFocusManager.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val offsetY = remember { Animatable(screenHeightPx) }

    LaunchedEffect(offsetY.value) {
        onDragProgress((1f - (offsetY.value / screenHeightPx)).coerceIn(0f, 1f))
    }
    // Bouncy entrance.
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow))
    }

    val dismiss: () -> Unit = {
        scope.launch {
            focusManager.clearFocus()
            offsetY.animateTo(screenHeightPx, spring(stiffness = Spring.StiffnessMedium))
            onDismiss()
        }
        Unit
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch { offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f)) }
    }

    BackHandler { dismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim fades with the sheet position.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - (offsetY.value / screenHeightPx)).coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.45f))
        )

        val sheetShape = ContinuousRoundedRectangle(topStart = 32.dp, topEnd = 32.dp)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .background(WhispryTokens.SurfaceElevated, sheetShape)
                .border(1.dp, WhispryTokens.GlassBorder, sheetShape)
                // Consume taps so they don't fall through to the scrim's dismiss handler.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Draggable header: handle + title. Dragging here dismisses; content scrolls on its own.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (offsetY.value > screenHeightPx * 0.18f || velocity > 1200f) dismiss()
                            else scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.7f)) }
                        }
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(40.dp, 4.dp).background(Color.White.copy(alpha = 0.22f), CircleShape))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = dismiss) {
                        Icon(Icons.Rounded.Close, "Close", tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            if (scrollableContent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .imePadding(),
                    content = content
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .imePadding(),
                    content = content
                )
            }
        }
    }
}

/** Rounded, glass-style text field used inside [WhispryBottomSheet] forms. */
@Composable
fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        shape = ContinuousRoundedRectangle(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.85f),
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedBorderColor = Color.White.copy(alpha = 0.4f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
            cursorColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Full-width primary action button used at the bottom of sheet forms. */
@Composable
fun SheetPrimaryButton(
    text: String = "Save",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ContinuousRoundedRectangle(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.3f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
