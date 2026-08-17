// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.util.adaptive

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
import androidx.compose.ui.platform.LocalDensity
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

data class RailNavigationItem(
    val label: String,
    val icon: ImageVector,
    val filledIcon: ImageVector = icon
)

@Composable
fun LiquidNavigationRail(
    selectedIndex: Int,
    items: List<RailNavigationItem>,
    backdrop: LayerBackdrop,
    accentColor: Color,
    modifier: Modifier = Modifier,
    useGlass: Boolean = true,
    onItemClick: (Int) -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current

    val animatedSelectedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.75f),
        label = "RailSelectedOffset"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.TopStart
    ) {
        val railWidth = constraints.maxWidth.toFloat()
        val railHeight = constraints.maxHeight.toFloat()
        val tabHeight = if (items.isNotEmpty()) railHeight / items.size else 0f

        // Compact fixed-height pill (matches the bottom bar), not the full tab height —
        // otherwise the indicator stretches into a tall accent block that swallows the icon.
        val baseIndicatorHeightPx = with(density) { 64.dp.toPx() }

        val animatedCenterOffset by animateFloatAsState(
            targetValue = (selectedIndex.toFloat() * tabHeight) + (tabHeight / 2f),
            animationSpec = spring(stiffness = 800f, dampingRatio = 0.75f),
            label = "RailIndicatorCenter"
        )

        // 1. Unified Rail Container
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { railWidth.toDp() })
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
                                )
                            },
                            effects = {
                                vibrancy()
                                blur(6f)
                                lens(24.dp.toPx(), 24.dp.toPx(), true)
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            },
                            shadow = { Shadow(alpha = 0.1f, radius = 16.dp) }
                        )
                    } else {
                        m.shadow(8.dp, ContinuousRoundedRectangle(34.dp), spotColor = Color.Black)
                            .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(34.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), ContinuousRoundedRectangle(34.dp))
                    }
                }
        ) {
            // 2. Selection Indicator
            Box(
                modifier = Modifier
                    .height(with(density) { baseIndicatorHeightPx.toDp() })
                    .width(64.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = animatedCenterOffset - (baseIndicatorHeightPx / 2f)
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
                                    lens(10.dp.toPx(), 14.dp.toPx(), chromaticAberration = false)
                                },
                                highlight = { Highlight.Default.copy(alpha = 0.1f) },
                                shadow = { Shadow(alpha = 0.1f) },
                                innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.1f) },
                                onDrawSurface = {
                                    drawRect(accentColor, blendMode = BlendMode.Hue)
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

        // 3. Invisible items layer to capture backdrop (tinted for glass effect)
        Column(
            modifier = Modifier
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .fillMaxHeight()
                .width(with(density) { railWidth.toDp() })
                .padding(horizontal = 4.dp)
                .graphicsLayer { alpha = 0f; colorFilter = ColorFilter.tint(accentColor) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                LiquidRailTab(
                    modifier = Modifier.weight(1f),
                    selected = selectedIndex == index,
                    icon = item.icon,
                    filledIcon = item.filledIcon,
                    label = item.label,
                    onClick = { }
                )
            }
        }

        // 4. Actual visible items
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { railWidth.toDp() })
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                LiquidRailTab(
                    modifier = Modifier.weight(1f),
                    selected = selectedIndex == index,
                    icon = item.icon,
                    filledIcon = item.filledIcon,
                    label = item.label,
                    onClick = { onItemClick(index) }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LiquidRailTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    filledIcon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (selected) 1.02f else 1f,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.7f),
        label = "RailTabScale"
    )

    val accentColor = WhispryTheme.colors.accent

    Column(
        modifier = modifier
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
        // Selected glyph is white for high contrast against the accent pill — accent-on-accent
        // makes the icon vanish into the indicator in the narrow rail.
        val iconColor by animateColorAsState(
            targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "RailIconColor"
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
