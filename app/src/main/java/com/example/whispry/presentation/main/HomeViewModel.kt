package com.example.whispry.presentation.main

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.model.TranscriptStats
import com.example.whispry.domain.model.UsageInfo
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.repository.UsageRepository
import com.example.whispry.service.BubbleService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class ServiceState {
    Active, Stopped, Unknown
}

data class HomeState(
    val totalTranscripts: Int = 0,
    val totalWords: Int = 0,
    val avgDurationMs: Long = 0,
    val recentTranscripts: List<Transcript> = emptyList(),
    val isServiceRunning: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val serviceState: ServiceState = ServiceState.Unknown,
    val usageInfo: UsageInfo = UsageInfo(0, 0)
)

sealed class HomeIntent {
    data object RefreshStats : HomeIntent()
    data object ToggleService : HomeIntent()
    data object CheckPermissions : HomeIntent()
    data object StartManualRecording : HomeIntent()
    data object StopManualRecording : HomeIntent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TranscriptRepository,
    @ApplicationContext private val context: Context,
    private val serviceBridge: com.example.whispry.service.ServiceBridge,
    private val soundManager: com.example.whispry.service.SoundManager,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val serviceState: StateFlow<ServiceState> = flow {
        while (true) {
            val running = isServiceRunning() && isAccessibilityServiceEnabled(context)
            emit(if (running) ServiceState.Active else ServiceState.Stopped)
            delay(2000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceState.Unknown)

    init {
        observeTranscripts()
        observeUsage()
        checkPermissions()
        
        // Sync serviceState with HomeState
        serviceState.onEach { state ->
            _state.update { it.copy(serviceState = state) }
        }.launchIn(viewModelScope)
    }

    private fun observeUsage() {
        usageRepository.observeTodayUsage()
            .onEach { usage -> _state.update { it.copy(usageInfo = usage) } }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun isServiceRunning(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE)
            ?.any { it.service.className == BubbleService::class.java.name } == true
    }

    private fun observeTranscripts() {
        // Observe optimized stats
        repository.getStats()
            .onEach { stats ->
                _state.update {
                    it.copy(
                        totalTranscripts = stats.totalCount,
                        totalWords = stats.totalWords,
                        avgDurationMs = stats.averageDurationMs
                    )
                }
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .launchIn(viewModelScope)

        // Observe recent transcripts separately (limited to 3)
        repository.getRecentTranscripts(3)
            .onEach { transcripts ->
                _state.update { it.copy(recentTranscripts = transcripts) }
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        
        // Microphone
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Microphone")
        }

        // Overlay is intentionally NOT part of this guard — it's non-essential (the bubble is optional
        // and the floating widget is off by default). It's requested just-in-time when the user turns
        // on the Floating Widget in Settings.

        // Notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missing.add("Notifications")
            }
        }
        
        // Accessibility - Important check
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        if (!isAccessibilityEnabled) {
            missing.add("Accessibility")
        }

        // Phone State is intentionally NOT part of this guard — it's a non-essential permission
        // requested just-in-time only when the user enables call suppression in Settings.

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
            HomeIntent.StartManualRecording -> {
                soundManager.play(com.example.whispry.service.SoundEvent.TRIGGER_START)
                val i = android.content.Intent(context, BubbleService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
                serviceBridge.emit(com.example.whispry.service.ServiceBridge.TriggerEvent.RecordingStarted)
            }
            HomeIntent.StopManualRecording -> {
                soundManager.play(com.example.whispry.service.SoundEvent.TRIGGER_STOP)
                serviceBridge.emit(com.example.whispry.service.ServiceBridge.TriggerEvent.RecordingStopped)
            }
        }
    }
}
