// SPDX-License-Identifier: AGPL-3.0-or-later
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
    // Full language set supported by whisper-large-v3 (Groq/OpenAI), alphabetized by name.
    // "hi" doubles as the Hinglish (Hindi + English code-switching) option — Whisper has no
    // separate Hinglish code, but hinting "hi" already handles mixed Hindi/English speech well.
    val languages = remember {
        listOf(
            "Auto" to "Detect Language",
            "af" to "Afrikaans", "sq" to "Albanian", "am" to "Amharic", "ar" to "Arabic",
            "hy" to "Armenian", "as" to "Assamese", "az" to "Azerbaijani", "ba" to "Bashkir",
            "eu" to "Basque", "be" to "Belarusian", "bn" to "Bengali", "bs" to "Bosnian",
            "br" to "Breton", "bg" to "Bulgarian", "yue" to "Cantonese", "ca" to "Catalan",
            "zh" to "Chinese", "hr" to "Croatian", "cs" to "Czech", "da" to "Danish",
            "nl" to "Dutch", "en" to "English", "et" to "Estonian", "fo" to "Faroese",
            "fi" to "Finnish", "fr" to "French", "gl" to "Galician", "ka" to "Georgian",
            "de" to "German", "el" to "Greek", "gu" to "Gujarati", "ht" to "Haitian Creole",
            "ha" to "Hausa", "haw" to "Hawaiian", "he" to "Hebrew", "hi" to "Hindi (Hinglish)",
            "hu" to "Hungarian", "is" to "Icelandic", "id" to "Indonesian", "it" to "Italian",
            "ja" to "Japanese", "jw" to "Javanese", "kn" to "Kannada", "kk" to "Kazakh",
            "km" to "Khmer", "ko" to "Korean", "lo" to "Lao", "la" to "Latin",
            "lv" to "Latvian", "ln" to "Lingala", "lt" to "Lithuanian", "lb" to "Luxembourgish",
            "mk" to "Macedonian", "mg" to "Malagasy", "ms" to "Malay", "ml" to "Malayalam",
            "mt" to "Maltese", "mi" to "Maori", "mr" to "Marathi", "mn" to "Mongolian",
            "my" to "Myanmar", "ne" to "Nepali", "no" to "Norwegian", "nn" to "Nynorsk",
            "oc" to "Occitan", "ps" to "Pashto", "fa" to "Persian", "pl" to "Polish",
            "pt" to "Portuguese", "pa" to "Punjabi", "ro" to "Romanian", "ru" to "Russian",
            "sa" to "Sanskrit", "sr" to "Serbian", "sn" to "Shona", "sd" to "Sindhi",
            "si" to "Sinhala", "sk" to "Slovak", "sl" to "Slovenian", "so" to "Somali",
            "es" to "Spanish", "su" to "Sundanese", "sw" to "Swahili", "sv" to "Swedish",
            "tl" to "Tagalog", "tg" to "Tajik", "ta" to "Tamil", "tt" to "Tatar",
            "te" to "Telugu", "th" to "Thai", "bo" to "Tibetan", "tr" to "Turkish",
            "tk" to "Turkmen", "uk" to "Ukrainian", "ur" to "Urdu", "uz" to "Uzbek",
            "vi" to "Vietnamese", "cy" to "Welsh", "yi" to "Yiddish", "yo" to "Yoruba"
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
