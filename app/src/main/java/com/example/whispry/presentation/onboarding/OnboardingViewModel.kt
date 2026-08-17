// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatMessage
import com.example.whispry.service.BubbleService
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
    val batteryOptimizationIgnored: Boolean = false,
    val apiKey: String = "",
    val isApiKeyValid: Boolean = false,
    val isValidatingKey: Boolean = false,
    val keyValidationError: String? = null,
    val allPermissionsGranted: Boolean = false,
    val isCompleted: Boolean = false,
    val tutorialStep: TutorialStep = TutorialStep.Idle,
    val recordedText: String = ""
)

enum class TutorialStep {
    Idle,
    TapField,
    TapLogo,
    Recording,
    Processing,
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

    /** IME visibility at the moment a step starts, so TapField only advances on a fresh keyboard-open. */
    private var keyboardWasVisible = false

    init {
        checkPermissions()
        observeServiceEvents()
        observeImeState()
    }

    private fun observeImeState() {
        serviceBridge.imeBounds
            .map { it != null }
            .onEach { keyboardOpen ->
                if (keyboardOpen && !keyboardWasVisible && _state.value.tutorialStep == TutorialStep.TapField) {
                    _state.update { it.copy(tutorialStep = TutorialStep.TapLogo) }
                }
                keyboardWasVisible = keyboardOpen
            }
            .launchIn(viewModelScope)
    }

    private fun observeServiceEvents() {
        serviceBridge.triggerEvent
            .onEach { event ->
                when (event) {
                    is ServiceBridge.TriggerEvent.RecordingStarted -> {
                        if (_state.value.tutorialStep == TutorialStep.TapLogo) {
                            _state.update { it.copy(tutorialStep = TutorialStep.Recording) }
                        }
                    }
                    is ServiceBridge.TriggerEvent.RecordingStopped -> {
                        if (_state.value.tutorialStep == TutorialStep.Recording) {
                            _state.update { it.copy(tutorialStep = TutorialStep.Processing) }
                        }
                    }
                    is ServiceBridge.TriggerEvent.TranscriptionResult -> {
                        if (_state.value.tutorialStep == TutorialStep.Recording || _state.value.tutorialStep == TutorialStep.Processing) {
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
                        if (step == TutorialStep.Recording || step == TutorialStep.Processing || step == TutorialStep.TapLogo) {
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
        // Make sure the real keyboard-logo host is running, then reset the step flow.
        keyboardWasVisible = false
        try {
            val i = Intent(context, BubbleService::class.java)
            context.startForegroundService(i)
        } catch (e: Exception) {
            Log.e("OnboardingViewModel", "Failed to start BubbleService for tutorial", e)
        }
        _state.update { it.copy(tutorialStep = TutorialStep.TapField, recordedText = "") }
    }

    /** Retry the live practice after a failed attempt. */
    fun retryTutorial() = startTutorial()

    /** Remember which onboarding screen the user is on so leaving/reopening resumes there. */
    fun saveResumeRoute(route: String) {
        viewModelScope.launch {
            settingsProvider.setOnboardingResumeRoute(route)
        }
    }

    fun checkPermissions() {
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlay = Settings.canDrawOverlays(context)
        val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        
        val accessibility = isAccessibilityServiceEnabled() && ServiceLocator.triggerService != null
        val batteryOptimizationIgnored = com.example.whispry.service.OemBatteryOptimization.isIgnoringBatteryOptimizations(context)

        _state.update {
            it.copy(
                micPermissionGranted = mic,
                overlayPermissionGranted = overlay,
                phoneStatePermissionGranted = phone,
                accessibilityEnabled = accessibility,
                batteryOptimizationIgnored = batteryOptimizationIgnored,
                // Microphone, Draw-over and Accessibility are all required: the keyboard mic
                // button is an overlay anchored to the IME via the accessibility service.
                allPermissionsGranted = mic && overlay && accessibility
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
                        model = "openai/gpt-oss-120b",
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
                            apiKeyProvider.saveTranscriptionApiKey(key)
                            apiKeyProvider.saveFormattingApiKey(key)
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
