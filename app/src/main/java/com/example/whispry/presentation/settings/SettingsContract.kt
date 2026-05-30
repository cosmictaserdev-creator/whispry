package com.example.whispry.presentation.settings

import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.domain.model.WakeWordMode
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
    
    // Feature 3
    val triggerMode: TriggerMode = TriggerMode.VolumeButton,
    val availableTriggerModes: List<TriggerMode> = emptyList(),
    val smartTriggerSuppression: Boolean = true,
    
    // Feature 1
    val floatingWidgetEnabled: Boolean = true,
    
    // Feature 2 & 6
    val wakeWordEnabled: Boolean = false,
    val wakeWordPhrase: String = "hey whispry",
    val wakeWordMode: WakeWordMode = WakeWordMode.DEFAULT,
    
    // Feature 5
    val soundEnabled: Boolean = true,
    val soundStart: TriggerSound = TriggerSound.SIRI_CLICK,
    val soundSuccess: TriggerSound = TriggerSound.SOFT_CHIME,
    val soundError: TriggerSound = TriggerSound.SOFT_POP
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
    
    // Feature 3
    data class SetTriggerMode(val mode: TriggerMode) : SettingsIntent()
    data class SetSmartTriggerSuppression(val enabled: Boolean) : SettingsIntent()
    
    // Feature 1
    data class SetFloatingWidgetEnabled(val enabled: Boolean) : SettingsIntent()
    
    // Feature 2 & 6
    data class SetWakeWordEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetWakeWordPhrase(val phrase: String) : SettingsIntent()
    data class SetWakeWordMode(val mode: WakeWordMode) : SettingsIntent()
    data class SaveVoiceFingerprint(val fp: String) : SettingsIntent()
    
    // Feature 5
    data class SetSoundEnabled(val enabled: Boolean) : SettingsIntent()
    data class SetSoundStart(val sound: TriggerSound) : SettingsIntent()
    data class SetSoundSuccess(val sound: TriggerSound) : SettingsIntent()
    data class SetSoundError(val sound: TriggerSound) : SettingsIntent()
}
