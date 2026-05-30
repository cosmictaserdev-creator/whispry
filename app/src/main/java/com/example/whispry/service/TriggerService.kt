package com.example.whispry.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.domain.repository.TriggerRepository
import com.example.whispry.util.HapticHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TriggerService : AccessibilityService() {

    private val TAG = "Whispry_TriggerService"

    @Inject
    lateinit var serviceBridge: ServiceBridge

    @Inject
    lateinit var hapticHelper: HapticHelper

    @Inject
    lateinit var settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var triggerRepository: TriggerRepository

    @Inject
    lateinit var soundManager: SoundManager

    // cached settings
    private var doublePressWindowMs = 400L
    private var useHaptics = true
    private var useSmartSuppression = true
    private var currentTriggerMode: TriggerMode = TriggerMode.VolumeButton

    // ------------------------------------------------------------------
    // State machine
    // ------------------------------------------------------------------

    private enum class TriggerState {
        IDLE,
        FIRST_PRESS_DETECTED,
        RECORDING
    }

    private var triggerState = TriggerState.IDLE
    private var firstPressTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    // ------------------------------------------------------------------
    // AccessibilityService lifecycle
    // ------------------------------------------------------------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected")
        ServiceLocator.triggerService = this

        // Observe settings
        serviceScope.launch {
            settingsProvider.doublePressInterval.collect {
                doublePressWindowMs = it
            }
        }
        serviceScope.launch {
            settingsProvider.hapticFeedback.collect {
                useHaptics = it
            }
        }
        serviceScope.launch {
            settingsProvider.smartTriggerSuppression.collect {
                useSmartSuppression = it
            }
        }
        serviceScope.launch {
            triggerRepository.getActiveTriggerMode().collect { mode ->
                currentTriggerMode = mode
                updateServiceInfoForMode(mode)
            }
        }
    }

    private fun updateServiceInfoForMode(mode: TriggerMode) {
        serviceInfo = serviceInfo.apply {
            flags = when (mode) {
                is TriggerMode.VolumeButton,
                is TriggerMode.ActionButton -> 
                    flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                else -> 
                    flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        ServiceLocator.triggerService = null
        handler.removeCallbacksAndMessages(null)
        resetToIdle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        resetToIdle()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return when (currentTriggerMode) {
            is TriggerMode.VolumeButton -> handleVolumeKeyEvent(event)
            is TriggerMode.ActionButton -> handleActionButtonEvent(event)
            else -> false
        }
    }

    private fun handleVolumeKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount != 0) {
            return triggerState == TriggerState.RECORDING
        }

        return when (triggerState) {
            TriggerState.IDLE -> handleIdleState(event)
            TriggerState.FIRST_PRESS_DETECTED -> handleFirstPressState(event)
            TriggerState.RECORDING -> handleRecordingState(event)
        }
    }

    private fun handleActionButtonEvent(event: KeyEvent): Boolean {
        val actionKeycodes = listOf(
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_ASSIST,
            219, 231
        )
        if (event.keyCode !in actionKeycodes) return false
        
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    triggerState = TriggerState.RECORDING
                    if (useHaptics) hapticHelper.vibrateShort()
                    soundManager.play(SoundEvent.TRIGGER_START)
                    serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
                    true
                } else false
            }
            KeyEvent.ACTION_UP -> {
                if (triggerState == TriggerState.RECORDING) {
                    triggerState = TriggerState.IDLE
                    soundManager.play(SoundEvent.TRIGGER_STOP)
                    serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
                    true
                } else false
            }
            else -> false
        }
    }

    private fun handleIdleState(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        firstPressTime = System.currentTimeMillis()
        triggerState = TriggerState.FIRST_PRESS_DETECTED
        handler.postDelayed({
            if (triggerState == TriggerState.FIRST_PRESS_DETECTED) resetToIdle()
        }, doublePressWindowMs)
        return false
    }

    private fun handleFirstPressState(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val gap = System.currentTimeMillis() - firstPressTime
        return if (gap < doublePressWindowMs) {
            handler.removeCallbacksAndMessages(null)
            if (useSmartSuppression && shouldSuppressTrigger()) {
                resetToIdle()
                return false
            }
            triggerState = TriggerState.RECORDING
            if (useHaptics) hapticHelper.vibrateShort()
            soundManager.play(SoundEvent.TRIGGER_START)
            
            val intent = android.content.Intent(this, BubbleService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
            true
        } else {
            firstPressTime = System.currentTimeMillis()
            handler.postDelayed({
                if (triggerState == TriggerState.FIRST_PRESS_DETECTED) resetToIdle()
            }, doublePressWindowMs)
            false
        }
    }

    private fun shouldSuppressTrigger(): Boolean {
        if (audioManager.isMusicActive) return true
        var focusHeld = false
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .setOnAudioFocusChangeListener { }
            .build()
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) focusHeld = true
        else audioManager.abandonAudioFocusRequest(focusRequest)
        if (focusHeld) return true
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) return true
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing READ_PHONE_STATE permission, skipping call state check")
        }
        return false
    }

    private fun handleRecordingState(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            triggerState = TriggerState.IDLE
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
        }
        return true
    }

    private fun resetToIdle() {
        triggerState = TriggerState.IDLE
        firstPressTime = 0L
        serviceBridge.emit(ServiceBridge.TriggerEvent.Idle)
    }
}
