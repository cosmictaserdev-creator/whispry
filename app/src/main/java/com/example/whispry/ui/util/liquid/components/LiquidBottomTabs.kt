package com.example.whispry.ui.util.liquid.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.kyant.backdrop.Backdrop
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
    backdrop: Backdrop,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val horizontalPadding = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }
        val effectiveWidth = totalWidth - (horizontalPadding * 2)
        val tabWidth = effectiveWidth / tabsCount
        
        val indicatorWidthPx = tabWidth * 1.05f
        
        val targetOffset = horizontalPadding + (selectedTabIndex() * tabWidth) - (indicatorWidthPx - tabWidth) / 2
        val animatedOffset by animateFloatAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                stiffness = 500f, // Even snappier
                dampingRatio = 0.7f  // Slightly more bouncy
            ),
            label = "TabOffset"
        )

        val animatedWidthPx by animateFloatAsState(
            targetValue = indicatorWidthPx,
            animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
            label = "TabWidth"
        )

        // 1. Unified Bottom Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(38.dp) },
                    effects = {
                        vibrancy()
                        blur(3.dp.toPx()) // Increased blur for better readability
                        lens(40f, 40f , depthEffect = true , chromaticAberration = true)
                    },
                    shadow = { Shadow(alpha = 0.2f, radius = 20.dp) },
                    innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.1f) },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }
                )
        ) {
            // 2. Selection Indicator - Now inside the same box to reduce rendering layers
            Box(
                modifier = Modifier
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { animatedWidthPx.toDp() })
                    .height(50.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = animatedOffset
                        // Squish effect based on distance moved (simplified for performance)
                        val stretch = 1.05f
                        scaleX = stretch
                    }
                    .padding(horizontal = 4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = ContinuousRoundedRectangle(32.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = ContinuousRoundedRectangle(32.dp)
                    )
            ) {
                // Glow effect inside the indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                0.0f to accentColor.copy(alpha = 0.2f),
                                1.0f to Color.Transparent
                            )
                        )
                )
            }
        }

        // 3. Icons and Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
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
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (selected) 1.05f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.6f),
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
            targetValue = if (selected) accentColor else Color.White.copy(alpha = 0.4f),
            animationSpec = tween(300),
            label = "IconColor"
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
            color = iconColor, // Follows icon color (accent or faded white)
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp).graphicsLayer {
                val s = if (selected) 1.05f else 1f
                scaleX = s
                scaleY = s
            }
        )
    }
}
