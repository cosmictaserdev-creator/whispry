// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import com.example.whispry.ui.theme.WhispryTokens

@Composable
fun GlassBox(
    backdrop: Backdrop, // Kept for compatibility but unused
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = WhispryTokens.SurfaceElevated, // translucent elevated panel
    blurRadius: Dp = 20.dp, 
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, ContinuousRoundedRectangle(cornerRadius), spotColor = Color.Black)
            .background(containerColor, ContinuousRoundedRectangle(cornerRadius))
            .border(1.dp, WhispryTokens.GlassBorder, ContinuousRoundedRectangle(cornerRadius))
            .clip(ContinuousRoundedRectangle(cornerRadius)),
        content = content
    )
}

/**
 * A standard glass card with default padding and Liquid Glass effect.
 * Now includes a built-in micro-animation for touch feedback.
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = WhispryTokens.SurfaceElevated, // translucent elevated panel
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "CardScale"
    )

    GlassBox(
        backdrop = backdrop,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        cornerRadius = cornerRadius,
        containerColor = containerColor,
        content = {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    )
}
