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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
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
    val railBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current

    val animatedSelectedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.75f),
        label = "RailSelectedOffset"
    )

    val indicatorHeight = dimensionResource(R.dimen.tablet_nav_rail_indicator_height)
    val indicatorHeightPx = with(density) { indicatorHeight.toPx() }

    BoxWithConstraints(
        modifier = modifier.width(dimensionResource(R.dimen.tablet_nav_rail_width)).fillMaxHeight(),
        contentAlignment = Alignment.TopStart
    ) {
        val railHeight = constraints.maxHeight.toFloat()
        val verticalPadding = with(density) { 6.dp.toPx() }
        val effectiveHeight = railHeight - (verticalPadding * 2)
        val itemHeight = if (items.isNotEmpty()) effectiveHeight / items.size else 0f

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(dimensionResource(R.dimen.tablet_nav_rail_content_width))
                .padding(vertical = 6.dp)
                .let { m ->
                    if (useGlass) {
                        m.drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(28.dp) },
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
                        m.shadow(8.dp, ContinuousRoundedRectangle(28.dp), spotColor = Color.Black)
                            .background(Color(0xFF1C1C1E), ContinuousRoundedRectangle(28.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), ContinuousRoundedRectangle(28.dp))
                    }
                }
        ) {
            // Selection indicator
            val indicatorCenterY = verticalPadding + (animatedSelectedOffset * itemHeight) + (itemHeight / 2f)
            Box(
                modifier = Modifier
                    .width(dimensionResource(R.dimen.tablet_nav_rail_indicator_width))
                    .height(indicatorHeight)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationY = indicatorCenterY - (indicatorHeightPx / 2f)
                        clip = true
                        shape = ContinuousRoundedRectangle(100.dp)
                    }
                    .let { m ->
                        if (useGlass) {
                            m.drawBackdrop(
                                backdrop = rememberCombinedBackdrop(backdrop, railBackdrop),
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

        // Invisible layer to capture backdrop
        Column(
            modifier = Modifier
                .alpha(0f)
                .layerBackdrop(railBackdrop)
                .fillMaxHeight()
                .width(80.dp)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { _, _ ->
                Box(modifier = Modifier.fillMaxWidth())
            }
        }

        // Actual visible items
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                LiquidRailTab(
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
private fun LiquidRailTab(
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(16.dp))
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
            label = "RailIconColor"
        )

        Icon(
            imageVector = if (selected) filledIcon else icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
