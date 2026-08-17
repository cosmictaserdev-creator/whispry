// SPDX-License-Identifier: AGPL-3.0-or-later
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
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.domain.repository.TriggerRepository
import com.example.whispry.util.HapticHelper
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
    private var useSmartSuppression = false
    private var consumeVolumeKeys = true
    private var isSinglePressEnabled = false
    private var currentTriggerMode: TriggerMode = TriggerMode.VolumeButton
    private var activeKeyCode: Int = KeyEvent.KEYCODE_VOLUME_DOWN

    // Hands-free trigger (opt-in). Press to start, press again to stop — no holding. Honors the
    // single-vs-double press preference: single mode toggles on one press, double mode on a quick
    // double press (to start AND to stop).
    private var handsFreeEnabled = false
    private var handsFreeArmingDelayMs = DataStoreKeys.DEFAULT_HANDS_FREE_ARMING_DELAY_MS
    // Plain press-and-hold arming delay (hands-free off) — separate slider from the one above.
    private var singlePressHoldDelayMs = DataStoreKeys.DEFAULT_SINGLE_PRESS_HOLD_DELAY_MS
    // Pre-recording arm/idle tracking for hands-free single-press mode only; once a recording
    // starts, pressState (below) is the source of truth, same as every other trigger path.
    private var handsFreeArmingState = HandsFreePressState()

    // Universal Press Actions (opt-in). When enabled, the volume key becomes a tap-to-toggle
    // trigger: single press fires [singlePressAction], double press fires [doublePressAction].
    private var pressActionsEnabled = false
    private var singlePressActionStr = "NORMAL"
    private var doublePressActionStr = "NORMAL"
    // Shared tap-to-toggle state for hands-free and press-actions.
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

        // Snapshot the keyboard state at connect in case it is already up.
        detectIme()

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
                handsFreeArmingDelayMs = prefs[DataStoreKeys.HANDS_FREE_ARMING_DELAY_MS]
                    ?: DataStoreKeys.DEFAULT_HANDS_FREE_ARMING_DELAY_MS
                singlePressHoldDelayMs = prefs[DataStoreKeys.SINGLE_PRESS_HOLD_DELAY_MS]
                    ?: DataStoreKeys.DEFAULT_SINGLE_PRESS_HOLD_DELAY_MS
                val newSinglePress = prefs[DataStoreKeys.SINGLE_PRESS_TRIGGER] ?: DataStoreKeys.DEFAULT_SINGLE_PRESS_TRIGGER
                val newHandsFree = prefs[DataStoreKeys.HANDS_FREE_MODE] ?: false
                val newPressActions = prefs[DataStoreKeys.PRESS_ACTIONS_ENABLED] ?: false

                // Did any flag that changes how a press is interpreted change this emission?
                val triggerBehaviorChanged =
                    newSinglePress != isSinglePressEnabled ||
                    newHandsFree != handsFreeEnabled ||
                    newPressActions != pressActionsEnabled

                isSinglePressEnabled = newSinglePress
                handsFreeEnabled = newHandsFree
                pressActionsEnabled = newPressActions

                // A tap-to-toggle (hands-free / press-action) recording is owned by the mode that
                // started it. If the user changes the trigger behavior mid-recording — e.g. turns
                // hands-free off, or flips single/double — finalize the in-flight recording now.
                // Otherwise it keeps running under the old semantics and the user has to trigger
                // again to stop it (the reported bug). This no longer requires *both* modes to be
                // off; any behavior change that orphans the recording ends it cleanly.
                if (triggerBehaviorChanged && pressState == PressState.RECORDING) {
                    handler.removeCallbacksAndMessages(null)
                    soundManager.play(SoundEvent.TRIGGER_STOP)
                    sendStopRecording()
                    pressState = PressState.IDLE
                } else if (!pressActionsEnabled && !handsFreeEnabled && pressState != PressState.IDLE) {
                    // Both tap-to-toggle modes off: clear any lingering non-recording state.
                    pressState = PressState.IDLE
                }
                if (triggerBehaviorChanged) {
                    // Mid-arm (key held, not yet recording) when the behavior underneath it changes:
                    // drop the pending arming timeout rather than let it fire under stale semantics.
                    handler.removeCallbacksAndMessages(null)
                    handsFreeArmingState = HandsFreePressState()
                }
                singlePressActionStr = prefs[DataStoreKeys.SINGLE_PRESS_ACTION] ?: "NORMAL"
                doublePressActionStr = prefs[DataStoreKeys.DOUBLE_PRESS_ACTION] ?: "NORMAL"
                Log.d(TAG, "settings: consumeVolumeKeys=$consumeVolumeKeys isSinglePressEnabled=$isSinglePressEnabled " +
                        "handsFreeEnabled=$handsFreeEnabled pressActionsEnabled=$pressActionsEnabled")
            }
        }
    }

    private fun updateServiceInfoForMode(mode: TriggerMode) {
        serviceInfo = serviceInfo.apply {
            flags = when (mode) {
                is TriggerMode.VolumeButton ->
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
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                if (!packageName.isNullOrBlank() && packageName != this.packageName) {
                    Log.d(TAG, "Foreground package changed to: $packageName")
                    ServiceLocator.lastForegroundPackage = packageName
                    serviceBridge.setForegroundPackage(packageName)
                }
                // Some OEMs only emit window-state-changed for IME show/hide.
                detectIme()
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> detectIme()
        }
    }

    /**
     * Report the soft keyboard's screen bounds to [serviceBridge]. The IME shows up in [windows]
     * once flagRetrieveInteractiveWindows is set (in accessibility_service_config.xml) and is the
     * only input-method window while the keyboard is up, disappearing when it is dismissed.
     */
    fun detectIme() {
        val ime = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }
        val bounds = android.graphics.Rect()
        if (ime != null) ime.getBoundsInScreen(bounds) else bounds.setEmpty()
        serviceBridge.setImeBounds(if (ime != null) bounds else null)
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
        resetToIdle()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return when (currentTriggerMode) {
            is TriggerMode.VolumeButton -> handleVolumeKeyEvent(event)
            else -> false
        }
    }

    private fun handleVolumeKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != activeKeyCode) return false

        Log.d(TAG, "key ${if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"} " +
                "pressActionsEnabled=$pressActionsEnabled handsFreeEnabled=$handsFreeEnabled " +
                "isSinglePressEnabled=$isSinglePressEnabled consumeVolumeKeys=$consumeVolumeKeys " +
                "triggerState=$triggerState pressState=$pressState")

        // Universal Press Actions take over the volume key entirely when enabled.
        if (pressActionsEnabled) {
            handlePressActionEvent(event)
            return true
        }

        // Hands-free tap-to-toggle (no holding) when enabled.
        if (handsFreeEnabled) {
            return handleHandsFreeEvent(event)
        }

        val consumed = if (isSinglePressEnabled) {
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
                    }, singlePressHoldDelayMs) // Pre-delay to prevent accidental short press
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

    // ------------------------------------------------------------------
    // Hands-free: tap-to-toggle the normal transcribe flow (no holding)
    // ------------------------------------------------------------------

    private fun handleHandsFreeEvent(event: KeyEvent): Boolean {
        if (isSinglePressEnabled) {
            // Already recording: any press stops it immediately (unchanged).
            if (pressState == PressState.RECORDING) {
                if (event.action == KeyEvent.ACTION_DOWN) stopHandsFree()
                return true
            }
            // Not yet recording: press-and-hold must clear the arming delay before it starts,
            // so a quick tap can act as a normal key press instead.
            return handleHandsFreeArmingEvent(event)
        }

        if (event.action != KeyEvent.ACTION_DOWN) return true

        // Double-press to start, double-press to stop.
        when (pressState) {
            PressState.IDLE -> {
                pressState = PressState.WAITING_SECOND
                firstPressTime = System.currentTimeMillis()
                handler.postDelayed({
                    if (pressState == PressState.WAITING_SECOND) pressState = PressState.IDLE
                }, doublePressWindowMs)
            }
            PressState.WAITING_SECOND -> {
                if (System.currentTimeMillis() - firstPressTime < doublePressWindowMs) {
                    handler.removeCallbacksAndMessages(null)
                    startPressRecording("NORMAL")
                }
            }
            PressState.RECORDING -> stopHandsFree()
        }
        return true
    }

    /** Pre-recording arming for hands-free single-press mode: hold past [handsFreeArmingDelayMs]
     *  to start, release earlier to pass through as a normal key press. */
    private fun handleHandsFreeArmingEvent(event: KeyEvent): Boolean {
        val resolverEvent = when (event.action) {
            KeyEvent.ACTION_DOWN -> HandsFreePressEvent.KeyDown
            KeyEvent.ACTION_UP -> HandsFreePressEvent.KeyUp
            else -> return true
        }
        val resolver = HandsFreePressResolver(
            HandsFreePressConfig(armingDelayMs = handsFreeArmingDelayMs, consumeVolumeKeys = consumeVolumeKeys)
        )
        val transition = resolver.reduce(handsFreeArmingState, resolverEvent)
        // pressState (via startPressRecording, checked at the top of handleHandsFreeEvent) is the
        // sole source of truth once recording actually starts — this tracker only ever needs to
        // hold IDLE/ARMING, so it's reset regardless of whether startPressRecording itself
        // actually started a recording or was suppressed (e.g. smart suppression).
        handsFreeArmingState = if (transition.effects.contains(HandsFreePressEffect.StartRecording)) {
            HandsFreePressState()
        } else {
            transition.state
        }
        transition.effects.forEach { effect ->
            when (effect) {
                HandsFreePressEffect.ScheduleArmingTimeout -> {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ onHandsFreeArmingTimeout() }, handsFreeArmingDelayMs)
                }
                HandsFreePressEffect.CancelArmingTimeout -> handler.removeCallbacksAndMessages(null)
                HandsFreePressEffect.StartRecording -> startPressRecording("NORMAL")
            }
        }
        return transition.consumed
    }

    private fun onHandsFreeArmingTimeout() {
        if (handsFreeArmingState.phase != HandsFreePressPhase.ARMING) return
        val resolver = HandsFreePressResolver(
            HandsFreePressConfig(armingDelayMs = handsFreeArmingDelayMs, consumeVolumeKeys = consumeVolumeKeys)
        )
        val transition = resolver.reduce(handsFreeArmingState, HandsFreePressEvent.ArmingTimeout)
        // Same reasoning as handleHandsFreeArmingEvent: pressState takes over once recording
        // actually starts (or fails to, if suppressed), so this tracker resets to IDLE either way.
        handsFreeArmingState = HandsFreePressState()
        transition.effects.forEach { effect ->
            if (effect == HandsFreePressEffect.StartRecording) startPressRecording("NORMAL")
        }
    }

    private fun stopHandsFree() {
        pressState = PressState.IDLE
        handler.removeCallbacksAndMessages(null)
        soundManager.play(SoundEvent.TRIGGER_STOP)
        sendStopRecording()
    }

    private fun resetToIdle() {
        triggerState = TriggerState.IDLE
        firstPressTime = 0L
        serviceBridge.emit(ServiceBridge.TriggerEvent.Idle)
    }

    // ------------------------------------------------------------------
    // Voice command "calculate": type a tokenized expression into whichever
    // calculator app VoiceCommandExecutor just launched, then press "=".
    // Best-effort — button labels differ across calculator apps/OEMs, so a
    // missing node just stops the automation (the expression is already on
    // the clipboard as a manual-paste fallback).
    // ponytail: tested against Google/Samsung calculators; unknown third-party
    // calculators may not match — add a per-package view-id map if reported broken.
    // ------------------------------------------------------------------

    fun performCalculatorInput(tokens: List<String>, calculatorPackages: Set<String>) {
        serviceScope.launch {
            if (!waitForForegroundPackage(calculatorPackages)) return@launch
            delay(300) // let the calculator's UI finish laying out after the window switch
            for (token in tokens) {
                val node = findClickableNode(rootInActiveWindow, aliasesForCalcToken(token)) ?: return@launch
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                delay(120)
            }
        }
    }

    private suspend fun waitForForegroundPackage(packages: Set<String>, timeoutMs: Long = 1500L): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (ServiceLocator.lastForegroundPackage in packages) return true
            delay(100)
        }
        return false
    }

    private fun aliasesForCalcToken(token: String): List<String> = when (token) {
        "+" -> listOf("+", "plus", "add")
        "-" -> listOf("-", "−", "minus", "subtract")
        "×" -> listOf("×", "*", "x", "multiply", "times")
        "÷" -> listOf("÷", "/", "divide")
        "=" -> listOf("=", "equals")
        "." -> listOf(".", "point", "decimal")
        else -> listOf(token)
    }

    private fun findClickableNode(root: AccessibilityNodeInfo?, aliases: List<String>): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (node.isClickable && aliases.any { it.equals(text, ignoreCase = true) || it.equals(desc, ignoreCase = true) }) {
                return node
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }
}
