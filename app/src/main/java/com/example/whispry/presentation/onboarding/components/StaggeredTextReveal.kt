// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Staggered word-by-word text reveal animation with blur-to-unblur effect.
 * Uses FlowRow to ensure text wraps naturally like a paragraph.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaggeredTextReveal(
    text: String,
    style: TextStyle,
    delayMs: Int = 0,
    staggerPerWordMs: Int = 80,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val words = remember(text) { text.split("\\s+".toRegex()).filter { it.isNotBlank() } }
    
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        words.forEachIndexed { index, word ->
            WordReveal(
                word = word,
                style = style,
                delayMs = delayMs + (index * staggerPerWordMs)
            )
            // Add a small spacer for word separation that doesn't break wrapping
            if (index < words.size - 1) {
                Spacer(modifier = Modifier.width(style.fontSize.value.dp * 0.25f))
            }
        }
    }
}

/**
 * Multiline version of the staggered reveal, optimized for longer content like transcriptions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiLineStaggeredText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    staggerPerWordMs: Int = 60
) {
    StaggeredTextReveal(
        text = text,
        style = style,
        modifier = modifier,
        delayMs = delayMs,
        staggerPerWordMs = staggerPerWordMs,
        horizontalArrangement = Arrangement.Center
    )
}

@Composable
private fun WordReveal(
    word: String,
    style: TextStyle,
    delayMs: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(word) {
        delay(delayMs.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "WordAlpha"
    )

    val blurRadius by animateDpAsState(
        targetValue = if (visible) 0.dp else 12.dp,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "WordBlur"
    )

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "WordOffset"
    )

    Text(
        text = word,
        style = style,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY.toPx()
            }
            .blur(blurRadius),
        softWrap = false // We handle wrapping via FlowRow
    )
}
