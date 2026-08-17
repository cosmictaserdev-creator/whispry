// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Shared building blocks for every secondary/detail screen, so they all share one design language
 * (the "Library" look): translucent glass cards with a single border + soft shadow, an iOS-style
 * press-scale spring, a consistent header/subtitle scaffold, and matching empty states and pills.
 *
 * Detail screens should compose [WhispryDetailScaffold] + [WhispryCard] instead of hand-rolling
 * backgrounds, borders, and spacing — that is what keeps them visually identical.
 */

/** The canonical card corner radius across cards (matches the Library transcript cards). */
val WhispryCardCorner: Dp = 20.dp

/**
 * Bundles the two scopes a shared-element ("hero") transition needs: the [SharedTransitionScope]
 * from the `SharedTransitionLayout` wrapping the NavHost, and the [AnimatedVisibilityScope] of the
 * current destination (the `composable { }` lambda's `AnimatedContentScope`). Pass one of these
 * from the NavHost into a screen to opt that screen into hub→detail hero morphs; pass null to keep
 * the screen rendering exactly as before.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class WhispryHero(
    val sharedScope: SharedTransitionScope,
    val animatedScope: AnimatedVisibilityScope
)

/** Stable shared-element keys pairing a productivity-hub row with its detail screen header. */
object WhispryHeroKeys {
    const val TextExpander = "hub:text-expander"
    const val AppTones = "hub:app-tones"
    const val HiddenApps = "hub:hidden-apps"
    const val Memory = "hub:memory"
    const val MyInfo = "hub:my-info"
    const val VoiceCommands = "hub:voice-commands"
    const val Updates = "hub:updates"
}

/**
 * Marks this element as one endpoint of a shared-bounds ("container transform") transition under
 * [hero]. No-op when [hero] or [key] is null, so callers can wire it unconditionally and it simply
 * does nothing until both endpoints (source row + target header) are present.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.heroSharedBounds(hero: WhispryHero?, key: Any?): Modifier {
    if (hero == null || key == null) return this
    return with(hero.sharedScope) {
        this@heroSharedBounds.sharedBounds(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = hero.animatedScope
        )
    }
}

/**
 * The "liquid button" touch feel for a tappable element that does NOT have its own elevated surface
 * (a plain row/chip). Because it's a single trailing modifier it can't wrap an upstream
 * background, so the press transform scales the content region; the glow is clipped to [shape].
 * For elevated cards prefer [WhispryCard] (or the [liquidExpand]/[liquidGlow] split) so the whole
 * surface transforms. Pairs a null-indication clickable with the glow instead of a ripple.
 */
@Composable
fun Modifier.pressClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = ContinuousRoundedRectangle(WhispryCardCorner)
): Modifier {
    val touch = rememberLiquidTouch()
    return this
        .liquidExpand(touch, enabled = enabled)
        .liquidGlow(touch, shape, enabled = enabled)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * The single elevated glass card used across detail screens: soft shadow + [WhispryTokens.SurfaceElevated]
 * fill + one [WhispryTokens.GlassBorder] outline on a continuous-rounded rectangle. When [onClick] is
 * supplied the whole card gets the liquid-button touch feel (glow blooms from the finger + the
 * surface scales up and stretches toward it).
 */
@Composable
fun WhispryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = WhispryCardCorner,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = ContinuousRoundedRectangle(cornerRadius)
    val touch = rememberLiquidTouch()
    val interactive = onClick != null
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidExpand(touch, enabled = interactive)
            .shadow(4.dp, shape, spotColor = Color.Black)
            .background(WhispryTokens.SurfaceElevated, shape)
            .border(1.dp, WhispryTokens.GlassBorder, shape)
            .liquidGlow(touch, shape, enabled = interactive)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Standard detail-screen scaffold: a [ScreenHeader] (back button + title + optional [headerActions]),
 * an optional [subtitle], then the [content] slot — all over the global glass backdrop (transparent
 * background). The whole page springs/fades up into place on first show for an iOS feel, and scales
 * back by [pushBackProgress] (0f..1f) when a bottom sheet is open.
 */
@Composable
fun WhispryDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    pushBackProgress: Float = 0f,
    horizontalPadding: Dp = 24.dp,
    hero: WhispryHero? = null,
    heroKey: Any? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // One-shot entrance: content rises a touch and fades in with a calm, no-overshoot spring.
    // When a hero transition is driving this screen, the shared element + nav transition already
    // animate it in, so we start fully settled to avoid a double animation.
    val heroActive = hero != null && heroKey != null
    val entrance = remember { Animatable(if (heroActive) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!heroActive) entrance.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pushScale = 1f - 0.08f * pushBackProgress
                    val enterScale = 0.98f + 0.02f * entrance.value
                    scaleX = pushScale * enterScale
                    scaleY = pushScale * enterScale
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 24f
                }
                .padding(horizontal = horizontalPadding)
        ) {
            Box(modifier = Modifier.heroSharedBounds(hero, heroKey)) {
                ScreenHeader(title = title, onBack = onBack, actions = headerActions)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    color = WhispryTokens.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

/** Consistent centered empty state: a bold [title] line and a quieter [hint]. */
@Composable
fun ColumnScope.WhispryEmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = WhispryTokens.TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                color = WhispryTokens.TextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

/** Small accent pill/badge used for shortcut words, keys, and tags inside cards. */
@Composable
fun WhispryPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
