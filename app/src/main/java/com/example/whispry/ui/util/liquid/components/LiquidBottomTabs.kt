// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.util.liquid.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    tabsCount: Int,
    backdrop: LayerBackdrop,
    accentColor: Color,
    modifier: Modifier = Modifier,
    useGlass: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val horizontalPadding = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }
        val effectiveWidth = totalWidth - (horizontalPadding * 2)
        val tabWidth = effectiveWidth / tabsCount

        // Fixed base width for indicator to avoid layout changes during animation
        val baseIndicatorWidthPx = tabWidth

        // Use a very snappy spring for instant movement
        val animatedCenterOffset by animateFloatAsState(
            targetValue = horizontalPadding + (selectedTabIndex() * tabWidth) + (tabWidth / 2),
            animationSpec = spring(
                stiffness = 800f, // Instant and snappy
                dampingRatio = 0.75f
            ),
            label = "TabCenterOffset"
        )

        // 1. Unified Bottom Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .let { m ->
                    if (useGlass) {
                        m.drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(50.dp) },
                            highlight = {
                                Highlight(
                                    width = 1.dp,
                                    blurRadius = 1.dp,
                                    alpha = .2f,
                                    style = HighlightStyle.Plain
                                )},
                            effects = {
                                vibrancy()
                                blur(6f)
                                lens(24.dp.toPx(),
                                    24.dp.toPx(),
                                    true
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            },
                            shadow = { Shadow(alpha = 0.1f, radius = 16.dp) },
                        )
                    } else {
                        m.shadow(8.dp, ContinuousRoundedRectangle(34.dp), spotColor = Color.Black)
                         .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(34.dp))
                         .border(0.5.dp, Color.White.copy(alpha = 0.1f), ContinuousRoundedRectangle(34.dp))
                    }
                }
        ) {
            // 2. Selection Indicator - Using graphicsLayer for GPU acceleration
            Box(
                modifier = Modifier
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { baseIndicatorWidthPx.toDp() })
                    .height(64.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        // Position based on center minus half width
                        translationX = animatedCenterOffset - (baseIndicatorWidthPx / 2)

                        // Snappy liquid stretch effect
                        scaleX = 1f
                        scaleY = 1f
                        clip = true
                        shape = ContinuousRoundedRectangle(100.dp)
                    }
                    .let { m ->
                        if (useGlass) {
                            m.drawBackdrop(
                                backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                                shape = { ContinuousRoundedRectangle(100.dp) },
                                effects = {
                                    blur(2f)
                                    lens(
                                        10.dp.toPx(),
                                        14.dp.toPx(),
                                        chromaticAberration = false
                                    )
                                },
                                highlight = {
                                    Highlight.Default.copy(alpha = 0.1f)
                                },
                                shadow = {
                                    Shadow(alpha = 0.1f)
                                },
                                innerShadow = {
                                    InnerShadow(
                                        radius = 8.dp,
                                        alpha = 0.1f
                                    )
                                },
                                onDrawSurface = {
                                    drawRect(
                                       accentColor, blendMode = BlendMode.Hue
                                    )
                                    drawRect(accentColor.copy(alpha = 0.1f))
                                }
                            )
                        } else {
                            m.background(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = ContinuousRoundedRectangle(100.dp)
                            ).border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.3f),
                                shape = ContinuousRoundedRectangle(100.dp)
                            )
                        }
                    }
            )
        }

        // 3. Invisible Icons Row to capture backdrop
        Row(
            modifier = Modifier
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // 4. Actual Visible Icons and Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

    }

}

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    icon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    selected: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Snappy scale animation in graphicsLayer
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (selected) 1.02f else 1f,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.7f),
        label = "TabScale"
    )
    
    val accentColor = WhispryTheme.colors.accent

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(ContinuousRoundedRectangle(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconColor by animateColorAsState(
            targetValue = if (selected) accentColor else Color.White.copy(alpha = 0.45f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "IconColor"
        )

        Icon(
            imageVector = if (selected) filledIcon else icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(26.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
