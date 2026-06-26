package com.example.whispry.presentation.settings

import com.example.whispry.domain.model.RetentionPolicy
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.service.TriggerSound

data class SettingsState(
    val apiKey: String = "",
    val language: String = "en",
    val doublePressInterval: Long = 400L,
    val hapticFeedback: Boolean = true,
    val customVocabulary: String = "",
    val temperature: Float = 0.0f,
    val bubbleSize: String = "Medium",
    val autoStartBoot: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val accentColor: String = "Purple",

    // Trigger
    val triggerMode: TriggerMode = TriggerMode.VolumeButton,
    val availableTriggerModes: List<TriggerMode> = emptyList(),
    val isActionButtonSupported: Boolean = true,
    val smartTriggerSuppression: Boolean = true,
    val consumeVolumeKeys: Boolean = true,
    val singlePressTrigger: Boolean = false,
    val triggerVolumeKey: String = "VOLUME_DOWN",

    // Interface
    val floatingWidgetEnabled: Boolean = true,
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
    val voiceCommandsEnabled: Boolean = true
)

sealed class SettingsIntent {
    data class UpdateApiKey(val apiKey: String) : SettingsIntent()
    object SaveApiKey : SettingsIntent()
    object ClearApiKey : SettingsIntent()
    data class SetLanguage(val language: String) : SettingsIntent()
    data class SetDoublePressInterval(val ms: Long) : SettingsIntent()
    data class SetHapticFeedback(val enabled: Boolean) : SettingsIntent()
    data class SetCustomVocabulary(val vocab: String) : SettingsIntent()
    data class SetTemperature(val temp: Float) : SettingsIntent()
    data class SetBubbleSize(val size: String) : SettingsIntent()
    data class SetAutoStartBoot(val enabled: Boolean) : SettingsIntent()
    data class SetAccentColor(val colorName: String) : SettingsIntent()
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
    
    // Feature 1
    data class SetFloatingWidgetEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetGlassNavbar(val enabled: Boolean) : SettingsIntent()
    data class SetGlassLiquidBackdrop(val enabled: Boolean) : SettingsIntent()
    
    // Feature 5
    data class SetSoundEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetSoundPack(val sound: TriggerSound) : SettingsIntent()

    // App-Aware Tone
    data class SetAppAwareToneEnabled(val enabled: Boolean) : SettingsIntent()

    // Voice Commands
    data class SetVoiceCommandsEnabled(val enabled: Boolean) : SettingsIntent()
}
