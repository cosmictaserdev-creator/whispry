// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.domain.model.ProviderConfigResolver
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
    private val floatingWidgetManager: com.example.whispry.service.FloatingWidgetManager,
    private val groqChatApiService: GroqChatApiService,
    val trainedModelMatcher: TrainedModelMatcher // Make it val
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(
            availableTriggerModes = triggerRepository.getAvailableTriggerModes(),
            transcriptionApiKey = apiKeyProvider.getRawTranscriptionApiKey(),
            formattingApiKey = apiKeyProvider.getRawFormattingApiKey(),
            appLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "system" }
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
        settingsProvider.dataStore.data.map { it[DataStoreKeys.CONSUME_VOLUME_KEYS] ?: true }.onEach { v -> _state.update { it.copy(consumeVolumeKeys = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.SINGLE_PRESS_TRIGGER] ?: DataStoreKeys.DEFAULT_SINGLE_PRESS_TRIGGER }.onEach { v -> _state.update { it.copy(singlePressTrigger = v) } }.launchIn(viewModelScope)
        settingsProvider.handsFreeMode.onEach { v -> _state.update { it.copy(handsFreeMode = v) } }.launchIn(viewModelScope)
        settingsProvider.handsFreeArmingDelayMs.onEach { v -> _state.update { it.copy(handsFreeArmingDelayMs = v) } }.launchIn(viewModelScope)
        settingsProvider.singlePressHoldDelayMs.onEach { v -> _state.update { it.copy(singlePressHoldDelayMs = v) } }.launchIn(viewModelScope)
        settingsProvider.transcriptionProviderPreset.onEach { v -> _state.update { it.copy(transcriptionProviderPreset = v) } }.launchIn(viewModelScope)
        settingsProvider.transcriptionCustomBaseUrl.onEach { v -> _state.update { it.copy(transcriptionCustomBaseUrl = v) } }.launchIn(viewModelScope)
        settingsProvider.transcriptionCustomModel.onEach { v -> _state.update { it.copy(transcriptionCustomModel = v) } }.launchIn(viewModelScope)
        settingsProvider.formattingProviderPreset.onEach { v -> _state.update { it.copy(formattingProviderPreset = v) } }.launchIn(viewModelScope)
        settingsProvider.formattingCustomBaseUrl.onEach { v -> _state.update { it.copy(formattingCustomBaseUrl = v) } }.launchIn(viewModelScope)
        settingsProvider.formattingCustomModel.onEach { v -> _state.update { it.copy(formattingCustomModel = v) } }.launchIn(viewModelScope)
        settingsProvider.hinglishOutputEnabled.onEach { v -> _state.update { it.copy(hinglishOutputEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.instantModeEnabled.onEach { v -> _state.update { it.copy(instantModeEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: DataStoreKeys.DEFAULT_FLOATING_WIDGET_ENABLED }.onEach { v -> _state.update { it.copy(floatingWidgetEnabled = v) } }.launchIn(viewModelScope)
        settingsProvider.dataStore.data.map { it[DataStoreKeys.KEYBOARD_LOGO_ENABLED] ?: DataStoreKeys.DEFAULT_KEYBOARD_LOGO_ENABLED }.onEach { v -> _state.update { it.copy(keyboardLogoEnabled = v) } }.launchIn(viewModelScope)
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

        // Floating widget (physical switch)
        settingsProvider.dataStore.data
            .map { com.example.whispry.service.WidgetConfig.fromPreferences(it) }
            .onEach { v -> _state.update { it.copy(widgetConfig = v) } }
            .launchIn(viewModelScope)

        // Premium feature alerts (opt-in)
        settingsProvider.dataStore.data
            .map { it[DataStoreKeys.PREMIUM_REMINDERS_ENABLED] ?: DataStoreKeys.DEFAULT_PREMIUM_REMINDERS_ENABLED }
            .onEach { v -> _state.update { it.copy(premiumReminderEnabled = v) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.SetLanguage -> settingsProvider.setLanguage(intent.language)
                is SettingsIntent.SetDoublePressInterval -> settingsProvider.setDoublePressInterval(intent.ms)
                is SettingsIntent.SetHapticFeedback -> settingsProvider.setHapticFeedback(intent.enabled)
                is SettingsIntent.SetCustomVocabulary -> settingsProvider.setCustomVocabulary(intent.vocab)
                is SettingsIntent.SetTemperature -> settingsProvider.setTemperature(intent.temp)
                is SettingsIntent.SetBubbleSize -> settingsProvider.setBubbleSize(intent.size)
                is SettingsIntent.SetAutoStartBoot -> settingsProvider.setAutoStartBoot(intent.enabled)
                is SettingsIntent.SetAccentColor -> settingsProvider.setAccentColor(intent.colorName)
                is SettingsIntent.SetInstantModeEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.INSTANT_MODE_ENABLED] = intent.enabled }
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
                is SettingsIntent.SetHandsFreeMode -> {
                    settingsProvider.setHandsFreeMode(intent.enabled)
                    // Mutually exclusive with Press Actions — both take over the volume key
                    // entirely, and Press Actions silently wins if left on, making Hands-free
                    // look broken (this was today's bug: both stuck on at once).
                    if (intent.enabled) settingsProvider.setPressActionsEnabled(false)
                }
                is SettingsIntent.SetHandsFreeArmingDelay -> settingsProvider.dataStore.edit { it[DataStoreKeys.HANDS_FREE_ARMING_DELAY_MS] = intent.ms }
                is SettingsIntent.SetSinglePressHoldDelay -> settingsProvider.dataStore.edit { it[DataStoreKeys.SINGLE_PRESS_HOLD_DELAY_MS] = intent.ms }
                is SettingsIntent.SetFloatingWidgetEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] = intent.enabled }
                is SettingsIntent.SetKeyboardLogoEnabled -> {
                    settingsProvider.dataStore.edit { it[DataStoreKeys.KEYBOARD_LOGO_ENABLED] = intent.enabled }
                    // The logo lives inside BubbleService; make sure it is running to host it.
                    if (intent.enabled) {
                        try {
                            val i = Intent(context, BubbleService::class.java)
                            context.startForegroundService(i)
                        } catch (e: Exception) {
                            Log.e("SettingsViewModel", "Failed to start BubbleService for keyboard logo", e)
                        }
                    }
                }
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
                is SettingsIntent.SetPressActionsEnabled -> {
                    settingsProvider.setPressActionsEnabled(intent.enabled)
                    if (intent.enabled) settingsProvider.setHandsFreeMode(false)
                }
                is SettingsIntent.SetSinglePressAction -> settingsProvider.setSinglePressAction(intent.action)
                is SettingsIntent.SetDoublePressAction -> settingsProvider.setDoublePressAction(intent.action)
                is SettingsIntent.SetTriggerVolumeKey -> settingsProvider.dataStore.edit { it[DataStoreKeys.TRIGGER_VOLUME_KEY] = intent.key }
                is SettingsIntent.SetDuckingEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.DUCKING_ENABLED] = intent.enabled }
                is SettingsIntent.SetDuckingPercent -> settingsProvider.dataStore.edit { it[DataStoreKeys.DUCKING_PERCENT] = intent.percent }
                is SettingsIntent.SetRetentionPolicy -> settingsProvider.dataStore.edit { it[DataStoreKeys.RETENTION_POLICY] = intent.policy.name }
                is SettingsIntent.SetWidgetIdleOpacity -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_IDLE_OPACITY_PCT] = intent.pct }
                is SettingsIntent.SetWidgetFadeDelay -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_FADE_DELAY_MS] = intent.ms }
                is SettingsIntent.SetWidgetArmingDelay -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_ARMING_DELAY_MS] = intent.ms }
                is SettingsIntent.SetWidgetCustomTriggers -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_CUSTOM_TRIGGERS] = intent.custom }
                is SettingsIntent.SetWidgetSingleTapAction -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_SINGLE_TAP_ACTION] = intent.action }
                is SettingsIntent.SetWidgetDoubleTapAction -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_DOUBLE_TAP_ACTION] = intent.action }
                is SettingsIntent.SetWidgetSoundMuted -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_SOUND_MUTED] = intent.muted }
                is SettingsIntent.SetWidgetAvoidKeyboard -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_AVOID_KEYBOARD] = intent.enabled }
                is SettingsIntent.SetPremiumReminderEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.PREMIUM_REMINDERS_ENABLED] = intent.enabled }
                is SettingsIntent.SetWidgetMotion -> settingsProvider.dataStore.edit { it[DataStoreKeys.WIDGET_REDUCED_MOTION] = intent.value }
                is SettingsIntent.SetTranscriptionProviderPreset -> settingsProvider.dataStore.edit { it[DataStoreKeys.TRANSCRIPTION_PROVIDER_PRESET] = intent.preset.name }
                is SettingsIntent.SetTranscriptionCustomBaseUrl -> settingsProvider.dataStore.edit { it[DataStoreKeys.TRANSCRIPTION_CUSTOM_BASE_URL] = intent.url }
                is SettingsIntent.SetTranscriptionCustomModel -> settingsProvider.dataStore.edit { it[DataStoreKeys.TRANSCRIPTION_CUSTOM_MODEL] = intent.model }
                is SettingsIntent.SetTranscriptionApiKey -> _state.update {
                    it.copy(transcriptionApiKey = intent.apiKey, transcriptionKeyTestState = ApiKeyTestState.Idle)
                }
                is SettingsIntent.TestAndSaveTranscriptionApiKey -> testAndSaveTranscriptionKey()
                is SettingsIntent.SetFormattingProviderPreset -> settingsProvider.dataStore.edit { it[DataStoreKeys.FORMATTING_PROVIDER_PRESET] = intent.preset.name }
                is SettingsIntent.SetFormattingCustomBaseUrl -> settingsProvider.dataStore.edit { it[DataStoreKeys.FORMATTING_CUSTOM_BASE_URL] = intent.url }
                is SettingsIntent.SetFormattingCustomModel -> settingsProvider.dataStore.edit { it[DataStoreKeys.FORMATTING_CUSTOM_MODEL] = intent.model }
                is SettingsIntent.SetFormattingApiKey -> _state.update {
                    it.copy(formattingApiKey = intent.apiKey, formattingKeyTestState = ApiKeyTestState.Idle)
                }
                is SettingsIntent.TestAndSaveFormattingApiKey -> testAndSaveFormattingKey()
                is SettingsIntent.SetHinglishOutputEnabled -> settingsProvider.dataStore.edit { it[DataStoreKeys.HINGLISH_OUTPUT_ENABLED] = intent.enabled }
                is SettingsIntent.SetAppLanguage -> {
                    val locales = if (intent.languageTag == "system") {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(intent.languageTag)
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                    _state.update { it.copy(appLanguageTag = intent.languageTag) }
                }
                is SettingsIntent.EnterWidgetEditMode -> {
                    floatingWidgetManager.enterEditMode()
                    // Positioning happens on a live home-screen preview.
                    val home = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(home)
                }
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

    /** GET .../models with the given key — model-agnostic, so it works for both transcription
     *  and formatting keys regardless of which model the step is set to use. */
    private suspend fun testApiKey(baseUrl: String, apiKey: String): ApiKeyTestState {
        if (apiKey.isBlank()) return ApiKeyTestState.Failure("Enter an API key first.")
        if (baseUrl.isBlank()) return ApiKeyTestState.Failure("Set a Custom base URL first.")
        return runCatching {
            groqChatApiService.listModels(url = baseUrl + "models", authorization = "Bearer $apiKey")
        }.fold(
            onSuccess = { response ->
                when {
                    response.isSuccessful -> ApiKeyTestState.Success
                    response.code() == 401 || response.code() == 403 ->
                        ApiKeyTestState.Failure("Key rejected (${response.code()}). Double-check it.")
                    else -> ApiKeyTestState.Failure("Couldn't verify (error ${response.code()}).")
                }
            },
            onFailure = { ApiKeyTestState.Failure("Couldn't reach the server — check your connection.") }
        )
    }

    private suspend fun testAndSaveTranscriptionKey() {
        val key = _state.value.transcriptionApiKey.trim()
        _state.update { it.copy(transcriptionKeyTestState = ApiKeyTestState.Testing) }
        val resolved = ProviderConfigResolver.resolveTranscription(
            preset = _state.value.transcriptionProviderPreset,
            customBaseUrl = _state.value.transcriptionCustomBaseUrl,
            customModel = _state.value.transcriptionCustomModel,
            apiKey = key
        )
        val result = testApiKey(resolved.baseUrl, key)
        if (result is ApiKeyTestState.Success) apiKeyProvider.saveTranscriptionApiKey(key)
        _state.update { it.copy(transcriptionKeyTestState = result) }
    }

    private suspend fun testAndSaveFormattingKey() {
        val key = _state.value.formattingApiKey.trim()
        _state.update { it.copy(formattingKeyTestState = ApiKeyTestState.Testing) }
        val resolved = ProviderConfigResolver.resolveFormatting(
            preset = _state.value.formattingProviderPreset,
            customBaseUrl = _state.value.formattingCustomBaseUrl,
            customModel = _state.value.formattingCustomModel,
            apiKey = key
        )
        val result = testApiKey(resolved.baseUrl, key)
        if (result is ApiKeyTestState.Success) apiKeyProvider.saveFormattingApiKey(key)
        _state.update { it.copy(formattingKeyTestState = result) }
    }

    private fun refreshStatus() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isServiceRunning = ServiceLocator.triggerService != null
        val batteryOptimizationIgnored = com.example.whispry.service.OemBatteryOptimization.isIgnoringBatteryOptimizations(context)
        _state.update {
            it.copy(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isServiceRunning = isServiceRunning,
                batteryOptimizationIgnored = batteryOptimizationIgnored
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
