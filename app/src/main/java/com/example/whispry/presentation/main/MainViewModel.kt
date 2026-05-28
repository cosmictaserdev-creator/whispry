package com.example.whispry.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.AccentPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _onboardingCompleted = MutableStateFlow<Boolean?>(null)
    val onboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()

    private val _onboardingStartDestination = MutableStateFlow("intro")
    val onboardingStartDestination: StateFlow<String> = _onboardingStartDestination.asStateFlow()

    val accentColor: StateFlow<AccentPreset> = settingsProvider.accentColor
        .map { colorName ->
            AccentPreset.entries.find { it.name == colorName } ?: AccentPreset.Purple
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccentPreset.Purple
        )

    init {
        viewModelScope.launch {
            settingsProvider.onboardingCompleted.collect {
                _onboardingCompleted.value = it
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsProvider.setOnboardingCompleted(true)
            _onboardingStartDestination.value = "intro" // Reset to default
        }
    }

    fun revisitTutorial() {
        viewModelScope.launch {
            _onboardingStartDestination.value = "howItWorks"
            settingsProvider.setOnboardingCompleted(false)
        }
    }
}
