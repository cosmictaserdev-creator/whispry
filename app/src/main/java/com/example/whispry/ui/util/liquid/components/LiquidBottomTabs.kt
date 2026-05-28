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
                .height(58.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(38.dp) },
                    effects = {
                        vibrancy()
                        blur(3.dp.toPx())
                        lens(40f, 40f , depthEffect = true , chromaticAberration = true)
                    },
                    shadow = { Shadow(alpha = 0.2f, radius = 20.dp) },
                    innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.1f) },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }
                )
        ) {
            // 2. Selection Indicator - Using graphicsLayer for GPU acceleration
            Box(
                modifier = Modifier
                    .width(with(androidx.compose.ui.platform.LocalDensity.current) { baseIndicatorWidthPx.toDp() + 10.dp })
                    .height(50.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        // Position based on center minus half width
                        translationX = animatedCenterOffset - (baseIndicatorWidthPx / 2)
                        
                        // Snappy liquid stretch effect: scaleX slightly larger than 1
                        scaleX = 1.08f 
                        clip = true
                        shape = ContinuousRoundedRectangle(32.dp)
                    }
                    .padding(horizontal = 6.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        shape = ContinuousRoundedRectangle(32.dp)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.55f), Color.Transparent)
                        ),
                        shape = ContinuousRoundedRectangle(32.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                0.0f to accentColor.copy(alpha = 0.25f),
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
            targetValue = if (selected) accentColor else Color.White.copy(alpha = 0.35f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "IconColor"
        )

        Icon(
            imageVector = if (selected) filledIcon else icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
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
