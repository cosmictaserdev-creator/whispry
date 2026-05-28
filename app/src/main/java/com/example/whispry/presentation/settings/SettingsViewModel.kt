package com.example.whispry.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.service.BubbleService
import com.example.whispry.service.ServiceLocator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(apiKey = apiKeyProvider.getApiKey()) }
        observeSettings()
        refreshStatus()
    }

    private fun observeSettings() {
        settingsProvider.language.onEach { v -> _state.update { it.copy(language = v) } }.launchIn(viewModelScope)
        settingsProvider.doublePressInterval.onEach { v -> _state.update { it.copy(doublePressInterval = v) } }.launchIn(viewModelScope)
        settingsProvider.hapticFeedback.onEach { v -> _state.update { it.copy(hapticFeedback = v) } }.launchIn(viewModelScope)
        settingsProvider.customVocabulary.onEach { v -> _state.update { it.copy(customVocabulary = v) } }.launchIn(viewModelScope)
        settingsProvider.temperature.onEach { v -> _state.update { it.copy(temperature = v) } }.launchIn(viewModelScope)
        settingsProvider.bubbleSize.onEach { v -> _state.update { it.copy(bubbleSize = v) } }.launchIn(viewModelScope)
        settingsProvider.autoStartBoot.onEach { v -> _state.update { it.copy(autoStartBoot = v) } }.launchIn(viewModelScope)
        settingsProvider.accentColor.onEach { v -> _state.update { it.copy(accentColor = v) } }.launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.UpdateApiKey -> _state.update { it.copy(apiKey = intent.apiKey, isSaved = false) }
                is SettingsIntent.SaveApiKey -> {
                    apiKeyProvider.saveApiKey(_state.value.apiKey)
                    _state.update { it.copy(isSaved = true) }
                }
                is SettingsIntent.ClearApiKey -> {
                    apiKeyProvider.clearApiKey()
                    _state.update { it.copy(apiKey = "", isSaved = false) }
                }
                is SettingsIntent.SetLanguage -> settingsProvider.setLanguage(intent.language)
                is SettingsIntent.SetDoublePressInterval -> settingsProvider.setDoublePressInterval(intent.ms)
                is SettingsIntent.SetHapticFeedback -> settingsProvider.setHapticFeedback(intent.enabled)
                is SettingsIntent.SetCustomVocabulary -> settingsProvider.setCustomVocabulary(intent.vocab)
                is SettingsIntent.SetTemperature -> settingsProvider.setTemperature(intent.temp)
                is SettingsIntent.SetBubbleSize -> settingsProvider.setBubbleSize(intent.size)
                is SettingsIntent.SetAutoStartBoot -> settingsProvider.setAutoStartBoot(intent.enabled)
                is SettingsIntent.SetAccentColor -> settingsProvider.setAccentColor(intent.colorName)
                is SettingsIntent.RefreshStatus -> refreshStatus()
                is SettingsIntent.OpenAccessibilitySettings -> {
                    val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(i)
                }
                is SettingsIntent.RestartService -> {
                    val i = Intent(context, BubbleService::class.java)
                    context.stopService(i)
                    context.startForegroundService(i)
                }
                is SettingsIntent.ResetOnboarding -> {
                    settingsProvider.setOnboardingCompleted(false)
                }
            }
        }
    }

    private fun refreshStatus() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isServiceRunning = ServiceLocator.triggerService != null
        _state.update { 
            it.copy(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isServiceRunning = isServiceRunning
            ) 
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${context.packageName}/com.example.whispry.service.TriggerService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(expectedComponentName) == true
    }
}
