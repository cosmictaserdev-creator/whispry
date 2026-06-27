package com.example.whispry.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whispry.ui.components.SheetTextField
import com.example.whispry.ui.components.WhispryBottomSheet
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * Language picker, built on the shared [WhispryBottomSheet] so it matches every other sheet:
 * translucent glass, rounded top, bouncy entrance, drag-to-dismiss, and the settings screen
 * behind it shrinks back (via [onDragProgress]) for the iOS-style depth effect.
 *
 * [backdrop] is retained for call-site compatibility; the unified sheet doesn't need it.
 */
@Composable
fun LanguagePickerBottomSheet(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onDragProgress: (Float) -> Unit,
    @Suppress("UNUSED_PARAMETER") backdrop: LayerBackdrop
) {
    val languages = remember {
        listOf(
            "Auto" to "Detect Language", "en" to "English", "es" to "Spanish", "fr" to "French",
            "de" to "German", "it" to "Italian", "pt" to "Portuguese", "nl" to "Dutch",
            "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ru" to "Russian",
            "tr" to "Turkish", "ar" to "Arabic", "hi" to "Hindi", "vi" to "Vietnamese",
            "pl" to "Polish", "uk" to "Ukrainian", "id" to "Indonesian", "th" to "Thai"
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        languages.filter {
            it.second.contains(searchQuery, ignoreCase = true) || it.first.contains(searchQuery, ignoreCase = true)
        }
    }

    WhispryBottomSheet(
        title = "Select Language",
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        scrollableContent = false,
        onDragProgress = onDragProgress
    ) {
        SheetTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search",
            placeholder = "Search languages…",
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredLanguages, key = { it.first }) { (code, name) ->
                LanguageItem(name, code, selectedLanguage == code) { onLanguageSelected(code) }
            }
        }
    }
}

@Composable
private fun LanguageItem(name: String, code: String, isSelected: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(code.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            }
            if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color.White)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
    }
}
