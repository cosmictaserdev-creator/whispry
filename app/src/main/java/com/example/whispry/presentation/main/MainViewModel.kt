// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.AccentColorSet
import com.example.whispry.ui.theme.resolveAccentColors
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

    private val _onboardingStartDestination = MutableStateFlow<String?>(null)
    val onboardingStartDestination: StateFlow<String?> = _onboardingStartDestination.asStateFlow()

    val accentColor: StateFlow<AccentColorSet> = settingsProvider.accentColor
        .map { colorName -> resolveAccentColors(colorName) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = resolveAccentColors(null)
        )

    init {
        viewModelScope.launch {
            settingsProvider.onboardingCompleted.collect {
                _onboardingCompleted.value = it
            }
        }
        viewModelScope.launch {
            // Resume the last onboarding screen instead of restarting at Intro on leave/reopen.
            settingsProvider.onboardingResumeRoute.collect { route ->
                _onboardingStartDestination.value = route ?: "intro"
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsProvider.setOnboardingCompleted(true)
            settingsProvider.setOnboardingResumeRoute(null) // Reset to default
        }
    }

    fun revisitTutorial() {
        viewModelScope.launch {
            settingsProvider.setOnboardingResumeRoute("howItWorks")
            settingsProvider.setOnboardingCompleted(false)
        }
    }
}
