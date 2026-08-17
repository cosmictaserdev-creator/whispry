// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.settings

import com.example.whispry.domain.model.FormattingProviderPreset
import com.example.whispry.domain.model.RetentionPolicy
import com.example.whispry.domain.model.TranscriptionProviderPreset
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.service.TriggerSound

/** Result of testing a provider API key with a live network call. */
sealed class ApiKeyTestState {
    object Idle : ApiKeyTestState()
    object Testing : ApiKeyTestState()
    object Success : ApiKeyTestState()
    data class Failure(val message: String) : ApiKeyTestState()
}

data class SettingsState(
    val language: String = "en",
    val doublePressInterval: Long = 400L,
    val hapticFeedback: Boolean = true,
    val customVocabulary: String = "",
    val temperature: Float = 0.0f,
    val bubbleSize: String = "Medium",
    val autoStartBoot: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val accentColor: String = "Purple",
    val instantModeEnabled: Boolean = false,

    // Trigger
    val triggerMode: TriggerMode = TriggerMode.Manual,
    val availableTriggerModes: List<TriggerMode> = emptyList(),
    val smartTriggerSuppression: Boolean = false,
    val consumeVolumeKeys: Boolean = true,
    val singlePressTrigger: Boolean = false,
    val handsFreeMode: Boolean = false,
    val handsFreeArmingDelayMs: Long = 450L,
    val singlePressHoldDelayMs: Long = 450L,
    val triggerVolumeKey: String = "VOLUME_DOWN",

    // Interface
    val floatingWidgetEnabled: Boolean = false,
    val keyboardLogoEnabled: Boolean = false,
    val widgetConfig: com.example.whispry.service.WidgetConfig = com.example.whispry.service.WidgetConfig(),
    val glassNavbar: Boolean = true,
    val glassLiquidBackdrop: Boolean = true,

    // Sounds
    val soundEnabled: Boolean = true,
    val selectedSound: TriggerSound = TriggerSound.WHISPRY_D,

    // Audio Ducking
    val duckingEnabled: Boolean = true,
    val duckPercent: Int = 70,

    // Retention
    val retentionPolicy: RetentionPolicy = RetentionPolicy.FOREVER,

    // App-Aware Tone
    val appAwareToneEnabled: Boolean = false,

    // Voice Commands + expand/insert first-word router
    val voiceCommandsEnabled: Boolean = true,

    // Universal Press Actions (opt-in)
    val pressActionsEnabled: Boolean = false,
    val singlePressAction: String = "NORMAL",
    val doublePressAction: String = "NORMAL",

    // Premium feature alerts (opt-in)
    val premiumReminderEnabled: Boolean = false,

    // Multi-provider AI support: transcription and formatting resolve independently.
    val transcriptionProviderPreset: TranscriptionProviderPreset = TranscriptionProviderPreset.GROQ,
    val transcriptionCustomBaseUrl: String = "",
    val transcriptionCustomModel: String = "",
    val transcriptionApiKey: String = "",
    val formattingProviderPreset: FormattingProviderPreset = FormattingProviderPreset.GROQ,
    val formattingCustomBaseUrl: String = "",
    val formattingCustomModel: String = "",
    val formattingApiKey: String = "",
    val transcriptionKeyTestState: ApiKeyTestState = ApiKeyTestState.Idle,
    val formattingKeyTestState: ApiKeyTestState = ApiKeyTestState.Idle,

    // Hinglish output: romanizes a "hi" transcript instead of leaving it in Devanagari.
    val hinglishOutputEnabled: Boolean = false,

    // App UI language (distinct from the transcription language above). "system" follows
    // the device language; anything else is a BCP-47 tag ("es", "hi", "ar", ...).
    val appLanguageTag: String = "system"
)

sealed class SettingsIntent {
    data class SetLanguage(val language: String) : SettingsIntent()
    data class SetDoublePressInterval(val ms: Long) : SettingsIntent()
    data class SetHapticFeedback(val enabled: Boolean) : SettingsIntent()
    data class SetCustomVocabulary(val vocab: String) : SettingsIntent()
    data class SetTemperature(val temp: Float) : SettingsIntent()
    data class SetBubbleSize(val size: String) : SettingsIntent()
    data class SetAutoStartBoot(val enabled: Boolean) : SettingsIntent()
    data class SetAccentColor(val colorName: String) : SettingsIntent()
    data class SetInstantModeEnabled(val enabled: Boolean) : SettingsIntent()
    object RefreshStatus : SettingsIntent()
    object OpenAccessibilitySettings : SettingsIntent()
    object RestartService : SettingsIntent()
    object ResetOnboarding : SettingsIntent()
    object ResetToDefaults : SettingsIntent()

    // New intents
    data class SetTriggerVolumeKey(val key: String) : SettingsIntent()
    data class SetDuckingEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetDuckingPercent(val percent: Int) : SettingsIntent()
    data class SetRetentionPolicy(val policy: RetentionPolicy) : SettingsIntent()
    object ClearAllTranscripts : SettingsIntent()
    object ClearAudioCache : SettingsIntent()

    // Feature 3
    data class SetTriggerMode(val mode: TriggerMode) : SettingsIntent()
    data class SetSmartTriggerSuppression(val enabled: Boolean) : SettingsIntent()
    data class SetConsumeVolumeKeys(val enabled: Boolean) : SettingsIntent()
    data class SetSinglePressTrigger(val enabled: Boolean) : SettingsIntent()
    data class SetHandsFreeMode(val enabled: Boolean) : SettingsIntent()
    data class SetHandsFreeArmingDelay(val ms: Long) : SettingsIntent()
    data class SetSinglePressHoldDelay(val ms: Long) : SettingsIntent()

    // Feature 1
    data class SetFloatingWidgetEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetKeyboardLogoEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetGlassNavbar(val enabled: Boolean) : SettingsIntent()
    data class SetGlassLiquidBackdrop(val enabled: Boolean) : SettingsIntent()
    
    // Feature 5
    data class SetSoundEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetSoundPack(val sound: TriggerSound) : SettingsIntent()

    // App-Aware Tone
    data class SetAppAwareToneEnabled(val enabled: Boolean) : SettingsIntent()

    // Voice Commands
    data class SetVoiceCommandsEnabled(val enabled: Boolean) : SettingsIntent()

    // Universal Press Actions
    data class SetPressActionsEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetSinglePressAction(val action: String) : SettingsIntent()
    data class SetDoublePressAction(val action: String) : SettingsIntent()

    // Floating widget (physical switch)
    data class SetWidgetIdleOpacity(val pct: Int) : SettingsIntent()
    data class SetWidgetFadeDelay(val ms: Long) : SettingsIntent()
    data class SetWidgetArmingDelay(val ms: Long) : SettingsIntent()
    data class SetWidgetCustomTriggers(val custom: Boolean) : SettingsIntent()
    data class SetWidgetSingleTapAction(val action: String) : SettingsIntent()
    data class SetWidgetDoubleTapAction(val action: String) : SettingsIntent()
    data class SetWidgetSoundMuted(val muted: Boolean) : SettingsIntent()
    data class SetWidgetAvoidKeyboard(val enabled: Boolean) : SettingsIntent()
    data class SetWidgetMotion(val value: String) : SettingsIntent()
    object EnterWidgetEditMode : SettingsIntent()

    // Premium feature alerts
    data class SetPremiumReminderEnabled(val enabled: Boolean) : SettingsIntent()

    // Multi-provider AI support
    data class SetTranscriptionProviderPreset(val preset: TranscriptionProviderPreset) : SettingsIntent()
    data class SetTranscriptionCustomBaseUrl(val url: String) : SettingsIntent()
    data class SetTranscriptionCustomModel(val model: String) : SettingsIntent()
    data class SetTranscriptionApiKey(val apiKey: String) : SettingsIntent()
    data class SetFormattingProviderPreset(val preset: FormattingProviderPreset) : SettingsIntent()
    data class SetFormattingCustomBaseUrl(val url: String) : SettingsIntent()
    data class SetFormattingCustomModel(val model: String) : SettingsIntent()
    data class SetFormattingApiKey(val apiKey: String) : SettingsIntent()
    object TestAndSaveTranscriptionApiKey : SettingsIntent()
    object TestAndSaveFormattingApiKey : SettingsIntent()
    data class SetHinglishOutputEnabled(val enabled: Boolean) : SettingsIntent()

    // App UI language
    data class SetAppLanguage(val languageTag: String) : SettingsIntent()
}
