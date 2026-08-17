// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WhispryColorScheme = darkColorScheme(
    primary = DeepPurple,
    secondary = LightPurple,
    tertiary = SuccessTeal,
    background = PureBlack,
    surface = PureBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = PureBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun WhispryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WhispryColorScheme,
        typography = Typography,
        content = content
    )
}