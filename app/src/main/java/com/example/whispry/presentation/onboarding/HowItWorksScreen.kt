// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

@Composable
fun HowItWorksScreen(
    onContinue: () -> Unit,
    backdrop: Backdrop
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Tighten the vertical gutters on short screens so the three step cards + CTA fit
        // without scrolling.
        val compact = maxHeight < 640.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(if (compact) 36.dp else 72.dp))

            StaggeredTextReveal(
                text = "How it works",
                style = TextStyle(
                    color = WhispryTokens.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            StaggeredTextReveal(
                text = "Talk to type in any app — right where you're already typing.",
                style = TextStyle(
                    color = WhispryTokens.TextSecondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 26.sp
                ),
                delayMs = 200
            )

            Spacer(modifier = Modifier.height(if (compact) 20.dp else 40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepCard(
                    number = 1,
                    icon = Icons.Rounded.TouchApp,
                    title = "Tap",
                    description = "Open the app you want to type in. Whispry's mic button appears above your keyboard.",
                    delayMs = 350,
                    backdrop = backdrop
                )
                StepCard(
                    number = 2,
                    icon = Icons.Rounded.GraphicEq,
                    title = "Speak",
                    description = "Tap the mic and say what you want to write, naturally.",
                    delayMs = 450,
                    backdrop = backdrop
                )
                StepCard(
                    number = 3,
                    icon = Icons.Rounded.ContentPaste,
                    title = "Done",
                    description = "Tap it again — your words are transcribed and placed right in the field.",
                    delayMs = 550,
                    backdrop = backdrop
                )
            }
        }

        Column {
            LiquidButton(
                onClick = onContinue,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    "Got it",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 24.dp else 48.dp))
        }
    }
    }
}

@Composable
private fun StepCard(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String,
    delayMs: Int,
    backdrop: Backdrop
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }

    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = spring(stiffness = 250f, dampingRatio = 0.85f),
        label = "StepOffset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "StepAlpha"
    )

    GlassCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset
                this.alpha = alpha
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = WhispryTokens.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = WhispryTokens.TextTertiary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = number.toString(),
                color = WhispryTokens.TextTertiary.copy(alpha = 0.5f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
