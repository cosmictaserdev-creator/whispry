// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.usecase.FormatTranscriptUseCase
import com.example.whispry.domain.usecase.TranscribeAudioUseCase
import com.example.whispry.features.expander.domain.usecase.ExpandTextUseCase
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.resolveAccentColors
import com.example.whispry.util.HapticHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.annotation.RequiresPermission
import androidx.datastore.preferences.core.edit
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.UsageRepository
import com.example.whispry.notification.WhispryNotificationManager
import com.example.whispry.service.AudioDuckingManager
import kotlinx.coroutines.flow.distinctUntilChanged

@AndroidEntryPoint
class BubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    private var isMiniMode = false
    private var isAnimatingSize = false

    // Keyboard-anchored record toggle (see KeyboardLogoSurface). Hosted here because this is the
    // persistent overlay service; shown only while the soft keyboard is up (IME bounds reported
    // by TriggerService via serviceBridge.imeBounds).
    private var keyboardLogoEnabled = false
    private var currentImeBounds: android.graphics.Rect? = null
    private var keyboardLogoView: ComposeView? = null
    private var keyboardLogoLp: WindowManager.LayoutParams? = null
    private var keyboardLogoXPct = DataStoreKeys.DEFAULT_KEYBOARD_LOGO_X
    // How far (px) the logo rests above the IME's top edge; persisted so a drag placement survives
    // across keyboard opens. The logo rides the keyboard by anchoring to this offset.
    private var keyboardLogoYOffsetPx = 0
    private var isKeyboardLogoVisible = mutableStateOf(false)
    private var keyboardLogoYAnimator: ValueAnimator? = null
    private var hideKeyboardLogoRunnable: Runnable? = null
    // Preset submenu shown on double-tap; a second overlay window anchored to the pill.
    private var presetPanelView: ComposeView? = null
    private var presetPanelLp: WindowManager.LayoutParams? = null
    // Hidden-apps suppression (see WindowOverlayCoordinator.widgetsHidden).
    private var widgetsHidden = false

    private fun getSafeWindowBounds(): android.graphics.Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
            android.graphics.Rect(0, 0, getScreenWidth(), getScreenHeight())
        }
    }

    private fun handleBubbleDrag(dx: Float, dy: Float) {
        val lp = layoutParams ?: return
        val view = composeView ?: return
        val safeBounds = getSafeWindowBounds()
        val viewWidth = view.width
        val viewHeight = view.height
        
        lp.x = (lp.x + dx.toInt()).coerceIn(safeBounds.left, (safeBounds.right - viewWidth).coerceAtLeast(safeBounds.left))
        lp.y = (lp.y + dy.toInt()).coerceIn(safeBounds.top, (safeBounds.bottom - viewHeight).coerceAtLeast(safeBounds.top))
        
        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, lp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating drag layout", e)
        }
    }

    private fun handleBubbleDragEnd() {
        snapToNearestEdge()
    }

    private var miniTransitionAnimator: ValueAnimator? = null

    private fun transitionToMiniProcessing() {
        if (bubbleState.value !is BubbleState.Processing || isMiniMode) return

        val safeBounds = getSafeWindowBounds()

        // Target: Top Right corner
        val targetX = safeBounds.right - 72.dpToPx()
        val targetY = safeBounds.top + 100.dpToPx()

        val view = composeView ?: return
        val lp = layoutParams ?: return

        val startX = lp.x
        val startY = lp.y
        val startWidth = view.width
        val startHeight = view.height
        val endSize = 56.dpToPx()

        isAnimatingSize = true
        miniTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                if (!view.isAttachedToWindow) return@addUpdateListener

                val progress = anim.animatedValue as Float
                try {
                    lp.x = (startX + (targetX - startX) * progress).toInt()
                    lp.y = (startY + (targetY - startY) * progress).toInt()
                    lp.width = (startWidth + (endSize - startWidth) * progress).toInt()
                    lp.height = (startHeight + (endSize - startHeight) * progress).toInt()

                    windowManager.updateViewLayout(view, lp)
                } catch (e: Exception) {
                    anim.cancel()
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var wasCanceled = false
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    wasCanceled = true
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimatingSize = false
                    miniTransitionAnimator = null
                    if (wasCanceled) return
                    val currentState = bubbleState.value
                    if (currentState is BubbleState.Processing && view.isAttachedToWindow) {
                        isMiniMode = true
                        bubbleState.value = currentState.copy(miniMode = true)
                        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
                    }
                }
            })
            start()
        }
    }

    /** Abort a pending or in-flight shrink-to-mini transition and restore normal window size.
     *  Needed because the shrink runs as a raw ValueAnimator outside coroutine control - if the
     *  AI response lands mid-shrink, the animator keeps mutating window size underneath the
     *  success/error state that's about to render unless explicitly cancelled here. */
    private fun cancelPendingMiniTransition() {
        mainHandler.removeCallbacks(miniModeTransitionRunnable)
        val anim = miniTransitionAnimator
        if (anim != null && anim.isRunning) {
            anim.cancel()
            if (!isMiniMode) {
                val view = composeView
                val lp = layoutParams
                if (view != null && lp != null && view.isAttachedToWindow) {
                    lp.width = WindowManager.LayoutParams.WRAP_CONTENT
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                    try { windowManager.updateViewLayout(view, lp) } catch (e: Exception) {}
                }
            }
        }
        miniTransitionAnimator = null
    }

    private fun expandFromMini(onExpanded: () -> Unit) {
        if (!isMiniMode) {
            onExpanded()
            return
        }
        
        val safeBounds = getSafeWindowBounds()
        val centerX = safeBounds.left + (safeBounds.width() / 2) - 75.dpToPx()
        val centerY = safeBounds.top + (safeBounds.height() / 2) - 75.dpToPx()
        
        val view = composeView ?: return
        val lp = layoutParams ?: return
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()

        val startX = lp.x
        val startY = lp.y
        val startSize = view.width
        val endSize = 150.dpToPx()

        isAnimatingSize = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                if (!view.isAttachedToWindow) return@addUpdateListener
                
                try {
                    val progress = anim.animatedValue as Float
                    lp.x = (startX + (centerX - startX) * progress).toInt()
                    lp.y = (startY + (centerY - startY) * progress).toInt()
                    lp.width = (startSize + (endSize - startSize) * progress).toInt()
                    lp.height = (startSize + (endSize - startSize) * progress).toInt()
                    
                    windowManager.updateViewLayout(view, lp)
                } catch (e: Exception) {
                    anim.cancel()
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimatingSize = false
                    isMiniMode = false
                    lp.width = WindowManager.LayoutParams.WRAP_CONTENT
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                    try {
                        if (view.isAttachedToWindow) {
                            windowManager.updateViewLayout(view, lp)
                        }
                    } catch (e: Exception) {}
                    onExpanded()
                }
            })
            start()
        }
    }

    private val TAG = "Whispry_BubbleService"

    @Inject lateinit var serviceBridge: ServiceBridge
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var audioRepository: AudioRepository
    @Inject lateinit var transcriptRepository: TranscriptRepository
    @Inject lateinit var formatTranscriptUseCase: FormatTranscriptUseCase
    @Inject lateinit var transcribeAudioUseCase: TranscribeAudioUseCase
    @Inject lateinit var transliterationUseCase: com.example.whispry.domain.usecase.TransliterationUseCase
    @Inject lateinit var expandTextUseCase: ExpandTextUseCase
    @Inject lateinit var processTranscriptUseCase: com.example.whispry.domain.usecase.ProcessTranscriptUseCase
    @Inject lateinit var voiceCommandExecutor: VoiceCommandExecutor
    @Inject lateinit var textInserter: TextInserter
    @Inject lateinit var hapticHelper: HapticHelper
    @Inject lateinit var settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    @Inject lateinit var overlayCoordinator: WindowOverlayCoordinator
    @Inject lateinit var floatingWidgetManager: FloatingWidgetManager
    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var audioDuckingManager: AudioDuckingManager
    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var notificationManager: WhispryNotificationManager

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var duckingEnabled = true
    private var duckPercent = 70
    private var defaultOutputPreset = OutputPreset.NONE
    private var instantModeEnabled = false
    // Press Action carried on the START intent (Normal unless a custom single/double press fired).
    private var pendingPressAction: com.example.whispry.domain.model.PressAction = com.example.whispry.domain.model.PressAction.Normal

    private val bubbleState = mutableStateOf<BubbleState>(BubbleState.Idle)
    private val amplitude = mutableStateOf(0f)
    private val message = mutableStateOf("")
    private val isRecording = mutableStateOf(false)
    private val showCancelHint = mutableStateOf(false)
    // Mirrors the widget's drag-down cancel state: pill drains red, "release to cancel".
    private val cancelArming = mutableStateOf(false)

    private val positionManager by lazy {
        BubblePositionManager(resources.displayMetrics.density)
    }

    private var lastAudioFilePath: String? = null
    private var lastAudioDurationMs: Long = 0L

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var amplitudeJob: Job? = null
    private var transcriptionJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Result of routing a finished transcript through [processTranscriptUseCase]. */
    private sealed interface Processed {
        data class Text(val text: String) : Processed
        data class Command(
            val action: com.example.whispry.domain.model.VoiceAppAction,
            val original: String
        ) : Processed
        data class Err(val message: String, val noInternet: Boolean) : Processed
    }

    companion object {
        const val ACTION_START_RECORDING = "com.example.whispry.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.whispry.action.STOP_RECORDING"
        const val EXTRA_PRESS_ACTION = "extra_press_action"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bubble_service_channel"
        private const val SUCCESS_DISMISS_DELAY_MS = 2200L
        private const val ERROR_DISMISS_DELAY_MS = 4000L
        private const val AMPLITUDE_POLL_INTERVAL_MS = 120L
        private const val CANCEL_HINT_DELAY_MS = 3000L
        private const val BUBBLE_ADD_VIEW_RETRY_DELAY_MS = 150L
        private const val KEYBOARD_LOGO_WIDTH_DP = 72
        private const val KEYBOARD_LOGO_HEIGHT_DP = 42
        private const val KEYBOARD_LOGO_MARGIN_DP = 8
        private const val PRESET_PANEL_WIDTH_DP = 250
        private const val PRESET_PANEL_HEIGHT_DP = 320
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observeTriggerEvents()
        observeSettings()
        observeKeyboardLogo()
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: DataStoreKeys.DEFAULT_FLOATING_WIDGET_ENABLED }.collect { enabled ->
                overlayCoordinator.setWidgetEnabled(enabled)
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.collect { prefs ->
                duckingEnabled = prefs[DataStoreKeys.DUCKING_ENABLED] ?: true
                duckPercent = prefs[DataStoreKeys.DUCKING_PERCENT] ?: 70
                val presetName = prefs[DataStoreKeys.DEFAULT_OUTPUT_PRESET] ?: "NONE"
                defaultOutputPreset = try { OutputPreset.valueOf(presetName) } catch (e: Exception) { OutputPreset.NONE }
                instantModeEnabled = prefs[DataStoreKeys.INSTANT_MODE_ENABLED] ?: false
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        updateForegroundStatus()
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                pendingPressAction = com.example.whispry.domain.model.PressAction.parse(intent.getStringExtra(EXTRA_PRESS_ACTION))
                onRecordingStarted()
            }
            ACTION_STOP_RECORDING -> onRecordingStopped()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    private fun updateForegroundStatus() {
        try {
            val notification = buildNotification()
            updateNotificationWithUsage()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Check for microphone permission before requesting microphone FGS type
                val hasMicPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasMicPermission) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    // Fallback: Start without microphone type to prevent crash, then user can be prompted
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update foreground status", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        audioRecorder.cancel()
        audioDuckingManager.restoreIfNeeded()
        removeBubble()
        hideKeyboardLogo()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, BubbleService::class.java).apply {
            action = "ACTION_RESTART_FROM_TASK_REMOVED"
        }
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1001,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 2000L,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        floatingWidgetManager.onOrientationChanged()
        val view = composeView ?: return
        val params = layoutParams ?: return
        if (!view.isAttachedToWindow) return

        serviceScope.launch {
            val savedXPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_X]
            val savedYPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_Y]
            if (savedXPct == null || savedYPct == null) return@launch

            val safeBounds = getSafeWindowBounds().toBubbleBounds()
            val bubbleWidth = view.width.toFloat()
            val bubbleHeight = view.height.toFloat()

            val (denormX, denormY) = positionManager.denormalize(
                savedXPct.toFloat(), savedYPct.toFloat(), safeBounds
            )
            val (newX, newY) = positionManager.snapPosition(
                denormX, denormY, bubbleWidth, bubbleHeight, safeBounds
            )

            animateToPosition(params.x, newX.toInt(), params.y, newY.toInt()) {
                saveBubblePosition()
            }
        }
    }

    private fun observeKeyboardLogo() {
        serviceScope.launch {
            overlayCoordinator.widgetsHidden.collect { hidden ->
                widgetsHidden = hidden
                updateKeyboardLogo()
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.map { prefs ->
                prefs[DataStoreKeys.KEYBOARD_LOGO_X] ?: DataStoreKeys.DEFAULT_KEYBOARD_LOGO_X
            }.distinctUntilChanged().collect { xPct ->
                keyboardLogoXPct = xPct
                if (keyboardLogoView != null) currentImeBounds?.let { positionKeyboardLogo(it) }
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.map { prefs ->
                prefs[DataStoreKeys.KEYBOARD_LOGO_Y_OFFSET]
                    ?: (KEYBOARD_LOGO_MARGIN_DP * resources.displayMetrics.density).toInt()
            }.distinctUntilChanged().collect { offset ->
                keyboardLogoYOffsetPx = offset
                if (keyboardLogoView != null) currentImeBounds?.let { positionKeyboardLogo(it) }
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.map { prefs ->
                prefs[DataStoreKeys.KEYBOARD_LOGO_ENABLED] ?: DataStoreKeys.DEFAULT_KEYBOARD_LOGO_ENABLED
            }.distinctUntilChanged().collect { enabled ->
                keyboardLogoEnabled = enabled
                updateKeyboardLogo()
            }
        }
        serviceScope.launch {
            serviceBridge.imeBounds.collect { bounds ->
                currentImeBounds = bounds
                updateKeyboardLogo()
            }
        }
    }

    /** Show, hide, or reposition the keyboard logo to match the current enable + IME state. */
    private fun updateKeyboardLogo() {
        if (!keyboardLogoEnabled || widgetsHidden) {
            hideKeyboardLogo()
            return
        }
        val ime = currentImeBounds
        if (ime == null || ime.isEmpty) {
            isKeyboardLogoVisible.value = false
            hideKeyboardLogoPresetPanel()
            val view = keyboardLogoView
            if (view != null && view.visibility == View.VISIBLE) {
                hideKeyboardLogoRunnable?.let { mainHandler.removeCallbacks(it) }
                val runnable = Runnable {
                    if (currentImeBounds == null || currentImeBounds?.isEmpty == true) {
                        view.visibility = View.GONE
                    }
                }
                hideKeyboardLogoRunnable = runnable
                mainHandler.postDelayed(runnable, 210L)
            }
            return
        }

        hideKeyboardLogoRunnable?.let { mainHandler.removeCallbacks(it) }
        hideKeyboardLogoRunnable = null

        if (keyboardLogoView == null) {
            val view = createKeyboardLogoView() ?: return
            try {
                windowManager.addView(view, keyboardLogoLp)
                isKeyboardLogoVisible.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error showing keyboard logo", e)
                keyboardLogoView = null
            }
        } else {
            val view = keyboardLogoView
            if (view?.visibility != View.VISIBLE) {
                view?.visibility = View.VISIBLE
            }
            isKeyboardLogoVisible.value = true
        }
        positionKeyboardLogo(ime)
    }

    private fun createKeyboardLogoView(): ComposeView? {
        val density = resources.displayMetrics.density
        keyboardLogoLp = WindowManager.LayoutParams(
            (KEYBOARD_LOGO_WIDTH_DP * density).toInt(),
            (KEYBOARD_LOGO_HEIGHT_DP * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        return ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeViewModelStoreOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            setContent {
                val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                val accentColors = remember(accentName) { resolveAccentColors(accentName) }
                WhispryTheme(accentColors = accentColors) {
                    KeyboardLogoSurface(
                        isRecording = isRecording.value,
                        isVisible = isKeyboardLogoVisible.value,
                        onToggle = { onKeyboardLogoToggle() },
                        onDoubleTap = {
                            if (presetPanelView != null) hideKeyboardLogoPresetPanel()
                            else showKeyboardLogoPresetPanel()
                        },
                        onDrag = { dx, dy -> moveKeyboardLogo(dx, dy) },
                        onDragStart = { hideKeyboardLogoPresetPanel() },
                        onDragEnd = { persistKeyboardLogoPosition() }
                    )
                }
            }
        }.also { keyboardLogoView = it }
    }

    /** Anchor the logo a saved offset above the IME's top edge, at the saved X percentage. */
    private fun positionKeyboardLogo(ime: android.graphics.Rect) {
        val view = keyboardLogoView ?: return
        val lp = keyboardLogoLp ?: return
        val safeBounds = getSafeWindowBounds()

        val maxX = (safeBounds.right - lp.width).coerceAtLeast(safeBounds.left)
        val x = safeBounds.left + ((safeBounds.width() - lp.width) * (keyboardLogoXPct / 100f)).toInt()
        lp.x = x.coerceIn(safeBounds.left, maxX)

        val offsetPx = keyboardLogoYOffsetPx.coerceAtLeast((KEYBOARD_LOGO_MARGIN_DP * resources.displayMetrics.density).toInt())
        val targetY = (ime.top - lp.height - offsetPx).coerceAtLeast(safeBounds.top)

        keyboardLogoYAnimator?.cancel()
        if (!view.isAttachedToWindow) {
            lp.y = targetY
            return
        }

        val startY = lp.y
        if (startY == targetY) return

        keyboardLogoYAnimator = ValueAnimator.ofInt(startY, targetY).apply {
            duration = 160
            interpolator = PathInterpolator(0.1f, 0.9f, 0.2f, 1.0f)
            addUpdateListener { anim ->
                lp.y = anim.animatedValue as Int
                try {
                    if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error positioning keyboard logo", e)
                }
            }
            start()
        }
    }

    private fun moveKeyboardLogo(dx: Float, dy: Float) {
        val view = keyboardLogoView ?: return
        val lp = keyboardLogoLp ?: return
        val safeBounds = getSafeWindowBounds()
        lp.x = (lp.x + dx.toInt()).coerceIn(safeBounds.left, (safeBounds.right - lp.width).coerceAtLeast(safeBounds.left))
        lp.y = (lp.y + dy.toInt()).coerceIn(safeBounds.top, (safeBounds.bottom - lp.height).coerceAtLeast(safeBounds.top))
        try {
            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error dragging keyboard logo", e)
        }
    }

    private fun persistKeyboardLogoPosition() {
        val lp = keyboardLogoLp ?: return
        val ime = currentImeBounds ?: return
        val safeBounds = getSafeWindowBounds()
        val usable = safeBounds.width() - lp.width
        val pct = if (usable > 0) {
            ((lp.x - safeBounds.left).toFloat() / usable * 100f).toInt().coerceIn(0, 100)
        } else {
            DataStoreKeys.DEFAULT_KEYBOARD_LOGO_X
        }
        val marginPx = (KEYBOARD_LOGO_MARGIN_DP * resources.displayMetrics.density).toInt()
        val rawOffset = ime.top - (lp.y + lp.height)
        val offset = rawOffset.coerceAtLeast(marginPx)
        keyboardLogoXPct = pct
        keyboardLogoYOffsetPx = offset
        serviceScope.launch {
            settingsProvider.dataStore.edit { prefs ->
                prefs[DataStoreKeys.KEYBOARD_LOGO_X] = pct
                prefs[DataStoreKeys.KEYBOARD_LOGO_Y_OFFSET] = offset
            }
        }
        if (rawOffset != offset) {
            positionKeyboardLogo(ime)
        }
    }

    private fun hideKeyboardLogo() {
        isKeyboardLogoVisible.value = false
        hideKeyboardLogoRunnable?.let { mainHandler.removeCallbacks(it) }
        hideKeyboardLogoRunnable = null
        keyboardLogoYAnimator?.cancel()
        val view = keyboardLogoView ?: return
        keyboardLogoView = null
        hideKeyboardLogoPresetPanel()
        try { windowManager.removeView(view) } catch (e: Exception) {}
    }

    /** Open the preset submenu: a second overlay window anchored to the pill, growing up/down to avoid clipping. */
    private fun showKeyboardLogoPresetPanel() {
        val pillLp = keyboardLogoLp ?: return
        val pillView = keyboardLogoView ?: return
        if (!pillView.isAttachedToWindow) return
        if (presetPanelView != null) {
            presetPanelView?.visibility = View.VISIBLE
            return
        }
        val density = resources.displayMetrics.density
        val safeBounds = getSafeWindowBounds()
        val panelWidth = (PRESET_PANEL_WIDTH_DP * density).toInt()
        val panelHeight = (PRESET_PANEL_HEIGHT_DP * density).toInt()

        presetPanelLp = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Anchor to the pill's top-left; x stays under the pill, y flips up or down based on space.
            x = (pillLp.x - (panelWidth - pillLp.width) / 2)
                .coerceIn(safeBounds.left, (safeBounds.right - panelWidth).coerceAtLeast(safeBounds.left))
            val belowSpace = safeBounds.bottom - (pillLp.y + pillLp.height)
            val aboveSpace = pillLp.y - safeBounds.top
            y = if (belowSpace >= panelHeight) {
                pillLp.y + pillLp.height + (KEYBOARD_LOGO_MARGIN_DP * density).toInt()
            } else {
                (pillLp.y - panelHeight - (KEYBOARD_LOGO_MARGIN_DP * density).toInt()).coerceAtLeast(safeBounds.top)
            }
        }
        val view = ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeViewModelStoreOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            setContent {
                val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                val accentColors = remember(accentName) { resolveAccentColors(accentName) }
                val selectedPreset by settingsProvider.dataStore.data
                    .map { prefs ->
                        val name = prefs[DataStoreKeys.DEFAULT_OUTPUT_PRESET] ?: OutputPreset.NONE.name
                        try { OutputPreset.valueOf(name) } catch (e: Exception) { OutputPreset.NONE }
                    }
                    .collectAsState(initial = OutputPreset.NONE)
                WhispryTheme(accentColors = accentColors) {
                    KeyboardLogoPresetPanel(
                        selectedPreset = selectedPreset,
                        onPick = { preset -> selectPresetFromKeyboardLogo(preset) },
                        onDismiss = { hideKeyboardLogoPresetPanel() }
                    )
                }
            }
        }
        try {
            windowManager.addView(view, presetPanelLp)
            presetPanelView = view
        } catch (e: Exception) {
            Log.e(TAG, "Error showing keyboard logo preset panel", e)
            presetPanelView = null
        }
    }

    private fun hideKeyboardLogoPresetPanel() {
        val view = presetPanelView ?: return
        presetPanelView = null
        try { windowManager.removeView(view) } catch (e: Exception) {}
    }

    private fun selectPresetFromKeyboardLogo(preset: OutputPreset) {
        serviceScope.launch {
            settingsProvider.dataStore.edit { prefs -> prefs[DataStoreKeys.DEFAULT_OUTPUT_PRESET] = preset.name }
        }
        hideKeyboardLogoPresetPanel()
    }

    private fun onKeyboardLogoToggle() {
        if (isRecording.value) {
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
        } else {
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
        }
    }

    private fun observeTriggerEvents() {
        serviceScope.launch {
            serviceBridge.triggerEvent.collect { event ->
                when (event) {
                    is ServiceBridge.TriggerEvent.RecordingStarted -> onRecordingStarted()
                    is ServiceBridge.TriggerEvent.RecordingStopped -> onRecordingStopped()
                    is ServiceBridge.TriggerEvent.RecordingCancelled -> cancelSession()
                    is ServiceBridge.TriggerEvent.CancelArming -> cancelArming.value = event.armed
                    else -> {}
                }
            }
        }
    }

    /** Abort the in-flight recording/transcription and dismiss (widget drag-down + pill ✕ share this). */
    private fun cancelSession() {
        transcriptionJob?.cancel()
        cancelPendingMiniTransition()
        audioRecorder.cancel()
        stopAmplitudePolling()
        hapticHelper.vibrateShort()
        cancelArming.value = false
        isRecording.value = false
        bubbleState.value = BubbleState.Idle
        audioDuckingManager.restore()
        scheduleBubbleDismissal(0)
    }

    private fun onRecordingStarted() {
        hideKeyboardLogoPresetPanel()
        if (isRecording.value) return
        if (duckingEnabled) audioDuckingManager.duck(duckPercent)
        mainHandler.removeCallbacksAndMessages(null)
        overlayCoordinator.showBubble()
        showBubble()
        
        // Reset window size layout params to WRAP_CONTENT for new session
        layoutParams?.let { lp ->
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            try {
                composeView?.let { view ->
                    if (view.isAttachedToWindow) {
                        windowManager.updateViewLayout(view, lp)
                    }
                }
            } catch (e: Exception) {}
        }
        
        updateForegroundStatus()
        message.value = ""
        cancelArming.value = false
        bubbleState.value = BubbleState.Listening
        isRecording.value = true
        isMiniMode = false
        val path = audioRecorder.startRecording()
        if (path != null) {
            startAmplitudePolling()
        } else {
            bubbleState.value = BubbleState.Error("Mic failed")
            message.value = "Mic failed"
            isRecording.value = false
            audioDuckingManager.restore()
            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
        }
    }

    private val miniModeTransitionRunnable = Runnable {
        if (bubbleState.value is BubbleState.Processing) transitionToMiniProcessing()
    }

    private fun onRecordingStopped() {
        if (!isRecording.value && bubbleState.value !is BubbleState.Listening) return
        stopAmplitudePolling()
        bubbleState.value = BubbleState.Processing()
        isRecording.value = false
        val result = audioRecorder.stopRecording()
        soundManager.play(SoundEvent.TRIGGER_STOP)
        if (result == null) {
            bubbleState.value = BubbleState.Error("Too short")
            message.value = "Too short"
            soundManager.play(SoundEvent.ERROR)
            audioDuckingManager.restore()
            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
            return
        }
        lastAudioFilePath = result.filePath
        lastAudioDurationMs = result.durationMs
        mainHandler.postDelayed(miniModeTransitionRunnable, 2000)
        performTranscription(result.filePath, result.durationMs)
    }

    private fun performTranscription(filePath: String, durationMs: Long) {
        showCancelHint.value = false
        transcriptionJob?.cancel()
        transcriptionJob = serviceScope.launch {
            val hintJob = launch {
                delay(CANCEL_HINT_DELAY_MS)
                showCancelHint.value = true
                if (bubbleState.value is BubbleState.Processing) {
                    bubbleState.value = (bubbleState.value as BubbleState.Processing).copy(showCancelHint = true)
                }
            }
            try {
                val language = settingsProvider.language.first()
                // A custom press action can override the preset, or redirect to opening an app.
                val pressAction = pendingPressAction
                pendingPressAction = com.example.whispry.domain.model.PressAction.Normal
                val preset = when (pressAction) {
                    is com.example.whispry.domain.model.PressAction.Preset -> pressAction.preset
                    else -> defaultOutputPreset
                }
                val processed: Processed = withContext(Dispatchers.IO) {
                    val rawResult = audioRepository.transcribeAudio(
                        audioFilePath = filePath,
                        languageCode = if (language == "Auto") "en" else language
                    )
                    when (rawResult) {
                        is com.example.whispry.domain.util.Result.Success -> {
                            val transcribedText = rawResult.data
                            // Romanize a native-script transcript before any further routing, so
                            // preset formatting / expand / voice commands all see Latin script.
                            val transliterationLanguage = com.example.whispry.domain.model.TransliterationLanguage.fromCode(language)
                            val rawText = if (transliterationLanguage != null && settingsProvider.hinglishOutputEnabled.first()) {
                                val transliterated = transliterationUseCase(transcribedText, transliterationLanguage)
                                if (transliterated is com.example.whispry.domain.util.Result.Success) {
                                    transliterated.data
                                } else {
                                    transcribedText
                                }
                            } else {
                                transcribedText
                            }
                            // Open-app press action: skip routing, open the app and copy the text.
                            if (pressAction is com.example.whispry.domain.model.PressAction.OpenApp && rawText.isNotBlank()) {
                                transcriptRepository.saveTranscript(rawText, rawText, durationMs, language ?: "en", OutputPreset.NONE.name)
                                return@withContext Processed.Command(
                                    com.example.whispry.domain.model.VoiceAppAction.OpenApp(
                                        pressAction.packageName, pressAction.label, rawText
                                    ),
                                    rawText
                                )
                            }
                            if (preset != OutputPreset.NONE) {
                                withContext(Dispatchers.Main) { bubbleState.value = BubbleState.Formatting(preset) }
                            }
                            // Single decision point: expand / insert / voice command / normal format.
                            when (val outcome = processTranscriptUseCase(rawText, preset)) {
                                is com.example.whispry.domain.model.TranscriptOutcome.InsertText -> {
                                    val finalText = outcome.text
                                    val savedPreset = if (finalText == rawText) OutputPreset.NONE.name else preset.name
                                    transcriptRepository.saveTranscript(finalText, rawText, durationMs, language ?: "en", savedPreset)
                                    Processed.Text(finalText)
                                }
                                is com.example.whispry.domain.model.TranscriptOutcome.RunCommand -> {
                                    transcriptRepository.saveTranscript(outcome.originalTranscript, rawText, durationMs, language ?: "en", OutputPreset.NONE.name)
                                    Processed.Command(outcome.action, outcome.originalTranscript)
                                }
                            }
                        }
                        is com.example.whispry.domain.util.Result.Error ->
                            Processed.Err(rawResult.message, rawResult.message == "no_internet")
                        else -> Processed.Err("Error", false)
                    }
                }
                cancelPendingMiniTransition()
                hintJob.cancel()
                showCancelHint.value = false

                // Track usage after successful transcription (before UI handling)
                val producedText: String? = when (processed) {
                    is Processed.Text -> processed.text
                    is Processed.Command -> processed.original
                    is Processed.Err -> null
                }
                if (producedText != null) {
                    try {
                        usageRepository.incrementRequests(1)
                        val wordCount = producedText.split("\\s+".toRegex()).size
                        if (wordCount > 0) usageRepository.incrementWords(wordCount)
                        val usage = usageRepository.getTodayUsage()
                        notificationManager.updateForegroundNotification(usage)
                    } catch (_: Exception) { }
                }

                val handleResult = {
                    when (processed) {
                        is Processed.Text -> {
                            val insertResult = textInserter.insertText(processed.text, this@BubbleService, ServiceLocator.triggerService)
                            serviceBridge.emit(ServiceBridge.TriggerEvent.TranscriptionResult(processed.text))
                            bubbleState.value = BubbleState.Success
                            hapticHelper.vibrateSuccess()
                            soundManager.play(SoundEvent.SUCCESS)
                            audioDuckingManager.restore()
                            message.value = if (insertResult == TextInserter.InsertResult.PASTED) "Pasted ✓" else "Copied ✓"
                            scheduleBubbleDismissal(SUCCESS_DISMISS_DELAY_MS)
                            lastAudioFilePath = null
                        }
                        is Processed.Command -> {
                            val exec = voiceCommandExecutor.execute(processed.action)
                            serviceBridge.emit(ServiceBridge.TriggerEvent.TranscriptionResult(processed.original))
                            if (exec is VoiceCommandExecutor.ExecResult.Failed) {
                                // App missing / launch failed -> paste original transcript so nothing is lost.
                                textInserter.insertText(processed.original, this@BubbleService, ServiceLocator.triggerService)
                            }
                            bubbleState.value = BubbleState.Success
                            hapticHelper.vibrateSuccess()
                            soundManager.play(SoundEvent.SUCCESS)
                            audioDuckingManager.restore()
                            message.value = when (exec) {
                                is VoiceCommandExecutor.ExecResult.Launched -> exec.message
                                is VoiceCommandExecutor.ExecResult.Failed -> exec.message
                            }
                            scheduleBubbleDismissal(SUCCESS_DISMISS_DELAY_MS)
                            lastAudioFilePath = null
                        }
                        is Processed.Err -> {
                            val isNoInternet = processed.noInternet
                            serviceBridge.emit(ServiceBridge.TriggerEvent.TranscriptionFailed(processed.message))
                            bubbleState.value = BubbleState.Error(processed.message, isNoInternet)
                            message.value = if (isNoInternet) "No Internet" else processed.message
                            hapticHelper.vibrateError()
                            soundManager.play(SoundEvent.ERROR)
                            audioDuckingManager.restore()
                            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
                        }
                    }
                }
                if (isMiniMode) expandFromMini { handleResult() } else handleResult()
            } catch (e: CancellationException) {
                cancelPendingMiniTransition()
                audioDuckingManager.restore()
            } catch (e: Exception) {
                cancelPendingMiniTransition()
                hintJob.cancel()
                showCancelHint.value = false
                audioDuckingManager.restore()
                val handleError = {
                    bubbleState.value = BubbleState.Error("Error")
                    message.value = "Error"
                    scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
                }
                if (isMiniMode) expandFromMini { handleError() } else handleError()
            }
        }
    }

    private fun retryTranscription() {
        val path = lastAudioFilePath ?: return
        bubbleState.value = BubbleState.Processing()
        message.value = ""
        mainHandler.removeCallbacksAndMessages(null)
        performTranscription(path, lastAudioDurationMs)
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive) {
                amplitude.value = audioRecorder.getCurrentAmplitude()
                delay(AMPLITUDE_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        amplitude.value = 0f
    }

    private fun showBubble() {
        mainHandler.post {
            if (bubbleState.value == BubbleState.Idle) bubbleState.value = BubbleState.Listening
            if (composeView != null) return@post
            composeView = ComposeView(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewTreeLifecycleOwner(this@BubbleService)
                setViewTreeViewModelStoreOwner(this@BubbleService)
                setViewTreeSavedStateRegistryOwner(this@BubbleService)
                setContent {
                    val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                    val accentColors = remember(accentName) { resolveAccentColors(accentName) }
                    val instant by settingsProvider.instantModeEnabled.collectAsState(initial = false)
                    WhispryTheme(accentColors = accentColors) {
                        Box(
                            modifier = Modifier.wrapContentSize()
                        ) {
                            BubbleOverlay(
                                state = bubbleState.value,
                                amplitudeProvider = { amplitude.value },
                                message = message.value,
                                cancelArming = cancelArming.value,
                                instant = instant,
                                onRetry = { retryTranscription() },
                                onCancel = { cancelSession() },
                                onStop = { if (bubbleState.value is BubbleState.Listening) serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped) },
                                onDrag = { dx, dy -> handleBubbleDrag(dx, dy) },
                                onDragEnd = { handleBubbleDragEnd() }
                            )
                        }
                    }
                }
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            serviceScope.launch {
                val safeBounds = getSafeWindowBounds().toBubbleBounds()
                val savedXPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_X]
                val savedYPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_Y]
                val initialWidth = 280.dpToPx().toFloat()
                val initialHeight = 68.dpToPx().toFloat()

                layoutParams?.let { lp ->
                    if (savedXPct != null && savedYPct != null) {
                        val (x, y) = positionManager.denormalize(
                            savedXPct.toFloat(), savedYPct.toFloat(), safeBounds
                        )
                        lp.x = x.toInt()
                        lp.y = y.toInt()
                    } else {
                        val (x, y) = positionManager.defaultPosition(
                            initialWidth, initialHeight, safeBounds
                        )
                        lp.x = x.toInt()
                        lp.y = y.toInt()
                    }
                }

                try {
                    com.example.whispry.util.retryOnce(
                        onFirstFailure = { e ->
                            Log.e(TAG, "Error adding bubble view, retrying once", e)
                            delay(BUBBLE_ADD_VIEW_RETRY_DELAY_MS)
                        }
                    ) {
                        windowManager.addView(composeView, layoutParams)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Retry failed, giving up on this session's bubble", e)
                    composeView = null
                }
            }
        }
    }

    private fun removeBubble() {
        composeView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        composeView = null
        layoutParams = null
        isRecording.value = false
        overlayCoordinator.hideBubble()
    }

    private fun scheduleBubbleDismissal(delayMs: Long) {
        // Instant Mode abbreviates the linger + exit-animation wait, it doesn't zero them out -
        // the exit transition (see BubbleOverlay) still needs to finish before the view is torn down.
        val lingerMs = if (instantModeEnabled && delayMs > 0L) (delayMs / 4).coerceAtLeast(200L) else delayMs
        val exitAnimMs = if (instantModeEnabled) 150L else 600L
        mainHandler.postDelayed({
            bubbleState.value = BubbleState.Idle
            mainHandler.postDelayed({ removeBubble() }, exitAnimMs)
        }, lingerMs)
    }



    private fun snapToNearestEdge() {
        val params = layoutParams ?: return
        val view = composeView ?: return
        val safeBounds = getSafeWindowBounds().toBubbleBounds()
        val bubbleWidth = view.width.toFloat()
        val bubbleHeight = view.height.toFloat()

        val (targetX, targetY) = positionManager.snapPosition(
            params.x.toFloat(), params.y.toFloat(),
            bubbleWidth, bubbleHeight, safeBounds
        )

        animateToPosition(params.x, targetX.toInt(), params.y, targetY.toInt()) {
            saveBubblePosition()
        }
    }

    private fun animateToPosition(
        startX: Int, endX: Int, startY: Int, endY: Int,
        onEnd: () -> Unit
    ) {
        val params = layoutParams ?: return
        val view = composeView ?: return
        val dx = endX - startX
        val dy = endY - startY

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                params.x = (startX + dx * fraction).toInt()
                params.y = (startY + dy * fraction).toInt()
                try { windowManager.updateViewLayout(view, params) } catch (e: Exception) {}
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    private fun saveBubblePosition() {
        val params = layoutParams ?: return
        val safeBounds = getSafeWindowBounds().toBubbleBounds()
        val (nx, ny) = positionManager.normalize(
            params.x.toFloat(), params.y.toFloat(), safeBounds
        )
        serviceScope.launch {
            settingsProvider.dataStore.edit { prefs ->
                prefs[DataStoreKeys.BUBBLE_POSITION_X] = nx.toInt()
                prefs[DataStoreKeys.BUBBLE_POSITION_Y] = ny.toInt()
            }
        }
    }

    private fun getScreenWidth(): Int = resources.displayMetrics.widthPixels
    private fun getScreenHeight(): Int = resources.displayMetrics.heightPixels
    
    private fun createNotificationChannel() {
        notificationManager.createChannels()
    }

    private fun buildNotification(): Notification {
        return notificationManager.buildFallbackNotification()
    }

    private fun updateNotificationWithUsage() {
        serviceScope.launch {
            try {
                usageRepository.resetIfNewDay()
                val usage = usageRepository.getTodayUsage()
                notificationManager.updateForegroundNotification(usage)
            } catch (_: Exception) { }
        }
    }
        
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}

private fun android.graphics.Rect.toBubbleBounds() = BubbleBounds(left, top, right, bottom)
