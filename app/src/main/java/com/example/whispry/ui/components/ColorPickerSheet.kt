// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.whispry.ui.theme.WhispryTokens
import kotlin.math.roundToInt

/**
 * Floating full-color HSV picker for the accent-color "Custom" swatch. Rendered inside a
 * [WhispryBottomSheet] so it inherits the same glass styling, drag-to-dismiss, and
 * orientation/screen-size handling as every other sheet in the app.
 */
@Composable
fun ColorPickerSheet(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        val out = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (initialColor.red * 255).roundToInt(),
            (initialColor.green * 255).roundToInt(),
            (initialColor.blue * 255).roundToInt(),
            out
        )
        out
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor by remember {
        derivedStateOf { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))) }
    }

    WhispryBottomSheet(title = "Custom Color", onDismiss = onDismiss, heightFraction = 0.75f) {
        SaturationValueBox(hue = hue, sat = sat, value = value, onChange = { s, v -> sat = s; value = v })

        Spacer(Modifier.height(20.dp))

        HueSlider(hue = hue, onChange = { hue = it })

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, WhispryTokens.GlassBorder, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "#" + (currentColor.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        SheetPrimaryButton(text = "Use This Color") { onConfirm(currentColor) }
    }
}

/**
 * Drag/tap tracking shared by the SV box and hue slider — press position included, not just
 * moves — reporting fractional (0f..1f) coordinates within the element so callers never need
 * the element's pixel size (which isn't available outside the pointerInput scope).
 */
private fun Modifier.pointerFractionDrag(key: Any?, onFraction: (Offset) -> Unit): Modifier =
    pointerInput(key) {
        fun report(pos: Offset) {
            onFraction(
                Offset(
                    (pos.x / size.width).coerceIn(0f, 1f),
                    (pos.y / size.height).coerceIn(0f, 1f)
                )
            )
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            report(down.position)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                change.consume()
                report(change.position)
            }
        }
    }

@Composable
private fun SaturationValueBox(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit
) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerFractionDrag(hue) { f -> onChange(f.x, 1f - f.y) }
    ) {
        val cursorX = maxWidth * sat
        val cursorY = maxHeight * (1f - value)
        Box(
            modifier = Modifier
                .offset(x = cursorX - 9.dp, y = cursorY - 9.dp)
                .size(18.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val hueColors = remember {
        (0..360 step 60).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(hueColors))
            .pointerFractionDrag(Unit) { f -> onChange(f.x * 360f) }
    ) {
        val cursorX = maxWidth * (hue / 360f)
        Box(
            modifier = Modifier
                .offset(x = cursorX - 3.dp)
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
        )
    }
}
