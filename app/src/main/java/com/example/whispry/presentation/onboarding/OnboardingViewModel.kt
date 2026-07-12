package com.example.whispry.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatMessage
import com.example.whispry.service.ServiceBridge
import com.example.whispry.service.ServiceLocator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val micPermissionGranted: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val phoneStatePermissionGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val apiKey: String = "",
    val isApiKeyValid: Boolean = false,
    val isValidatingKey: Boolean = false,
    val keyValidationError: String? = null,
    val allPermissionsGranted: Boolean = false,
    val isCompleted: Boolean = false,
    val tutorialStep: TutorialStep = TutorialStep.Idle,
    val recordedText: String = "",
    // Live trigger config so the teach phase mirrors the user's real settings.
    val triggerVolumeKey: String = "VOLUME_DOWN", // "VOLUME_UP" | "VOLUME_DOWN"
    val isHoldGesture: Boolean = DataStoreKeys.DEFAULT_SINGLE_PRESS_TRIGGER // true = press-and-hold, false = double-press
) {
    /** Human-readable key label for the teach phase, e.g. "Volume Down". */
    val triggerKeyLabel: String
        get() = if (triggerVolumeKey == "VOLUME_UP") "Volume Up" else "Volume Down"
}

enum class TutorialStep {
    Idle,
    DoublePressMe,
    HoldMe,
    Recording,
    Success,
    Failed
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val apiKeyProvider: ApiKeyProvider,
    private val serviceBridge: ServiceBridge,
    private val chatApiService: GroqChatApiService
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        checkPermissions()
        observeServiceEvents()
        observeTriggerConfig()
    }

    private fun observeTriggerConfig() {
        settingsProvider.dataStore.data
            .onEach { prefs ->
                _state.update {
                    it.copy(
                        triggerVolumeKey = prefs[DataStoreKeys.TRIGGER_VOLUME_KEY] ?: "VOLUME_DOWN",
                        isHoldGesture = prefs[DataStoreKeys.SINGLE_PRESS_TRIGGER]
                            ?: DataStoreKeys.DEFAULT_SINGLE_PRESS_TRIGGER
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeServiceEvents() {
        serviceBridge.triggerEvent
            .onEach { event ->
                when (event) {
                    is ServiceBridge.TriggerEvent.RecordingStarted -> {
                        if (_state.value.tutorialStep == TutorialStep.HoldMe || _state.value.tutorialStep == TutorialStep.DoublePressMe) {
                            _state.update { it.copy(tutorialStep = TutorialStep.Recording) }
                        }
                    }
                    is ServiceBridge.TriggerEvent.RecordingStopped -> {
                        if (_state.value.tutorialStep == TutorialStep.Recording) {
                            _state.update { it.copy(tutorialStep = TutorialStep.HoldMe) } // Temporary state to show loading
                        }
                    }
                    is ServiceBridge.TriggerEvent.TranscriptionResult -> {
                        if (_state.value.tutorialStep == TutorialStep.HoldMe || _state.value.tutorialStep == TutorialStep.Recording) {
                            _state.update { it.copy(
                                tutorialStep = TutorialStep.Success,
                                recordedText = event.text
                            ) }
                        }
                    }
                    is ServiceBridge.TriggerEvent.TranscriptionFailed -> {
                        // The live-transcription payoff couldn't complete (bad network, API hiccup).
                        // Drop into a graceful failure state instead of hanging on the spinner.
                        val step = _state.value.tutorialStep
                        if (step == TutorialStep.Recording || step == TutorialStep.HoldMe || step == TutorialStep.DoublePressMe) {
                            _state.update { it.copy(tutorialStep = TutorialStep.Failed) }
                        }
                    }
                    is ServiceBridge.TriggerEvent.Idle -> {
                        // Handle idle if needed
                    }
                    else -> { /* widget-only events (cancel arming etc.) don't affect the tutorial */ }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startTutorial() {
        // Enter the flow at the step that matches the user's saved gesture.
        val firstStep = if (_state.value.isHoldGesture) TutorialStep.HoldMe else TutorialStep.DoublePressMe
        _state.update { it.copy(tutorialStep = firstStep, recordedText = "") }
    }

    /** Retry the live practice after a failed attempt. */
    fun retryTutorial() = startTutorial()

    fun checkPermissions() {
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlay = Settings.canDrawOverlays(context)
        val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        
        val accessibility = isAccessibilityServiceEnabled() && ServiceLocator.triggerService != null
        
        _state.update {
            it.copy(
                micPermissionGranted = mic,
                overlayPermissionGranted = overlay,
                phoneStatePermissionGranted = phone,
                accessibilityEnabled = accessibility,
                // Only microphone + accessibility are required to continue. Overlay (the bubble) is
                // optional with a degraded fallback; Phone State is requested just-in-time in Settings.
                allPermissionsGranted = mic && accessibility
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

    fun updateApiKey(key: String) {
        _state.update { it.copy(apiKey = key, keyValidationError = null) }
    }

    fun validateAndSaveApiKey() {
        val key = _state.value.apiKey.trim()
        if (key.isBlank()) return

        // Cheap shape check first — avoids a network round-trip on an obvious typo.
        if (!key.startsWith("gsk_") || key.length < 40) {
            _state.update { it.copy(keyValidationError = "That doesn't look like a Groq key — it should start with \"gsk_\".") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isValidatingKey = true, keyValidationError = null) }

            // Verify the key actually works with a tiny live call, so a bad key is caught here
            // instead of surfacing as a mysterious authorization error at the first transcription.
            val result = runCatching {
                chatApiService.chatCompletion(
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    authorization = "Bearer $key",
                    request = ChatCompletionRequest(
                        model = "llama-3.3-70b-versatile",
                        messages = listOf(ChatMessage(role = "user", content = "ping")),
                        maxTokens = 1,
                        temperature = 0f
                    )
                )
            }

            result.fold(
                onSuccess = { response ->
                    when {
                        response.isSuccessful -> {
                            apiKeyProvider.saveApiKey(key)
                            _state.update { it.copy(isValidatingKey = false, isApiKeyValid = true) }
                        }
                        response.code() == 401 -> _state.update {
                            it.copy(isValidatingKey = false, keyValidationError = "Groq rejected this key. Double-check it and try again.")
                        }
                        else -> _state.update {
                            it.copy(isValidatingKey = false, keyValidationError = "Couldn't verify the key (error ${response.code()}). Please try again.")
                        }
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(isValidatingKey = false, keyValidationError = "Couldn't reach Groq — check your connection and try again.")
                    }
                }
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsProvider.setOnboardingCompleted(true)
            _state.update { it.copy(isCompleted = true) }
        }
    }
}
