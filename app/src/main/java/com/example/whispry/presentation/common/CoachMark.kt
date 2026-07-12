package com.example.whispry.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.WhispryTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** First-visit tips. One entry per screen we want to introduce. */
enum class CoachMark { PRESETS, SETTINGS }

@HiltViewModel
class CoachMarkViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    // Null until the first DataStore value arrives — treating "not loaded yet" as "not seen"
    // made every tip flash on every app open.
    private val seen: StateFlow<Set<String>?> = settingsProvider.dataStore.data
        .map { it[DataStoreKeys.COACH_MARKS_SEEN] ?: emptySet<String>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether [mark] still needs to be shown. False until the persisted set loads. */
    @Composable
    fun shouldShow(mark: CoachMark): Boolean {
        val current by seen.collectAsState()
        return current?.contains(mark.name) == false
    }

    fun markSeen(mark: CoachMark) = viewModelScope.launch {
        settingsProvider.dataStore.edit { prefs ->
            prefs[DataStoreKeys.COACH_MARKS_SEEN] = (prefs[DataStoreKeys.COACH_MARKS_SEEN] ?: emptySet()) + mark.name
        }
    }

    /** Clears all seen flags so the tips show again — backs the "Replay tips" entry in Settings. */
    fun replayAll() = viewModelScope.launch {
        settingsProvider.dataStore.edit { it.remove(DataStoreKeys.COACH_MARKS_SEEN) }
    }
}

/**
 * A dismissible first-visit tip. Dims the screen and shows a single card with one clear message.
 * Rendered inside the host activity (no system overlay permission needed). Tap anywhere to dismiss.
 */
@Composable
fun CoachMarkOverlay(
    visible: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, WhispryTokens.GlassBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    color = WhispryTokens.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    color = WhispryTokens.TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Got it",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
