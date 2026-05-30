package com.example.whispry.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.service.BubbleService
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.domain.repository.TriggerRepository
import com.example.whispry.service.TriggerSound
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsProvider: SettingsProvider,
    private val triggerRepository: TriggerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(
            apiKey = apiKeyProvider.getApiKey(),
            availableTriggerModes = triggerRepository.getAvailableTriggerModes()
        ) }
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
        
        triggerRepository.getActiveTriggerMode().onEach { v -> _state.update { it.copy(triggerMode = v) } }.launchIn(viewModelScope)
        settingsProvider.smartTriggerSuppression.onEach { v -> _state.update { it.copy(smartTriggerSuppression = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: true }.onEach { v -> _state.update { it.copy(floatingWidgetEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.WAKE_WORD_ENABLED] ?: false }.onEach { v -> _state.update { it.copy(wakeWordEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.WAKE_WORD_PHRASE] ?: \"hey whispry\" }.onEach { v -> _state.update { it.copy(wakeWordPhrase = v) } }.launchIn(viewModelScope)
        
        // Feature 5
        settingsProvider.dataStore.data.map { it[DataStoreKeys.SOUND_ENABLED] ?: true }.onEach { v -> _state.update { it.copy(soundEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { TriggerSound.fromName(it[DataStoreKeys.SOUND_START]) }.onEach { v -> _state.update { it.copy(soundStart = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { TriggerSound.fromName(it[DataStoreKeys.SOUND_SUCCESS]) }.onEach { v -> _state.update { it.copy(soundSuccess = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { TriggerSound.fromName(it[DataStoreKeys.SOUND_ERROR]) }.onEach { v -> _state.update { it.copy(soundError = v) } }.launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                // ...
                is SettingsIntent.SetWakeWordPhrase -> settingsProvider.dataStore.edit { it[DataStoreKeys.WAKE_WORD_PHRASE] = intent.phrase }
                
                // Feature 5
                is SettingsIntent.SetSoundEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_ENABLED] = intent.enabled }
                is SettingsIntent.SetSoundStart -> settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_START] = intent.sound.name }
                is SettingsIntent.SetSoundSuccess -> settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_SUCCESS] = intent.sound.name }
                is SettingsIntent.SetSoundError -> settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_ERROR] = intent.sound.name }
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
