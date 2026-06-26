package com.example.whispry.presentation.presets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.TopFadeScrim
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.platform.LocalConfiguration

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    val selectedPreset: StateFlow<OutputPreset> = settingsProvider.dataStore.data
        .map { prefs ->
            val name = prefs[DataStoreKeys.DEFAULT_OUTPUT_PRESET] ?: OutputPreset.NONE.name
            try { OutputPreset.valueOf(name) } catch (e: Exception) { OutputPreset.NONE }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OutputPreset.NONE)

    val customInstructions: StateFlow<String> = settingsProvider.dataStore.data
        .map { prefs -> prefs[DataStoreKeys.CUSTOM_AI_INSTRUCTIONS] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val translateTargetLanguage: StateFlow<String> = settingsProvider.translateTargetLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.whispry.domain.model.TranslationLanguages.DEFAULT)

    fun selectPreset(preset: OutputPreset) {
        viewModelScope.launch {
            settingsProvider.dataStore.edit { it[DataStoreKeys.DEFAULT_OUTPUT_PRESET] = preset.name }
        }
    }

    fun saveCustomInstructions(instructions: String) {
        viewModelScope.launch {
            settingsProvider.dataStore.edit { it[DataStoreKeys.CUSTOM_AI_INSTRUCTIONS] = instructions }
        }
    }

    fun setTranslateTargetLanguage(language: String) {
        viewModelScope.launch {
            settingsProvider.setTranslateTargetLanguage(language)
        }
    }
}

@Composable
fun PresetsScreen(
    backdrop: LayerBackdrop,
    viewModel: PresetsViewModel = hiltViewModel()
) {
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val customInstructions by viewModel.customInstructions.collectAsStateWithLifecycle()
    val translateLanguage by viewModel.translateTargetLanguage.collectAsStateWithLifecycle()
    var showLanguageSheet by remember { mutableStateOf(false) }
    val themeAccent = androidx.compose.ui.graphics.Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 190.dp,
                bottom = 140.dp,
                start = 24.dp,
                end = 24.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(OutputPreset.entries) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = selectedPreset == preset,
                    accentColor = themeAccent,
                    onClick = { viewModel.selectPreset(preset) }
                )
            }

            if (selectedPreset == OutputPreset.CUSTOM) {
                item(span = { GridItemSpan(2) }) {
                    CustomInstructionsEditor(
                        instructions = customInstructions,
                        onSave = { viewModel.saveCustomInstructions(it) },
                        accentColor = themeAccent
                    )
                }
            }

            if (selectedPreset == OutputPreset.TRANSLATE_AUTO) {
                item(span = { GridItemSpan(2) }) {
                    TranslateLanguageSelector(
                        selectedLanguage = translateLanguage,
                        onClick = { showLanguageSheet = true },
                        accentColor = themeAccent
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Smart presets send your transcript to Groq for formatting. This uses a small amount of your API quota.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                )
            }
        }

        // Top Panel Container (bleeds 50dp past each edge of the available area). Width is
        // based on the *local* available width so it stays aligned and doesn't clip when the
        // content is inset by the rail in landscape.
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
        ) {
          Box(
            modifier = Modifier
                .width(maxWidth + 100.dp)
                .align(Alignment.TopCenter)
          ) {
            // Darkening top bar (fade), shared across screens.
            TopFadeScrim(
                modifier = Modifier.matchParentSize()
            )

            // Content Panel
            Column(
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp)
                    .padding(horizontal = 64.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Formatting Presets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AIBadge()
                }
                
                Text(
                    text = "Choose how your transcripts are formatted automatically using AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
          }
        }

        if (showLanguageSheet) {
            TranslateLanguageSheet(
                selectedLanguage = translateLanguage,
                onSelect = {
                    viewModel.setTranslateTargetLanguage(it)
                    showLanguageSheet = false
                },
                onDismiss = { showLanguageSheet = false }
            )
        }
    }
}

@Composable
fun TranslateLanguageSelector(
    selectedLanguage: String,
    onClick: () -> Unit,
    accentColor: Color
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Translate to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Whatever you say is translated into this language.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = selectedLanguage,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateLanguageSheet(
    selectedLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D0D14),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Translate to",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Output language for the Translate preset.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(com.example.whispry.domain.model.TranslationLanguages.all) { lang ->
                    val isSelected = lang == selectedLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable { onSelect(lang) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            lang,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PresetCard(
    preset: OutputPreset,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                )
            } else if (preset != OutputPreset.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(androidx.compose.ui.graphics.Color.White, CircleShape)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = preset.emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AIBadge() {
    Surface(
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f))
    ) {
        Text(
            text = "AI",
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CustomInstructionsEditor(
    instructions: String,
    onSave: (String) -> Unit,
    accentColor: Color
) {
    var text by remember(instructions) { mutableStateOf(instructions) }
    val isModified = text != instructions

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Custom AI Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type instructions to guide the AI when formatting your text. For example: 'Format as a formal email draft. Avoid bullet points. Translate slang to formal language.'",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = "e.g., Format this as a professional message, fix any spelling, and present bullet points if there are lists.",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                },
                minLines = 3,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(text) },
                enabled = isModified || text.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (isModified) "Save Instructions" else "Saved",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
