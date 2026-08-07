package com.example.whispry.data.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val dataStore = context.dataStore

    // ------------------------------------------------------------------
    // Flows
    // ------------------------------------------------------------------

    val language: Flow<String> = dataStore.data.map { it[DataStoreKeys.LANGUAGE] ?: "en" }
    val doublePressInterval: Flow<Long> = dataStore.data.map { it[DataStoreKeys.DOUBLE_PRESS_INTERVAL] ?: 400L }
    val hapticFeedback: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.HAPTIC_FEEDBACK] ?: true }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.ONBOARDING_COMPLETED] ?: false }
    val customVocabulary: Flow<String> = dataStore.data.map { it[DataStoreKeys.CUSTOM_VOCABULARY] ?: "" }
    val temperature: Flow<Float> = dataStore.data.map { it[DataStoreKeys.TEMPERATURE] ?: 0.0f }
    val bubbleSize: Flow<String> = dataStore.data.map { it[DataStoreKeys.BUBBLE_SIZE] ?: "Medium" }
    val autoStartBoot: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.AUTO_START_BOOT] ?: true }
    val accentColor: Flow<String> = dataStore.data.map { it[DataStoreKeys.ACCENT_COLOR] ?: "Purple" }
    val customAiInstructions: Flow<String> = dataStore.data.map { it[DataStoreKeys.CUSTOM_AI_INSTRUCTIONS] ?: "" }
    val appAwareToneEnabled: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.APP_AWARE_TONE_ENABLED] ?: false }
    val translateTargetLanguage: Flow<String> = dataStore.data.map { it[DataStoreKeys.TRANSLATE_TARGET_LANGUAGE] ?: "English" }
    val voiceCommandsEnabled: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.VOICE_COMMANDS_ENABLED] ?: true }

    // Hands-free trigger (press to start, press again to stop).
    val handsFreeMode: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.HANDS_FREE_MODE] ?: false }
    val handsFreeArmingDelayMs: Flow<Long> = dataStore.data.map {
        it[DataStoreKeys.HANDS_FREE_ARMING_DELAY_MS] ?: DataStoreKeys.DEFAULT_HANDS_FREE_ARMING_DELAY_MS
    }

    // Plain press-and-hold arming delay (hands-free off).
    val singlePressHoldDelayMs: Flow<Long> = dataStore.data.map {
        it[DataStoreKeys.SINGLE_PRESS_HOLD_DELAY_MS] ?: DataStoreKeys.DEFAULT_SINGLE_PRESS_HOLD_DELAY_MS
    }

    // Universal Press Actions (opt-in).
    val pressActionsEnabled: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.PRESS_ACTIONS_ENABLED] ?: false }
    val singlePressAction: Flow<String> = dataStore.data.map { it[DataStoreKeys.SINGLE_PRESS_ACTION] ?: "NORMAL" }
    val doublePressAction: Flow<String> = dataStore.data.map { it[DataStoreKeys.DOUBLE_PRESS_ACTION] ?: "NORMAL" }

    // Bug 1
    // Default false: the call-suppression half of this feature needs READ_PHONE_STATE, which is
    // only ever requested from the Settings toggle itself (just-in-time). Defaulting true here
    // made the toggle look enabled on a fresh install despite that permission never having been
    // granted or asked for — the toggle now honestly reflects "off until you turn it on".
    val smartTriggerSuppression: Flow<Boolean> = dataStore.data.map { it[DataStoreKeys.SMART_TRIGGER_SUPPRESSION] ?: false }

    // Multi-provider AI support: transcription and formatting resolve independently.
    val transcriptionProviderPreset: Flow<com.example.whispry.domain.model.TranscriptionProviderPreset> =
        dataStore.data.map {
            com.example.whispry.domain.model.TranscriptionProviderPreset.fromName(
                it[DataStoreKeys.TRANSCRIPTION_PROVIDER_PRESET]
            )
        }
    val transcriptionCustomBaseUrl: Flow<String> =
        dataStore.data.map { it[DataStoreKeys.TRANSCRIPTION_CUSTOM_BASE_URL] ?: "" }
    val transcriptionCustomModel: Flow<String> =
        dataStore.data.map { it[DataStoreKeys.TRANSCRIPTION_CUSTOM_MODEL] ?: "" }

    val formattingProviderPreset: Flow<com.example.whispry.domain.model.FormattingProviderPreset> =
        dataStore.data.map {
            com.example.whispry.domain.model.FormattingProviderPreset.fromName(
                it[DataStoreKeys.FORMATTING_PROVIDER_PRESET]
            )
        }
    val formattingCustomBaseUrl: Flow<String> =
        dataStore.data.map { it[DataStoreKeys.FORMATTING_CUSTOM_BASE_URL] ?: "" }
    val formattingCustomModel: Flow<String> =
        dataStore.data.map { it[DataStoreKeys.FORMATTING_CUSTOM_MODEL] ?: "" }

    val hinglishOutputEnabled: Flow<Boolean> =
        dataStore.data.map { it[DataStoreKeys.HINGLISH_OUTPUT_ENABLED] ?: false }

    val instantModeEnabled: Flow<Boolean> =
        dataStore.data.map { it[DataStoreKeys.INSTANT_MODE_ENABLED] ?: false }

    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------

    suspend fun setLanguage(value: String) = dataStore.edit { it[DataStoreKeys.LANGUAGE] = value }
    suspend fun setDoublePressInterval(value: Long) = dataStore.edit { it[DataStoreKeys.DOUBLE_PRESS_INTERVAL] = value }
    suspend fun setHapticFeedback(value: Boolean) = dataStore.edit { it[DataStoreKeys.HAPTIC_FEEDBACK] = value }
    suspend fun setOnboardingCompleted(value: Boolean) = dataStore.edit { it[DataStoreKeys.ONBOARDING_COMPLETED] = value }
    suspend fun setCustomVocabulary(value: String) = dataStore.edit { it[DataStoreKeys.CUSTOM_VOCABULARY] = value }
    suspend fun setTemperature(value: Float) = dataStore.edit { it[DataStoreKeys.TEMPERATURE] = value }
    suspend fun setBubbleSize(value: String) = dataStore.edit { it[DataStoreKeys.BUBBLE_SIZE] = value }
    suspend fun setAutoStartBoot(value: Boolean) = dataStore.edit { it[DataStoreKeys.AUTO_START_BOOT] = value }
    suspend fun setAccentColor(value: String) = dataStore.edit { it[DataStoreKeys.ACCENT_COLOR] = value }
    suspend fun setSmartTriggerSuppression(value: Boolean) = dataStore.edit { it[DataStoreKeys.SMART_TRIGGER_SUPPRESSION] = value }
    suspend fun setCustomAiInstructions(value: String) = dataStore.edit { it[DataStoreKeys.CUSTOM_AI_INSTRUCTIONS] = value }
    suspend fun setAppAwareToneEnabled(value: Boolean) = dataStore.edit { it[DataStoreKeys.APP_AWARE_TONE_ENABLED] = value }
    suspend fun setTranslateTargetLanguage(value: String) = dataStore.edit { it[DataStoreKeys.TRANSLATE_TARGET_LANGUAGE] = value }
    suspend fun setVoiceCommandsEnabled(value: Boolean) = dataStore.edit { it[DataStoreKeys.VOICE_COMMANDS_ENABLED] = value }
    suspend fun setHandsFreeMode(value: Boolean) = dataStore.edit { it[DataStoreKeys.HANDS_FREE_MODE] = value }
    suspend fun setPressActionsEnabled(value: Boolean) = dataStore.edit { it[DataStoreKeys.PRESS_ACTIONS_ENABLED] = value }
    suspend fun setSinglePressAction(value: String) = dataStore.edit { it[DataStoreKeys.SINGLE_PRESS_ACTION] = value }
    suspend fun setDoublePressAction(value: String) = dataStore.edit { it[DataStoreKeys.DOUBLE_PRESS_ACTION] = value }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}
