package com.example.whispry.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.R
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTokens
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

enum class IntroPhase {
    VOID,           
    AWAKENING,      
    LOGO_FORMING,   
    NAME_REVEAL,    
    BREATHING,      
    TRANSITIONING   
}

@Composable
fun IntroScreen(
    onTransition: () -> Unit,
    backdrop: Backdrop
) {
    var phase by remember { mutableStateOf(IntroPhase.VOID) }
    
    LaunchedEffect(Unit) {
        delay(400)
        phase = IntroPhase.AWAKENING
        delay(800)
        phase = IntroPhase.LOGO_FORMING
        delay(800)
        phase = IntroPhase.NAME_REVEAL
        delay(700)
        phase = IntroPhase.BREATHING
    }

    val logoScale by animateFloatAsState(
        targetValue = if (phase >= IntroPhase.LOGO_FORMING) 1f else 0.88f,
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.72f),
        label = "LogoScale"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= IntroPhase.LOGO_FORMING) 1f else 0f,
        animationSpec = tween(800),
        label = "LogoAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "IntroBreathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = phase >= IntroPhase.NAME_REVEAL,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTransition()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        val s = logoScale * if (phase == IntroPhase.BREATHING) breathScale else 1f
                        scaleX = s
                        scaleY = s
                        alpha = logoAlpha
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.whisperlogo),
                    contentDescription = "Whispry Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            if (phase >= IntroPhase.NAME_REVEAL) {
                StaggeredTextReveal(
                    text = "Whispry",
                    style = TextStyle(
                        color = WhispryTokens.TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                StaggeredTextReveal(
                    text = "Your voice, instantly understood.",
                    style = TextStyle(
                        color = WhispryTokens.TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.02.sp,
                        lineHeight = 26.sp
                    ),
                    delayMs = 500
                )
            }
        }
        
        AnimatedVisibility(
            visible = phase >= IntroPhase.BREATHING,
            enter = fadeIn(tween(1500)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)
        ) {
            Text(
                text = "Tap to begin",
                color = WhispryTokens.TextTertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.05.sp
            )
        }
    }
}
