package com.example.whispry.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.AccentPreset
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Floating control card shown while the widget is in edit mode: live sliders for the
 * widget's size (and arch in corner mode) plus Done. Rendered in its own overlay window.
 */
@Composable
fun WidgetEditControlCard(
    config: WidgetConfig,
    onBaseHeight: (Int) -> Unit,
    onProtrusion: (Int) -> Unit,
    onArch: (Int) -> Unit,
    onDone: () -> Unit
) {
    val accent = WhispryTheme.colors.accent
    Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF0141418))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Position & size",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Drag the pill anywhere — it snaps when you let go.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            CardSlider(
                label = if (config.shapeMode == WidgetShapeMode.RAMP) "Height" else "Size",
                value = config.baseHeightDp,
                range = 40f..88f,
                accent = accent,
                onChange = onBaseHeight
            )
            CardSlider(
                label = if (config.shapeMode == WidgetShapeMode.RAMP) "Width" else "Thickness",
                value = config.protrusionDp,
                range = 14f..36f,
                accent = accent,
                onChange = onProtrusion
            )
            if (config.shapeMode == WidgetShapeMode.CORNER) {
                CardSlider(
                    label = "Corner arch",
                    value = config.archDp,
                    range = 16f..64f,
                    accent = accent,
                    onChange = onArch
                )
            }

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            ) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
    }
}

@Composable
private fun CardSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Int) -> Unit
) {
    // Local echo so the thumb tracks the finger; keyed on the incoming value (not label, which
    // never changes) so the slider resyncs if config changes from anywhere but its own drag.
    var local by remember(value) { mutableStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = local,
            onValueChange = {
                local = it
                onChange(it.toInt())
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${local.toInt()}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
