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

    // cached settings
    private var doublePressWindowMs = 400L
    private var useHaptics = true
    private var useSmartSuppression = true

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
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        this.serviceInfo = info
        Log.d(TAG, "ServiceInfo flags set: ${info.flags}")

        android.widget.Toast.makeText(this, "Whispry Service Connected", android.widget.Toast.LENGTH_SHORT).show()
        ServiceLocator.triggerService = this  // register
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        ServiceLocator.triggerService = null  // unregister
        handler.removeCallbacksAndMessages(null)
        resetToIdle()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        resetToIdle()
    }


    // ------------------------------------------------------------------
    // Key event handling — the state machine
    // ------------------------------------------------------------------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyEvent: keyCode=${event.keyCode}, action=${event.action}")

        // only care about volume down
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        // ignore held-key repeats — only react to first signal of each press
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount != 0) {
            return triggerState == TriggerState.RECORDING
        }

        return when (triggerState) {
            TriggerState.IDLE -> handleIdleState(event)
            TriggerState.FIRST_PRESS_DETECTED -> handleFirstPressState(event)
            TriggerState.RECORDING -> handleRecordingState(event)
        }
    }

    private fun handleIdleState(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        Log.d(TAG, "First press detected")
        firstPressTime = System.currentTimeMillis()
        triggerState = TriggerState.FIRST_PRESS_DETECTED

        handler.postDelayed({
            if (triggerState == TriggerState.FIRST_PRESS_DETECTED) {
                Log.d(TAG, "First press timeout")
                resetToIdle()
            }
        }, doublePressWindowMs)

        return false
    }

    private fun handleFirstPressState(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val gap = System.currentTimeMillis() - firstPressTime
        Log.d(TAG, "Second press detected, gap=$gap")

        return if (gap < doublePressWindowMs) {
            handler.removeCallbacksAndMessages(null)
            
            // NEW — suppress if audio context is wrong
            if (useSmartSuppression && shouldSuppressTrigger()) {
                Log.d(TAG, "Trigger suppressed due to audio/call activity")
                resetToIdle()
                return false // pass through — volume changes normally
            }

            Log.d(TAG, "Valid double press! Emitting RecordingStarted")
            triggerState = TriggerState.RECORDING
            if (useHaptics) hapticHelper.vibrateShort()
            
            // Ensure BubbleService is running
            val intent = android.content.Intent(this, BubbleService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
            true
        } else {
            Log.d(TAG, "Gap too large, treating as new first press")
            firstPressTime = System.currentTimeMillis()
            handler.postDelayed({
                if (triggerState == TriggerState.FIRST_PRESS_DETECTED) {
                    resetToIdle()
                }
            }, doublePressWindowMs)
            false
        }
    }

    private fun shouldSuppressTrigger(): Boolean {
        // Primary check — is music actively playing?
        if (audioManager.isMusicActive) return true
        
        // Secondary check — does any app hold audio focus?
        // (covers games, navigation, podcasts, video)
        var focusHeld = false
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .setOnAudioFocusChangeListener { }
            .build()
            
        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            focusHeld = true
        } else {
            // we got focus — means nothing was holding it — immediately release
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
        if (focusHeld) return true
        
        // Check phone call state via TelephonyManager
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) return true
        
        return false
    }

    private fun handleRecordingState(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            Log.d(TAG, "Key UP detected while recording, emitting RecordingStopped")
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
