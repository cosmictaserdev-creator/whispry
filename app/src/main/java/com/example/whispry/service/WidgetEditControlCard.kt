// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
 * widget's size plus Done. Rendered in its own overlay window.
 */
@Composable
fun WidgetEditControlCard(
    config: WidgetConfig,
    onBaseHeight: (Int) -> Unit,
    onProtrusion: (Int) -> Unit,
    onEdgeClearance: (Int) -> Unit,
    onDrag: (Float, Float) -> Unit,
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
            // Drag handle: this card starts pinned bottom-center, which can sit on top of the
            // widget itself if its saved spot is also near the bottom — drag the card out of the
            // way by its handle rather than the whole surface, so the sliders below stay usable.
            // The touch target is padded well beyond the thin visible bar, same idea as a
            // bottom-sheet grabber.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.x, amount.y)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
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
                label = "Height",
                value = config.baseHeightDp,
                range = 40f..88f,
                accent = accent,
                onChange = onBaseHeight
            )
            CardSlider(
                label = "Width",
                value = config.protrusionDp,
                range = 14f..36f,
                accent = accent,
                onChange = onProtrusion
            )

            Text(
                text = "Edge clearance",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "Wider keeps the swipe-out gesture working on phones whose system back swipe ignores the widget (some Realme/Poco).",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                EdgeClearanceChip("Flush", 0, config.edgeClearanceDp, accent, onEdgeClearance, Modifier.weight(1f))
                EdgeClearanceChip("Default", 12, config.edgeClearanceDp, accent, onEdgeClearance, Modifier.weight(1f))
                EdgeClearanceChip("Wide", 24, config.edgeClearanceDp, accent, onEdgeClearance, Modifier.weight(1f))
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
private fun EdgeClearanceChip(
    label: String,
    value: Int,
    selected: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selected == value
    Box(
        modifier = modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) accent else Color.White.copy(alpha = 0.08f))
            .clickable { onSelect(value) }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF141418) else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
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
