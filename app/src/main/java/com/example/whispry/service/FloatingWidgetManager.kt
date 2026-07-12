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
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.util.HapticHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
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
    private val idleFadedState = mutableStateOf(false)
    private val editModeState = mutableStateOf(false)
    private val editDraggingState = mutableStateOf(false)
    private val edgeState = mutableStateOf(WidgetEdge.Right)
    private val cornerState = mutableStateOf(WidgetCorner.BottomRight)

    private var armingJob: Job? = null
    private var tapTimeoutJob: Job? = null
    private var idleJob: Job? = null
    private var depressTickJob: Job? = null

    private companion object {
        /** How long a press must survive before it haptically/visually commits. */
        const val DEPRESS_COMMIT_MS = 70L
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
            // Edge-panel-style collapsed sliver only makes sense for the edge-anchored wedge;
            // CORNER keeps its original always-visible + idle-fade behavior untouched.
            slimSliverEnabled = cfg.shapeMode == WidgetShapeMode.RAMP,
            revealThresholdPx = 48f * density
        )
    }

    // ------------------------------------------------------------------
    // Observers
    // ------------------------------------------------------------------

    private fun observeEnabled() {
        managerScope.launch {
            overlayCoordinator.widgetEnabled.collect { enabled ->
                if (enabled && android.provider.Settings.canDrawOverlays(context)) show() else hide()
            }
        }
    }

    private fun observeConfig() {
        managerScope.launch {
            settingsProvider.dataStore.data
                .map { WidgetConfig.fromPreferences(it) }
                .collect { cfg ->
                    val sizeChanged = cfg.visualSizeDp() != config.visualSizeDp() ||
                            cfg.shapeMode != config.shapeMode
                    config = cfg
                    configState.value = cfg
                    resolver = WidgetGestureResolver(gestureConfig(cfg))
                    if (sizeChanged && composeView != null) applyWindowSizeAndPosition()
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
                idleFadedState.value = false
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
                applyWindowSizeAndPosition()
                restartIdleTimer()
            }
            WidgetGestureEffect.CollapseWidget -> applyWindowSizeAndPosition()
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
                if (config.shapeMode == WidgetShapeMode.RAMP) {
                    // RAMP's idle state IS the collapsed sliver now — not a shrink-in-place.
                    onGestureEvent(WidgetGestureEvent.CollapseToSliver)
                } else {
                    idleFadedState.value = true
                }
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
            setContent { WidgetRoot() }
            setViewTreeLifecycleOwner(this@FloatingWidgetManager)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetManager)
        }

        layoutParams = WindowManager.LayoutParams(
            0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    // Corner mode tucks into the true screen corner, under the nav indicator.
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        managerScope.launch {
            applyWindowSizeAndPositionInternal(addView = true)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            idleFadedState.value = false
            restartIdleTimer()
        }
    }

    fun hide() {
        exitEditMode(save = false)
        composeView?.let {
            try {
                windowManager.removeView(it)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing widget", e)
            }
        }
        composeView = null
        layoutParams = null
        armingJob?.cancel()
        tapTimeoutJob?.cancel()
        idleJob?.cancel()
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
        val isSliver = config.shapeMode == WidgetShapeMode.RAMP &&
                gestureState.phase == WidgetGesturePhase.SLIVER
        val (visualW, visualH) = if (isSliver) {
            sliverWidthDp.toFloat() to config.visualSizeDp().second
        } else {
            config.visualSizeDp()
        }
        val slack = if (isSliver) sliverTouchSlackDp else touchSlackDp
        val winW = ((visualW + slack) * density).roundToInt()
            .coerceAtLeast(((if (isSliver) sliverWidthDp else minTouchWidthDp) * density).roundToInt())
        val winH = ((visualH + slack * 2) * density).roundToInt()
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
            val (x, y) = snapFor(rawX, rawY, winW.toFloat(), winH.toFloat(), bounds)
            lp.x = x.roundToInt()
            lp.y = y.roundToInt()
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

    /** Snap per shape mode and record which edge/corner we ended up on (drives mirroring). */
    private fun snapFor(
        x: Float, y: Float, w: Float, h: Float, bounds: BubbleBounds
    ): Pair<Float, Float> = when (config.shapeMode) {
        WidgetShapeMode.RAMP -> {
            val edge = positionManager.widgetEdgeTarget(x, w, bounds)
            edgeState.value = edge
            positionManager.widgetEdgePosition(edge, y, w, h, bounds)
        }
        WidgetShapeMode.CORNER -> {
            val corner = positionManager.widgetCornerTarget(x, y, w, h, bounds)
            cornerState.value = corner
            positionManager.widgetCornerPosition(corner, w, h, bounds)
        }
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
        idleFadedState.value = false
        editModeState.value = true
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

    /** Floating control card shown during edit mode: live size/arch sliders + Done. */
    private fun showControlCard() {
        if (controlCardView != null) return
        controlCardView = ComposeView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent {
                val cfg by configState
                val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                val accentPreset = remember(accentName) {
                    AccentPreset.entries.find { it.name == accentName } ?: AccentPreset.Purple
                }
                WhispryTheme(accentPreset = accentPreset) {
                    WidgetEditControlCard(
                        config = cfg,
                        onBaseHeight = { setIntPref(DataStoreKeys.WIDGET_BASE_HEIGHT_DP, it) },
                        onProtrusion = { setIntPref(DataStoreKeys.WIDGET_PROTRUSION_DP, it) },
                        onArch = { setIntPref(DataStoreKeys.WIDGET_ARCH_DP, it) },
                        onDone = { exitEditMode(save = true) }
                    )
                }
            }
            setViewTreeLifecycleOwner(this@FloatingWidgetManager)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetManager)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (48 * context.resources.displayMetrics.density).roundToInt()
        }
        try {
            windowManager.addView(controlCardView, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit control card", e)
            controlCardView = null
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
        val accentPreset = remember(accentName) {
            AccentPreset.entries.find { it.name == accentName } ?: AccentPreset.Purple
        }
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

        WhispryTheme(accentPreset = accentPreset) {
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
                                onGestureEvent(
                                    WidgetGestureEvent.PointerMove(
                                        SystemClock.uptimeMillis(), delta.x, delta.y
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
                    idleFaded = idleFadedState.value,
                    editMode = editMode,
                    edge = edgeState.value,
                    corner = cornerState.value,
                    reducedMotion = reducedMotion
                )
            }
        }
    }
}
