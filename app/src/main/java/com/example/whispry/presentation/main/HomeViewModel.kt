package com.example.whispry.presentation.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeState(
    val totalTranscripts: Int = 0,
    val totalWords: Int = 0,
    val avgDurationMs: Long = 0,
    val recentTranscripts: List<Transcript> = emptyList(),
    val isServiceRunning: Boolean = false,
    val missingPermissions: List<String> = emptyList()
)

sealed class HomeIntent {
    data object RefreshStats : HomeIntent()
    data object ToggleService : HomeIntent()
    data object CheckPermissions : HomeIntent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TranscriptRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        observeTranscripts()
        checkPermissions()
    }

    private fun observeTranscripts() {
        repository.getAllTranscripts().onEach { transcripts ->
            val totalWords = transcripts.sumOf { it.text.split("\\s+".toRegex()).size }
            val avgDuration = if (transcripts.isNotEmpty()) {
                transcripts.sumOf { it.durationMs } / transcripts.size
            } else 0L

            _state.update {
                it.copy(
                    totalTranscripts = transcripts.size,
                    totalWords = totalWords,
                    avgDurationMs = avgDuration,
                    recentTranscripts = transcripts.take(3)
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        
        // Microphone
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Microphone")
        }

        // Overlay
        if (!Settings.canDrawOverlays(context)) {
            missing.add("Overlay")
        }
        
        // Accessibility - Important check
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        if (!isAccessibilityEnabled) {
            missing.add("Accessibility")
        }

        _state.update { it.copy(missingPermissions = missing) }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = android.content.ComponentName(context, "com.example.whispry.service.TriggerService")
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val flatName = expectedComponentName.flattenToString()
        return enabledServices?.contains(flatName) == true || enabledServices?.contains(expectedComponentName.flattenToShortString()) == true
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.RefreshStats -> observeTranscripts()
            HomeIntent.ToggleService -> {
                // Logic to toggle service if needed
            }
            HomeIntent.CheckPermissions -> checkPermissions()
        }
    }
}
