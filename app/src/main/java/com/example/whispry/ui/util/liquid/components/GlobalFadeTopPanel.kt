// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.util.liquid.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun GlobalFadeTopPanel(
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    Box(
        modifier = modifier
            .offset(y = (-16).dp) // Bleed up
            .requiredWidth(LocalConfiguration.current.screenWidthDp.dp + 100.dp) // Bleed wide
            .height(height)
    ) {
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
                .background(
                    color = WhispryTokens.DeepVoid,
                    shape = ContinuousRoundedRectangle(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
        )
    }
}
