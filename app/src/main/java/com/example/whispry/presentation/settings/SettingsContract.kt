package com.example.whispry.presentation.settings

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
    val accentColor: String = "Purple"
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
}
