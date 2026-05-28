package com.example.whispry.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.presentation.onboarding.components.StaggeredTextReveal
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop

@Composable
fun ApiKeyScreen(
    state: OnboardingState,
    onApiKeyChange: (String) -> Unit,
    onValidate: () -> Unit,
    onGetApiKey: () -> Unit,
    onComplete: () -> Unit,
    backdrop: Backdrop,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(72.dp))

            StaggeredTextReveal(
                text = "Connect Groq",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = WhispryTokens.TextPrimary,
                    letterSpacing = (-1).sp,
                    fontSize = 36.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredTextReveal(
                text = "Whispry uses Groq's high-speed inference for instant transcription.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WhispryTokens.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    fontSize = 17.sp
                ),
                delayMs = 300
            )

            Spacer(modifier = Modifier.height(56.dp))

            // API Key Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = if (state.isApiKeyValid) WhispryTokens.SuccessGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Key, null, tint = WhispryTheme.colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    BasicTextField(
                        value = state.apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = SolidColor(WhispryTheme.colors.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        decorationBox = { inner ->
                            if (state.apiKey.isEmpty()) Text("gsk_...", color = Color.White.copy(0.25f), fontSize = 17.sp)
                            inner()
                        }
                    )
                }
            }

            if (state.keyValidationError != null) {
                Text(
                    text = state.keyValidationError,
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGetApiKey() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have a key? Get one free",
                    style = MaterialTheme.typography.labelMedium,
                    color = WhispryTheme.colors.accent,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = WhispryTheme.colors.accent, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))

            GlassCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.PrivacyTip, null, tint = WhispryTheme.colors.accent.copy(0.7f), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Privacy Commitment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WhispryTokens.TextPrimary)
                        Text(
                            "We never collect or store your audio or transcriptions. Everything is processed directly through Groq's API.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhispryTokens.TextTertiary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Column {
            LiquidButton(
                onClick = { if (state.isApiKeyValid) onComplete() else onValidate() },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !state.isValidatingKey && state.apiKey.isNotBlank()
            ) {
                if (state.isValidatingKey) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WhispryTheme.colors.accent, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (state.isApiKeyValid) "Continue" else "Test & Save Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WhispryTheme.colors.accent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
