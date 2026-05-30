package com.example.whispry.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
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
    val recordedText: String = ""
)

enum class TutorialStep {
    Idle,
    DoublePressMe,
    HoldMe,
    Recording,
    Success
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val apiKeyProvider: ApiKeyProvider,
    private val serviceBridge: ServiceBridge,
    private val groqDataSource: GroqRemoteDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        checkPermissions()
        observeServiceEvents()
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
                    is ServiceBridge.TriggerEvent.Idle -> {
                        // Handle idle if needed
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startTutorial() {
        _state.update { it.copy(tutorialStep = TutorialStep.DoublePressMe) }
    }

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
                allPermissionsGranted = mic && overlay && phone && accessibility
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
        val key = _state.value.apiKey
        if (key.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isValidatingKey = true, keyValidationError = null) }
            
            // Dummy validation for now
            kotlinx.coroutines.delay(1500)
            
            if (key.startsWith("gsk_") && key.length >= 40) {
                apiKeyProvider.saveApiKey(key)
                _state.update { it.copy(isValidatingKey = false, isApiKeyValid = true) }
            } else {
                _state.update { it.copy(isValidatingKey = false, keyValidationError = "Invalid Groq API Key format") }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsProvider.setOnboardingCompleted(true)
            _state.update { it.copy(isCompleted = true) }
        }
    }
}
