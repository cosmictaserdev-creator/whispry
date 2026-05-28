package com.example.whispry.util

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    hapticType: Int = HapticFeedbackConstants.KEYBOARD_TAP,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        onClick = {
            view.performHapticFeedback(hapticType)
            onClick()
        }
    )
}
