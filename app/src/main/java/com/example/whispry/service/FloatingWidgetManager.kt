// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.resolveAccentColors
import com.example.whispry.util.HapticHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The floating "physical switch" widget: a contentless, accent-colored launcher that
 * hold-to-talks into the existing BubbleService recording flow. Purely a trigger — all
 * recording visuals stay on the recording pill, which now coexists with this window.
 */
@Singleton
class FloatingWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val serviceBridge: ServiceBridge,
    private val overlayCoordinator: WindowOverlayCoordinator,
    private val hapticHelper: HapticHelper,
    private val soundManager: SoundManager
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val TAG = "Whispry_WidgetManager"

    /** Invisible slack (dp) around the visible shape so the touch target stays generous. */
    private val touchSlackDp = 32

    /** The window is never narrower than this, no matter how slim the visible shape is. */
    private val minTouchWidthDp = 56

    /**
     * Collapsed RAMP sliver: a slim edge strip, same height as the revealed widget (generous
     * vertical reach for the swipe) but a much narrower window — this is what actually solves
     * the touch-target/interference tradeoff, not just a smaller-looking visual. Sized to match
     * a Samsung Edge Panel handle (~24dp) rather than the previous 18dp, which was thin enough
     * to miss on a real thumb.
     */
    private val sliverWidthDp = 16
    private val sliverTouchSlackDp = 8

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val positionManager by lazy {
        BubblePositionManager(context.resources.displayMetrics.density)
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private var config = WidgetConfig()
    private val configState = mutableStateOf(WidgetConfig())

    private var resolver = WidgetGestureResolver(gestureConfig(config))
    private var gestureState = WidgetGestureState()

    private val phaseState = mutableStateOf(WidgetGesturePhase.IDLE)
    private val sessionActiveState = mutableStateOf(false)
    private val cancelArmedState = mutableStateOf(false)
    private val dragYState = mutableStateOf(0f)
    private val editModeState = mutableStateOf(false)
    private val editDraggingState = mutableStateOf(false)
    private val edgeState = mutableStateOf(WidgetEdge.Right)

    private var armingJob: Job? = null
    private var tapTimeoutJob: Job? = null
    private var idleJob: Job? = null
    private var depressTickJob: Job? = null
    private var collapseResizeJob: Job? = null
    private var keyboardNudgeJob: Job? = null

    /** True while the widget is parked above the keys (nudge in effect). The slide animation's
     *  job is null once it finishes, so this flag is what actually drives the slide-back. */
    private var keyboardNudged = false

    /** Last seen IME top edge — detects the keyboard's closing animation (top edge receding)
     *  so the nudge never re-parks the widget back up while the keyboard is dismissing. */
    private var lastImeTop: Int? = null

    private companion object {
        /** How long a press must survive before it haptically/visually commits. */
        const val DEPRESS_COMMIT_MS = 70L

        /** Gap (dp) between the nudged widget and the keyboard's top edge. */
        const val KEYBOARD_NUDGE_MARGIN_DP = 8

        /** Covers both the sliver-collapse scale spring and its alpha tween (500ms, see
         *  WidgetSwitchVisual) — the window itself must not clip down to the sliver's narrow
         *  width until the visual shrink has actually finished animating into it. */
        const val SLIVER_COLLAPSE_ANIM_MS = 520L

        /** Fixed width for the edit-mode control card's own overlay window (see showControlCard). */
        const val CONTROL_CARD_WIDTH_DP = 320
    }

    /** True while a session started by a tap (toggle) is live — the next tap stops it. */
    private var toggleSession = false

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        migrateLegacyTriggerMode()
        observeEnabled()
        observeConfig()
        observeSession()
        observeImeForKeyboardNudge()
    }

    /**
     * TriggerMode.FloatingWidget is retired: the widget is now an always-available surface
     * governed only by its enable toggle. Users who had it as their trigger mode get the
     * widget enabled with the global trigger falling back to Manual.
     */
    private fun migrateLegacyTriggerMode() {
        managerScope.launch {
            val prefs = settingsProvider.dataStore.data.first()
            if (prefs[DataStoreKeys.WIDGET_TRIGGER_MODE_MIGRATED] == true) return@launch
            settingsProvider.dataStore.edit { p ->
                if (p[DataStoreKeys.TRIGGER_MODE] == "floating_widget") {
                    p[DataStoreKeys.FLOATING_WIDGET_ENABLED] = true
                    p[DataStoreKeys.TRIGGER_MODE] = "manual"
                }
                p[DataStoreKeys.WIDGET_TRIGGER_MODE_MIGRATED] = true
            }
        }
    }

    private fun gestureConfig(cfg: WidgetConfig): WidgetGestureConfig {
        val density = context.resources.displayMetrics.density
        return WidgetGestureConfig(
            armingDelayMs = cfg.armingDelayMs,
            cancelThresholdPx = 56f * density,
            touchSlopPx = 14f * density,
            doubleTapWindowMs = 300L,
            doubleTapEnabled = cfg.doubleTapAction != WidgetTapAction.None,
            slimSliverEnabled = true,
            // A shorter reveal swipe than the original 48dp - still well clear of touchSlopPx
            // (14dp) so it can't be mistaken for an accidental brush, but a quick flick reveals
            // the widget instead of demanding a deliberate long drag.
            revealThresholdPx = 36f * density
        )
    }

    // ------------------------------------------------------------------
    // Observers
    // ------------------------------------------------------------------

    private fun observeEnabled() {
        managerScope.launch {
            combine(overlayCoordinator.widgetEnabled, overlayCoordinator.widgetsHidden) { enabled, hidden ->
                enabled && !hidden && android.provider.Settings.canDrawOverlays(context)
            }.collect { shouldShow ->
                if (shouldShow) show() else hide()
            }
        }
    }

    private fun observeConfig() {
        managerScope.launch {
            settingsProvider.dataStore.data
                .map { WidgetConfig.fromPreferences(it) }
                .collect { cfg ->
                    val sizeChanged = cfg.visualSizeDp() != config.visualSizeDp()
                    val clearanceChanged = cfg.edgeClearanceDp != config.edgeClearanceDp
                    config = cfg
                    configState.value = cfg
                    resolver = WidgetGestureResolver(gestureConfig(cfg))
                    if ((sizeChanged || clearanceChanged) && composeView != null) applyWindowSizeAndPosition()
                    restartIdleTimer()
                }
        }
    }

    private fun observeSession() {
        managerScope.launch {
            overlayCoordinator.bubbleVisible.collect { visible ->
                if (visible) {
                    onGestureEvent(WidgetGestureEvent.SessionStarted)
                } else {
                    toggleSession = false
                    onGestureEvent(WidgetGestureEvent.SessionEnded)
                    restartIdleTimer()
                }
            }
        }
    }

    /**
     * Keyboard-overlap nudge: while the IME is up and the widget's resting spot would sit under
     * the keys, slide it up to just above the keyboard's top edge (same X), then back when the
     * IME closes. The saved position is never touched — the nudge is a transient animation only.
     */
    private fun observeImeForKeyboardNudge() {
        managerScope.launch {
            serviceBridge.imeBounds.collect { ime ->
                val view = composeView ?: return@collect
                val lp = layoutParams ?: return@collect
                if (config.avoidKeyboard && !editModeState.value && ime != null && !ime.isEmpty) {
                    // Act only while the keyboard is opening (top edge rising) — never during its
                    // close animation, whose coarse/stale rects would re-nudge the widget back up
                    // right after it slid home.
                    val growing = lastImeTop == null || ime.top < lastImeTop!!
                    lastImeTop = ime.top
                    if (growing && lp.y + lp.height > ime.top) {
                        val density = context.resources.displayMetrics.density
                        val marginPx = (KEYBOARD_NUDGE_MARGIN_DP * density).toInt()
                        val targetY = (ime.top - lp.height - marginPx)
                            .coerceAtLeast(getSafeBounds().top)
                        if (targetY != lp.y) {
                            keyboardNudged = true
                            slideWidgetTo(targetY)
                        }
                    }
                    return@collect
                }
                lastImeTop = null
                // Keyboard down / nudge disabled / mid-edit: never nudge, but slide back to the
                // resting spot if the widget is parked above the keys.
                if (keyboardNudged && !editModeState.value) {
                    keyboardNudged = false
                    slideWidgetTo(restingY(lp))
                }
            }
        }
    }

    /** The widget's saved resting Y (denormalized), used to slide back after a keyboard nudge.
     *  Never-positioned widgets fall back to the default edge spot — returning [lp.y] (the nudged
     *  Y) here made the slide-back a no-op, leaving the widget parked above the keys. */
    private suspend fun restingY(lp: WindowManager.LayoutParams): Int {
        val bounds = getSafeBounds().toBubbleBounds()
        val prefs = settingsProvider.dataStore.data.first()
        val savedY = prefs[DataStoreKeys.WIDGET_POSITION_Y]
        val savedX = prefs[DataStoreKeys.WIDGET_POSITION_X]
        val (rawX, rawY) = if (savedX != null && savedY != null) {
            positionManager.denormalize(savedX.toFloat(), savedY.toFloat(), bounds)
        } else {
            positionManager.widgetDefaultEdgePosition(lp.width.toFloat(), lp.height.toFloat(), bounds)
        }
        val (_, y) = snapFor(rawX, rawY, lp.width.toFloat(), lp.height.toFloat(), bounds)
        return y.roundToInt()
    }

    /**
     * Y that parks the widget just above the keys when its resting spot overlaps the IME (a
     * keyboard nudge in flight), else null. Shared by the IME observer and the reposition paths
     * so reveal/collapse/config changes keep the widget above the keyboard while it's open.
     */
    private fun keyboardNudgeTargetY(restTop: Int, winH: Int): Int? {
        val ime = serviceBridge.imeBounds.value ?: return null
        if (ime.isEmpty) return null
        if (!config.avoidKeyboard || editModeState.value) return null
        if (restTop + winH <= ime.top) return null
        val marginPx = (KEYBOARD_NUDGE_MARGIN_DP * context.resources.displayMetrics.density).toInt()
        return (ime.top - winH - marginPx).coerceAtLeast(getSafeBounds().top)
    }

    /** Animate the widget window to [targetY], canceling any prior nudge/slide. */
    private fun slideWidgetTo(targetY: Int) {        val view = composeView ?: return
        val lp = layoutParams ?: return
        if (!view.isAttachedToWindow) return
        keyboardNudgeJob?.cancel()
        val startY = lp.y
        if (startY == targetY) return
        keyboardNudgeJob = managerScope.launch {
            val durationMs = 160L
            val start = SystemClock.uptimeMillis()
            while (true) {
                val t = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
                val eased = 1f - (1f - t) * (1f - t)
                lp.y = (startY + (targetY - startY) * eased).roundToInt()
                try {
                    if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
                } catch (e: Exception) {
                    break
                }
                if (t >= 1f) break
                delay(16)
            }
            if (keyboardNudgeJob == coroutineContext[Job]) keyboardNudgeJob = null
        }
    }

    // ------------------------------------------------------------------
    // Gesture reduction + effects
    // ------------------------------------------------------------------

    private fun onPointerDown(timeMs: Long) {
        // A live toggle-started session: this press stops it instead of arming a new one. Works
        // from the collapsed sliver too — a toggle-started recording must always be stoppable
        // without first requiring a swipe-to-reveal.
        if (toggleSession && gestureState.sessionActive &&
            (gestureState.phase == WidgetGesturePhase.IDLE || gestureState.phase == WidgetGesturePhase.SLIVER)
        ) {
            hapticHelper.vibrateTick()
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
            gestureState = gestureState.copy(phase = WidgetGesturePhase.CONSUMING)
            // Mirror onGestureEvent's bookkeeping since this transition bypasses resolver.reduce()
            // — otherwise a pending arming/tap-timeout job fires later against stale state, and
            // the derived state flows drift from gestureState.
            phaseState.value = gestureState.phase
            sessionActiveState.value = gestureState.sessionActive
            cancelArmedState.value = gestureState.cancelArmed
            dragYState.value = gestureState.dragY
            armingJob?.cancel()
            tapTimeoutJob?.cancel()
            return
        }
        onGestureEvent(WidgetGestureEvent.PointerDown(timeMs))
    }

    private fun onGestureEvent(event: WidgetGestureEvent) {
        val transition = resolver.reduce(gestureState, event)
        gestureState = transition.state
        phaseState.value = transition.state.phase
        sessionActiveState.value = transition.state.sessionActive
        cancelArmedState.value = transition.state.cancelArmed
        dragYState.value = transition.state.dragY
        if (transition.state.phase != WidgetGesturePhase.ARMING) armingJob?.cancel()
        if (transition.state.phase != WidgetGesturePhase.AWAITING_SECOND_TAP) tapTimeoutJob?.cancel()
        transition.effects.forEach { applyEffect(it) }
    }

    private fun applyEffect(effect: WidgetGestureEffect) {
        when (effect) {
            WidgetGestureEffect.Depress -> {
                idleJob?.cancel()
                // Delay the tick past the graze window so back-gesture swipes don't buzz.
                depressTickJob?.cancel()
                depressTickJob = managerScope.launch {
                    delay(DEPRESS_COMMIT_MS)
                    if (gestureState.phase == WidgetGesturePhase.ARMING) hapticHelper.vibrateTick()
                }
            }
            WidgetGestureEffect.Release -> {
                depressTickJob?.cancel()
                restartIdleTimer()
            }
            WidgetGestureEffect.ScheduleArming -> {
                armingJob?.cancel()
                armingJob = managerScope.launch {
                    delay(config.armingDelayMs)
                    onGestureEvent(WidgetGestureEvent.ArmingTimeout(SystemClock.uptimeMillis()))
                }
            }
            WidgetGestureEffect.ScheduleTapTimeout -> {
                tapTimeoutJob?.cancel()
                tapTimeoutJob = managerScope.launch {
                    delay(gestureConfig(config).doubleTapWindowMs)
                    onGestureEvent(WidgetGestureEvent.TapTimeout(SystemClock.uptimeMillis()))
                }
            }
            WidgetGestureEffect.StartRecording -> {
                hapticHelper.vibrateClick()
                if (!config.soundMuted) soundManager.play(SoundEvent.TRIGGER_START)
                serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
            }
            WidgetGestureEffect.SendRecording -> {
                hapticHelper.vibrateTick()
                serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
            }
            WidgetGestureEffect.DiscardRecording -> {
                hapticHelper.vibrateShort()
                serviceBridge.emit(ServiceBridge.TriggerEvent.CancelArming(false))
                serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingCancelled)
            }
            is WidgetGestureEffect.CancelArmChanged -> {
                if (effect.armed) hapticHelper.vibrateWarn()
                serviceBridge.emit(ServiceBridge.TriggerEvent.CancelArming(effect.armed))
            }
            WidgetGestureEffect.SingleTap -> performTap(config.singleTapAction)
            WidgetGestureEffect.DoubleTap -> performTap(config.doubleTapAction)
            WidgetGestureEffect.RevealWidget -> {
                collapseResizeJob?.cancel()
                applyWindowSizeAndPosition()
                restartIdleTimer()
            }
            WidgetGestureEffect.CollapseWidget -> {
                // Let the visual shrink animation finish before the window clips down to the
                // sliver's narrow touch target — resizing immediately cut the still-large
                // scaling content off mid-animation, which read as a bad, jarring pop.
                collapseResizeJob?.cancel()
                collapseResizeJob = managerScope.launch {
                    delay(SLIVER_COLLAPSE_ANIM_MS)
                    applyWindowSizeAndPositionInternal(addView = false)
                }
            }
            WidgetGestureEffect.CancelArmingTimer -> armingJob?.cancel()
        }
    }

    private fun performTap(action: WidgetTapAction) {
        when (action) {
            WidgetTapAction.None -> Unit
            is WidgetTapAction.ToggleRecord -> {
                toggleSession = true
                if (!config.soundMuted) soundManager.play(SoundEvent.TRIGGER_START)
                if (action.pressAction == "NORMAL") {
                    serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
                } else {
                    val intent = Intent(context, BubbleService::class.java).apply {
                        this.action = BubbleService.ACTION_START_RECORDING
                        putExtra(BubbleService.EXTRA_PRESS_ACTION, action.pressAction)
                    }
                    try {
                        context.startForegroundService(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start recording from tap", e)
                        toggleSession = false
                    }
                }
            }
        }
    }

    private fun restartIdleTimer() {
        idleJob?.cancel()
        idleJob = managerScope.launch {
            delay(config.fadeDelayMs)
            if (gestureState.phase == WidgetGesturePhase.IDLE &&
                !gestureState.sessionActive && !editModeState.value
            ) {
                // Idle state IS the collapsed sliver now — not a shrink-in-place.
                onGestureEvent(WidgetGestureEvent.CollapseToSliver)
            }
        }
    }

    // ------------------------------------------------------------------
    // Window management
    // ------------------------------------------------------------------

    fun show() {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@FloatingWidgetManager)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetManager)
            setContent { WidgetRoot() }
        }

        layoutParams = WindowManager.LayoutParams(
            0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        managerScope.launch {
            applyWindowSizeAndPositionInternal(addView = true)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            restartIdleTimer()
        }
    }

    fun hide() {
        exitEditMode(save = false)
        val view = composeView
        if (view != null) {
            // Yanking the view off with removeView() mid-frame reads as the widget just
            // vanishing — fade it out first so disabling the widget looks deliberate.
            view.animate()
                .alpha(0f)
                .setDuration(160L)
                .withEndAction {
                    try {
                        windowManager.removeView(view)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing widget", e)
                    }
                }
                .start()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        composeView = null
        layoutParams = null
        armingJob?.cancel()
        tapTimeoutJob?.cancel()
        idleJob?.cancel()
        collapseResizeJob?.cancel()
        gestureState = WidgetGestureState(sessionActive = gestureState.sessionActive)
        phaseState.value = WidgetGesturePhase.IDLE
    }

    /** Re-snap to the equivalent normalized spot after rotation (called by BubbleService). */
    fun onOrientationChanged() {
        if (composeView == null) return
        managerScope.launch { applyWindowSizeAndPositionInternal(addView = false) }
    }

    private fun applyWindowSizeAndPosition() {
        managerScope.launch { applyWindowSizeAndPositionInternal(addView = false) }
    }

    private suspend fun applyWindowSizeAndPositionInternal(addView: Boolean) {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        val density = context.resources.displayMetrics.density
        val isSliver = gestureState.phase == WidgetGesturePhase.SLIVER
        val (visualW, visualH) = if (isSliver) {
            sliverWidthDp.toFloat() to config.visualSizeDp().second
        } else {
            config.visualSizeDp()
        }
        val slack = if (isSliver) sliverTouchSlackDp else touchSlackDp
        // The window never resizes for ARMING/RECORDING — only for sliver vs full — so it must
        // already contain the shape at its largest transient scale (the recording pop), or that
        // pop clips against the window's own bounds. Sliver never reaches that scale (recording
        // can only start from the fully revealed state), so only the non-sliver case needs it.
        val displayW = if (isSliver) visualW else visualW * WIDGET_MAX_TRIGGER_SCALE
        val displayH = visualH * WIDGET_MAX_TRIGGER_SCALE
        // The clearance folds into the window as inner slack: the wedge is anchored to the
        // window's outer edge, so the widget stays connected to the screen edge (idle sliver
        // and revealed alike) while the touch target still clears the OS back-gesture zone.
        val winW = ((displayW + slack + config.edgeClearanceDp) * density).roundToInt()
            .coerceAtLeast(((if (isSliver) sliverWidthDp else minTouchWidthDp) * density).roundToInt())
        val winH = ((displayH + slack * 2) * density).roundToInt()
        lp.width = winW
        lp.height = winH

        val bounds = getSafeBounds().toBubbleBounds()

        if (editModeState.value && !addView) {
            // Mid-edit resizes (the control card sliders) must not yank the widget back to
            // its saved spot — resize in place and let the user's drag stand.
            val (x, y) = snapFor(lp.x.toFloat(), lp.y.toFloat(), winW.toFloat(), winH.toFloat(), bounds)
            lp.x = x.roundToInt()
            lp.y = y.roundToInt()
        } else {
            val prefs = settingsProvider.dataStore.data.first()
            val savedX = prefs[DataStoreKeys.WIDGET_POSITION_X]
            val savedY = prefs[DataStoreKeys.WIDGET_POSITION_Y]

            val (rawX, rawY) = if (savedX != null && savedY != null) {
                positionManager.denormalize(savedX.toFloat(), savedY.toFloat(), bounds)
            } else {
                positionManager.widgetDefaultEdgePosition(winW.toFloat(), winH.toFloat(), bounds)
            }
            val edge = positionManager.widgetEdgeTarget(rawX, winW.toFloat(), bounds)
            edgeState.value = edge
            val restY = rawY.coerceIn(
                bounds.top.toFloat(),
                (bounds.bottom - winH).coerceAtLeast(bounds.top).toFloat()
            )
            // Wedge hugs the physical screen edge — flush window, no visible gap.
            lp.x = positionManager.widgetEdgePosition(
                edge, restY, winW.toFloat(), winH.toFloat(), bounds
            ).first.roundToInt()
            // Repositions (reveal, collapse, config changes) must not drop the widget back under
            // the keyboard while it's open — stay at the nudge target if one is active.
            val nudgeY = keyboardNudgeTargetY(restY.roundToInt(), winH)
            keyboardNudged = nudgeY != null
            lp.y = nudgeY ?: restY.roundToInt()
        }

        try {
            if (addView) {
                com.example.whispry.util.retryOnce(
                    onFirstFailure = { e -> Log.e(TAG, "Error adding widget view, retrying once", e) }
                ) {
                    windowManager.addView(view, lp)
                }
            } else if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, lp)
            }
            updateGestureExclusion(view, winW, winH)
        } catch (e: Exception) {
            Log.e(TAG, "Error laying out widget", e)
            if (addView) composeView = null
        }
    }

    /**
     * The widget sits flush against the true screen edge, which is exactly where Android's
     * back-gesture nav intercepts touches before they ever reach this overlay window — without
     * this, a press on the sliver is indistinguishable from a back swipe and the OS wins. This is
     * the same API Samsung's own Edge Panel handle relies on to stay swipeable.
     */
    private fun updateGestureExclusion(view: ComposeView, widthPx: Int, heightPx: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            view.systemGestureExclusionRects = listOf(android.graphics.Rect(0, 0, widthPx, heightPx))
        }
    }

    /** Snap to the nearest edge and record which one we ended up on (drives mirroring). */
    private fun snapFor(
        x: Float, y: Float, w: Float, h: Float, bounds: BubbleBounds, clearance: Float = 0f
    ): Pair<Float, Float> {
        val edge = positionManager.widgetEdgeTarget(x, w, bounds)
        edgeState.value = edge
        return positionManager.widgetEdgePosition(edge, y, w, h, bounds, clearance)
    }

    private fun getSafeBounds(): android.graphics.Rect {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()
            )
            android.graphics.Rect(
                bounds.left + insets.left,
                bounds.top + insets.top,
                bounds.right - insets.right,
                bounds.bottom - insets.bottom
            )
        } else {
            val dm = context.resources.displayMetrics
            android.graphics.Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }
    }

    private fun android.graphics.Rect.toBubbleBounds() = BubbleBounds(left, top, right, bottom)

    // ------------------------------------------------------------------
    // Edit mode
    // ------------------------------------------------------------------

    fun enterEditMode() {
        if (composeView == null) return
        idleJob?.cancel()
        editModeState.value = true
        // The idle resting phase for RAMP is a collapsed sliver window a few dp wide - if edit
        // mode is entered from there, the drag gesture (bound to the window's own bounds) would
        // be confined to that sliver and effectively ungrabbable. Force full size before dragging.
        if (gestureState.phase == WidgetGesturePhase.SLIVER) {
            gestureState = gestureState.copy(phase = WidgetGesturePhase.IDLE)
            phaseState.value = WidgetGesturePhase.IDLE
        }
        applyWindowSizeAndPosition()
        showControlCard()
    }

    fun exitEditMode(save: Boolean = true) {
        if (!editModeState.value) return
        editModeState.value = false
        hideControlCard()
        if (save) saveCurrentPosition()
        restartIdleTimer()
    }

    fun isInEditMode(): Boolean = editModeState.value

    private fun onEditDrag(dx: Float, dy: Float) {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        lp.x += dx.roundToInt()
        lp.y += dy.roundToInt()
        try {
            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error during edit drag", e)
        }
    }

    private fun onEditDragEnd() {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        val bounds = getSafeBounds().toBubbleBounds()
        val (x, y) = snapFor(
            lp.x.toFloat(), lp.y.toFloat(),
            lp.width.toFloat(), lp.height.toFloat(), bounds
        )
        lp.x = x.roundToInt()
        lp.y = y.roundToInt()
        try {
            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error snapping in edit mode", e)
        }
        saveCurrentPosition()
    }

    private var controlCardView: ComposeView? = null
    private var controlCardLayoutParams: WindowManager.LayoutParams? = null

    /** Floating control card shown during edit mode: live size sliders + Done. */
    private fun showControlCard() {
        if (controlCardView != null) return
        controlCardView = ComposeView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent {
                val cfg by configState
                val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                val accentColors = remember(accentName) { resolveAccentColors(accentName) }
                WhispryTheme(accentColors = accentColors) {
                    WidgetEditControlCard(
                        config = cfg,
                        onBaseHeight = { setIntPref(DataStoreKeys.WIDGET_BASE_HEIGHT_DP, it) },
                        onProtrusion = { setIntPref(DataStoreKeys.WIDGET_PROTRUSION_DP, it) },
                        onEdgeClearance = { setIntPref(DataStoreKeys.WIDGET_EDGE_CLEARANCE, it) },
                        onDrag = { dx, dy -> onCardDrag(dx, dy) },
                        onDone = { exitEditMode(save = true) }
                    )
                }
            }
            setViewTreeLifecycleOwner(this@FloatingWidgetManager)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetManager)
        }
        // A fixed dp width, not MATCH_PARENT: a full-width window wouldn't actually move when
        // dragged horizontally (it'd still span edge-to-edge), and would keep blocking touches
        // across the whole row even once the visible card moved off to one side. Fixed (not
        // WRAP_CONTENT) so the card's own fillMaxWidth() below has an unambiguous width to fill,
        // same as it always did.
        val cardWidthPx = (CONTROL_CARD_WIDTH_DP * context.resources.displayMetrics.density).roundToInt()
        val lp = WindowManager.LayoutParams(
            cardWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (48 * context.resources.displayMetrics.density).roundToInt()
        }
        controlCardLayoutParams = lp
        try {
            windowManager.addView(controlCardView, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit control card", e)
            controlCardView = null
            controlCardLayoutParams = null
        }
    }

    private fun hideControlCard() {
        controlCardView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding edit control card", e)
            }
        }
        controlCardView = null
        controlCardLayoutParams = null
    }

    /**
     * Drags the control card itself — it starts gravity-anchored (bottom-center), which would
     * otherwise sit on top of the widget if the widget's saved spot is also near the bottom,
     * blocking the very thing edit mode is meant to let you drag. First call converts it from
     * gravity offsets to absolute coordinates (seeded from its current on-screen location) so it
     * can then be moved freely like the widget itself; the card is recreated gravity-anchored
     * next time edit mode opens, so this is never persisted.
     */
    private fun onCardDrag(dx: Float, dy: Float) {
        val lp = controlCardLayoutParams ?: return
        val view = controlCardView ?: return
        if (lp.gravity != (Gravity.TOP or Gravity.START)) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = location[0]
            lp.y = location[1]
        }
        lp.x += dx.roundToInt()
        lp.y += dy.roundToInt()
        try {
            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error dragging control card", e)
        }
    }

    private fun setIntPref(key: androidx.datastore.preferences.core.Preferences.Key<Int>, value: Int) {
        managerScope.launch {
            settingsProvider.dataStore.edit { it[key] = value }
        }
    }

    private fun saveCurrentPosition() {
        val lp = layoutParams ?: return
        val bounds = getSafeBounds().toBubbleBounds()
        val (nx, ny) = positionManager.normalize(lp.x.toFloat(), lp.y.toFloat(), bounds)
        managerScope.launch {
            settingsProvider.dataStore.edit { prefs ->
                prefs[DataStoreKeys.WIDGET_POSITION_X] = nx.roundToInt()
                prefs[DataStoreKeys.WIDGET_POSITION_Y] = ny.roundToInt()
            }
        }
    }

    // ------------------------------------------------------------------
    // Compose root
    // ------------------------------------------------------------------

    @Composable
    private fun WidgetRoot() {
        val cfg by configState
        val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
        val accentColors = remember(accentName) { resolveAccentColors(accentName) }
        val editMode by editModeState
        val a11yManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }
        val a11yMode = a11yManager.isTouchExplorationEnabled
        val reducedMotion = remember(cfg.motion) {
            when (cfg.motion) {
                WidgetMotionSetting.ON -> false
                WidgetMotionSetting.OFF -> true
                WidgetMotionSetting.AUTO -> android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
                ) == 0f
            }
        }

        WhispryTheme(accentColors = accentColors) {
            val gestureModifier = when {
                editMode -> Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, amount ->
                            change.consume()
                            onEditDrag(amount.x, amount.y)
                        },
                        onDragEnd = { onEditDragEnd() }
                    )
                }
                a11yMode -> Modifier
                    .semantics {
                        role = Role.Switch
                        contentDescription =
                            "Whispry voice switch. Double-tap to start or stop recording."
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Toggle-to-record: screen readers intercept hold and drag gestures.
                        if (gestureState.sessionActive) {
                            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
                        } else {
                            toggleSession = true
                            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
                        }
                    }
                else -> Modifier.pointerInput(cfg.armingDelayMs, cfg.doubleTapAction) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        onPointerDown(SystemClock.uptimeMillis())
                        var sawUp = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    sawUp = true
                                    onGestureEvent(
                                        WidgetGestureEvent.PointerUp(SystemClock.uptimeMillis())
                                    )
                                    break
                                }
                                change.consume()
                                val delta = change.position - down.position
                                // Normalize horizontal drag to "inward positive" regardless of which
                                // edge the widget is anchored on, so the resolver's reveal/collapse
                                // logic stays a single direction-agnostic sign convention: swiping
                                // from the edge toward screen center is always positive dx.
                                val inwardDx = if (edgeState.value == WidgetEdge.Right) -delta.x else delta.x
                                onGestureEvent(
                                    WidgetGestureEvent.PointerMove(
                                        SystemClock.uptimeMillis(), inwardDx, delta.y
                                    )
                                )
                            }
                        } finally {
                            // The system stole the stream (back gesture, window change):
                            // the press must not resolve as a tap or a send.
                            if (!sawUp) onGestureEvent(WidgetGestureEvent.PointerCancel)
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
                WidgetSwitchVisual(
                    config = cfg,
                    phase = phaseState.value,
                    sessionActive = sessionActiveState.value,
                    cancelArmed = cancelArmedState.value,
                    dragYPx = dragYState.value,
                    cancelThresholdPx = gestureConfig(cfg).cancelThresholdPx,
                    editMode = editMode,
                    edge = edgeState.value,
                    reducedMotion = reducedMotion
                )
            }
        }
    }
}
