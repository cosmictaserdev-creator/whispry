package com.example.whispry.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.repository.TriggerRepository
import com.example.whispry.service.BubbleService
import com.example.whispry.service.ServiceLocator
import com.example.whispry.service.TrainedModelMatcher
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
    private val triggerRepository: TriggerRepository,
    private val transcriptRepository: com.example.whispry.domain.repository.TranscriptRepository,
    private val soundManager: com.example.whispry.service.SoundManager,
    val trainedModelMatcher: TrainedModelMatcher // Make it val
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(
            apiKey = apiKeyProvider.getApiKey(),
            availableTriggerModes = triggerRepository.getAvailableTriggerModes(),
            isActionButtonSupported = checkActionButtonSupport()
        ) }
        observeSettings()
        refreshStatus()
    }

    private fun checkActionButtonSupport(): Boolean {
        // Common keycodes for action/assist buttons
        val actionKeycodes = intArrayOf(
            android.view.KeyEvent.KEYCODE_VOICE_ASSIST,
            android.view.KeyEvent.KEYCODE_ASSIST,
            219, 231 // Device-specific assist keys
        )
        return try {
            val deviceIds = android.view.InputDevice.getDeviceIds()
            deviceIds.any { id ->
                val device = android.view.InputDevice.getDevice(id)
                device?.hasKeys(*actionKeycodes)?.any { it } == true
            }
        } catch (e: Exception) {
            false
        }
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
        settingsProvider.dataStore.data.map { it[DataStoreKeys.CONSUME_VOLUME_KEYS] ?: true }.onEach { v -> _state.update { it.copy(consumeVolumeKeys = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.SINGLE_PRESS_TRIGGER] ?: false }.onEach { v -> _state.update { it.copy(singlePressTrigger = v) } }.launchIn(viewModelScope)
        settingsProvider.handsFreeMode.onEach { v -> _state.update { it.copy(handsFreeMode = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: true }.onEach { v -> _state.update { it.copy(floatingWidgetEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.GLASS_NAVBAR] ?: true }.onEach { v -> _state.update { it.copy(glassNavbar = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.GLASS_LIQUID_BACKDROP] ?: true }.onEach { v -> _state.update { it.copy(glassLiquidBackdrop = v) } }.launchIn(viewModelScope)
        
        // New Features
        settingsProvider.dataStore.data.map { it[DataStoreKeys.TRIGGER_VOLUME_KEY] ?: "VOLUME_DOWN" }.onEach { v -> _state.update { it.copy(triggerVolumeKey = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.DUCKING_ENABLED] ?: true }.onEach { v -> _state.update { it.copy(duckingEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.DUCKING_PERCENT] ?: 70 }.onEach { v -> _state.update { it.copy(duckPercent = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { 
            val name = it[DataStoreKeys.RETENTION_POLICY] ?: com.example.whispry.domain.model.RetentionPolicy.FOREVER.name
            try { com.example.whispry.domain.model.RetentionPolicy.valueOf(name) } catch (e: Exception) { com.example.whispry.domain.model.RetentionPolicy.FOREVER }
        }.onEach { v -> _state.update { it.copy(retentionPolicy = v) } }.launchIn(viewModelScope)

        // Feature 5
        settingsProvider.dataStore.data.map { it[DataStoreKeys.SOUND_ENABLED] ?: true }.onEach { v -> _state.update { it.copy(soundEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { TriggerSound.fromName(it[DataStoreKeys.SOUND_START]) }.onEach { v -> _state.update { it.copy(selectedSound = v) } }.launchIn(viewModelScope)
        
        settingsProvider.appAwareToneEnabled.onEach { v -> _state.update { it.copy(appAwareToneEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.voiceCommandsEnabled.onEach { v -> _state.update { it.copy(voiceCommandsEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.pressActionsEnabled.onEach { v -> _state.update { it.copy(pressActionsEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.singlePressAction.onEach { v -> _state.update { it.copy(singlePressAction = v) } }.launchIn(viewModelScope)
        settingsProvider.doublePressAction.onEach { v -> _state.update { it.copy(doublePressAction = v) } }.launchIn(viewModelScope)
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
                is SettingsIntent.ResetToDefaults -> {
                    settingsProvider.dataStore.edit { it.clear() }
                    apiKeyProvider.clearApiKey()
                    apiKeyProvider.clearFingerprint()
                    refreshStatus()
                }
                is SettingsIntent.SetTriggerMode -> triggerRepository.setTriggerMode(intent.mode)
                is SettingsIntent.SetSmartTriggerSuppression -> settingsProvider.setSmartTriggerSuppression(intent.enabled)
                is SettingsIntent.SetConsumeVolumeKeys -> settingsProvider.dataStore.edit { it[DataStoreKeys.CONSUME_VOLUME_KEYS] = intent.enabled }
                is SettingsIntent.SetSinglePressTrigger -> settingsProvider.dataStore.edit { it[DataStoreKeys.SINGLE_PRESS_TRIGGER] = intent.enabled }
                is SettingsIntent.SetHandsFreeMode -> settingsProvider.setHandsFreeMode(intent.enabled)
                is SettingsIntent.SetFloatingWidgetEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] = intent.enabled }
                is SettingsIntent.SetGlassNavbar -> settingsProvider.dataStore.edit { it[DataStoreKeys.GLASS_NAVBAR] = intent.enabled }
                is SettingsIntent.SetGlassLiquidBackdrop -> settingsProvider.dataStore.edit { it[DataStoreKeys.GLASS_LIQUID_BACKDROP] = intent.enabled }
                
                // Feature 5
                is SettingsIntent.SetSoundEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_ENABLED] = intent.enabled }
                is SettingsIntent.SetSoundPack -> {
                    settingsProvider.dataStore.edit { it[DataStoreKeys.SOUND_START] = intent.sound.name }
                    // Play a preview of the pack (WAKEUP sound)
                    soundManager.play(com.example.whispry.service.SoundEvent.TRIGGER_START, intent.sound)
                }
                is SettingsIntent.SetAppAwareToneEnabled -> settingsProvider.setAppAwareToneEnabled(intent.enabled)
                is SettingsIntent.SetVoiceCommandsEnabled -> settingsProvider.setVoiceCommandsEnabled(intent.enabled)
                is SettingsIntent.SetPressActionsEnabled -> settingsProvider.setPressActionsEnabled(intent.enabled)
                is SettingsIntent.SetSinglePressAction -> settingsProvider.setSinglePressAction(intent.action)
                is SettingsIntent.SetDoublePressAction -> settingsProvider.setDoublePressAction(intent.action)
                is SettingsIntent.SetTriggerVolumeKey -> settingsProvider.dataStore.edit { it[DataStoreKeys.TRIGGER_VOLUME_KEY] = intent.key }
                is SettingsIntent.SetDuckingEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.DUCKING_ENABLED] = intent.enabled }
                is SettingsIntent.SetDuckingPercent -> settingsProvider.dataStore.edit { it[DataStoreKeys.DUCKING_PERCENT] = intent.percent }
                is SettingsIntent.SetRetentionPolicy -> settingsProvider.dataStore.edit { it[DataStoreKeys.RETENTION_POLICY] = intent.policy.name }
                is SettingsIntent.ClearAllTranscripts -> {
                    transcriptRepository.deleteAll()
                }
                is SettingsIntent.ClearAudioCache -> {
                    val dir = java.io.File(context.cacheDir, "recordings")
                    var count = 0
                    if (dir.exists()) {
                        dir.listFiles()?.forEach { 
                            if (it.delete()) count++
                        }
                    }
                    // Show confirmation (using Toast for simplicity in ViewModel context, 
                    // or could emit a SideEffect)
                    android.widget.Toast.makeText(context, "Successfully cleaned $count cached files", android.widget.Toast.LENGTH_SHORT).show()
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
