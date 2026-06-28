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
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    lateinit var settingsProvider: SettingsProvider

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
    private var consumeVolumeKeys = true
    private var isSinglePressEnabled = false
    private var currentTriggerMode: TriggerMode = TriggerMode.VolumeButton
    private var activeKeyCode: Int = KeyEvent.KEYCODE_VOLUME_DOWN

    // Universal Press Actions (opt-in). When enabled, the volume key becomes a tap-to-toggle
    // trigger: single press fires [singlePressAction], double press fires [doublePressAction].
    private var pressActionsEnabled = false
    private var singlePressActionStr = "NORMAL"
    private var doublePressActionStr = "NORMAL"
    private enum class PressState { IDLE, WAITING_SECOND, RECORDING }
    private var pressState = PressState.IDLE

    // ------------------------------------------------------------------
    // State machine
    // ------------------------------------------------------------------

    private enum class TriggerState {
        IDLE,
        FIRST_PRESS_DETECTED,
        SINGLE_PRESS_DELAY,
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
        serviceScope.launch {
            settingsProvider.dataStore.data.map { prefs ->
                prefs[DataStoreKeys.TRIGGER_VOLUME_KEY] ?: "VOLUME_DOWN"
            }.distinctUntilChanged().collect { keyPref ->
                activeKeyCode = when (keyPref) {
                    "VOLUME_UP" -> KeyEvent.KEYCODE_VOLUME_UP
                    else -> KeyEvent.KEYCODE_VOLUME_DOWN
                }
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.collect { prefs ->
                consumeVolumeKeys = prefs[DataStoreKeys.CONSUME_VOLUME_KEYS] ?: true
                isSinglePressEnabled = prefs[DataStoreKeys.SINGLE_PRESS_TRIGGER] ?: false
                pressActionsEnabled = prefs[DataStoreKeys.PRESS_ACTIONS_ENABLED] ?: false
                if (!pressActionsEnabled) pressState = PressState.IDLE
                singlePressActionStr = prefs[DataStoreKeys.SINGLE_PRESS_ACTION] ?: "NORMAL"
                doublePressActionStr = prefs[DataStoreKeys.DOUBLE_PRESS_ACTION] ?: "NORMAL"
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (!packageName.isNullOrBlank() && packageName != this.packageName) {
                Log.d(TAG, "Foreground package changed to: $packageName")
                ServiceLocator.lastForegroundPackage = packageName
            }
        }
    }

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
        if (event.keyCode != activeKeyCode) return false

        // Universal Press Actions take over the volume key entirely when enabled.
        if (pressActionsEnabled) {
            handlePressActionEvent(event)
            return true
        }

        val consumed = if (consumeVolumeKeys && isSinglePressEnabled) {
            handleSinglePressLogic(event)
        } else {
            when (triggerState) {
                TriggerState.IDLE -> handleIdleState(event)
                TriggerState.FIRST_PRESS_DETECTED -> handleFirstPressState(event)
                TriggerState.RECORDING -> handleRecordingState(event)
                else -> false
            }
        }

        return if (consumeVolumeKeys) true else consumed
    }

    private fun handleSinglePressLogic(event: KeyEvent): Boolean {
        return when (triggerState) {
            TriggerState.IDLE -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    triggerState = TriggerState.SINGLE_PRESS_DELAY
                    handler.postDelayed({
                        if (triggerState == TriggerState.SINGLE_PRESS_DELAY) {
                            startRecordingProcess()
                        }
                    }, 450) // Pre-delay to prevent accidental short press
                    true
                } else false
            }
            TriggerState.SINGLE_PRESS_DELAY -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    resetToIdle()
                    handler.removeCallbacksAndMessages(null)
                }
                true
            }
            TriggerState.RECORDING -> handleRecordingState(event)
            else -> false
        }
    }

    private fun startRecordingProcess() {
        if (useSmartSuppression && shouldSuppressTrigger()) {
            resetToIdle()
            return
        }
        triggerState = TriggerState.RECORDING
        if (useHaptics) hapticHelper.vibrateShort()
        soundManager.play(SoundEvent.TRIGGER_START)
        
        try {
            val intent = android.content.Intent(this, BubbleService::class.java).apply {
                action = BubbleService.ACTION_START_RECORDING
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BubbleService", e)
            resetToIdle()
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
                    
                    val intent = android.content.Intent(this, BubbleService::class.java).apply {
                        action = BubbleService.ACTION_START_RECORDING
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }

                    serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
                    true
                } else false
            }
            KeyEvent.ACTION_UP -> {
                if (triggerState == TriggerState.RECORDING) {
                    triggerState = TriggerState.IDLE
                    soundManager.play(SoundEvent.TRIGGER_STOP)
                    
                    val intent = android.content.Intent(this, BubbleService::class.java).apply {
                        action = BubbleService.ACTION_STOP_RECORDING
                    }
                    startService(intent)

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
            
            val intent = android.content.Intent(this, BubbleService::class.java).apply {
                action = BubbleService.ACTION_START_RECORDING
            }
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
            val intent = android.content.Intent(this, BubbleService::class.java).apply {
                action = BubbleService.ACTION_STOP_RECORDING
            }
            startService(intent)
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
        }
        return true
    }

    // ------------------------------------------------------------------
    // Universal Press Actions: tap-to-toggle, single vs double press
    // ------------------------------------------------------------------

    private fun handlePressActionEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return
        when (pressState) {
            PressState.RECORDING -> {
                // Any press while recording stops it (hands-free toggle).
                pressState = PressState.IDLE
                handler.removeCallbacksAndMessages(null)
                soundManager.play(SoundEvent.TRIGGER_STOP)
                sendStopRecording()
            }
            PressState.IDLE -> {
                pressState = PressState.WAITING_SECOND
                firstPressTime = System.currentTimeMillis()
                handler.postDelayed({
                    if (pressState == PressState.WAITING_SECOND) {
                        startPressRecording(singlePressActionStr)
                    }
                }, doublePressWindowMs)
            }
            PressState.WAITING_SECOND -> {
                val gap = System.currentTimeMillis() - firstPressTime
                if (gap < doublePressWindowMs) {
                    handler.removeCallbacksAndMessages(null)
                    startPressRecording(doublePressActionStr)
                }
            }
        }
    }

    private fun startPressRecording(actionStr: String) {
        if (useSmartSuppression && shouldSuppressTrigger()) {
            pressState = PressState.IDLE
            return
        }
        pressState = PressState.RECORDING
        if (useHaptics) hapticHelper.vibrateShort()
        soundManager.play(SoundEvent.TRIGGER_START)
        try {
            val intent = android.content.Intent(this, BubbleService::class.java).apply {
                action = BubbleService.ACTION_START_RECORDING
                putExtra(BubbleService.EXTRA_PRESS_ACTION, actionStr)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BubbleService (press action)", e)
            pressState = PressState.IDLE
        }
    }

    private fun sendStopRecording() {
        val intent = android.content.Intent(this, BubbleService::class.java).apply {
            action = BubbleService.ACTION_STOP_RECORDING
        }
        startService(intent)
        serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
    }

    private fun resetToIdle() {
        triggerState = TriggerState.IDLE
        firstPressTime = 0L
        serviceBridge.emit(ServiceBridge.TriggerEvent.Idle)
    }
}
