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
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.WhispryTheme
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
        ValueAnimator.ofFloat(0f, 1f).apply {
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
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimatingSize = false
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
    @Inject lateinit var expandTextUseCase: ExpandTextUseCase
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

    private val bubbleState = mutableStateOf<BubbleState>(BubbleState.Idle)
    private val amplitude = mutableStateOf(0f)
    private val message = mutableStateOf("")
    private val isRecording = mutableStateOf(false)
    private val showCancelHint = mutableStateOf(false)

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

    companion object {
        const val ACTION_START_RECORDING = "com.example.whispry.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.whispry.action.STOP_RECORDING"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bubble_service_channel"
        private const val SUCCESS_DISMISS_DELAY_MS = 2200L
        private const val ERROR_DISMISS_DELAY_MS = 4000L
        private const val AMPLITUDE_POLL_INTERVAL_MS = 120L
        private const val CANCEL_HINT_DELAY_MS = 3000L
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observeTriggerEvents()
        observeSettings()
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: true }.collect { enabled ->
                overlayCoordinator.setWidgetEnabled(enabled)
            }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.collect { prefs ->
                duckingEnabled = prefs[DataStoreKeys.DUCKING_ENABLED] ?: true
                duckPercent = prefs[DataStoreKeys.DUCKING_PERCENT] ?: 70
                val presetName = prefs[DataStoreKeys.DEFAULT_OUTPUT_PRESET] ?: "NONE"
                defaultOutputPreset = try { OutputPreset.valueOf(presetName) } catch (e: Exception) { OutputPreset.NONE }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        updateForegroundStatus()
        when (intent?.action) {
            ACTION_START_RECORDING -> onRecordingStarted()
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

    private fun observeTriggerEvents() {
        serviceScope.launch {
            serviceBridge.triggerEvent.collect { event ->
                when (event) {
                    is ServiceBridge.TriggerEvent.RecordingStarted -> onRecordingStarted()
                    is ServiceBridge.TriggerEvent.RecordingStopped -> onRecordingStopped()
                    else -> {}
                }
            }
        }
    }

    private fun onRecordingStarted() {
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
                val preset = defaultOutputPreset
                val transcribeResult = withContext(Dispatchers.IO) {
                    val rawResult = audioRepository.transcribeAudio(
                        audioFilePath = filePath,
                        languageCode = if (language == "Auto") "en" else language
                    )
                    if (rawResult is com.example.whispry.domain.util.Result.Success) {
                        val expandedText = expandTextUseCase(rawResult.data)
                        val result = com.example.whispry.domain.util.Result.Success(expandedText)
                        if (preset != OutputPreset.NONE) {
                            withContext(Dispatchers.Main) { bubbleState.value = BubbleState.Formatting(preset) }
                            val formatResult = formatTranscriptUseCase(result.data, preset)
                            if (formatResult is com.example.whispry.domain.util.Result.Success) {
                                transcriptRepository.saveTranscript(formatResult.data, result.data, durationMs, language ?: "en", preset.name)
                                com.example.whispry.domain.util.Result.Success(formatResult.data)
                            } else {
                                transcriptRepository.saveTranscript(result.data, result.data, durationMs, language ?: "en", OutputPreset.NONE.name)
                                com.example.whispry.domain.util.Result.Success(result.data)
                            }
                        } else {
                            transcriptRepository.saveTranscript(result.data, result.data, durationMs, language ?: "en", OutputPreset.NONE.name)
                            result
                        }
                    } else rawResult
                }
                mainHandler.removeCallbacks(miniModeTransitionRunnable)
                hintJob.cancel()
                showCancelHint.value = false

                // Track usage after successful transcription (before UI handling)
                if (transcribeResult is com.example.whispry.domain.util.Result.Success) {
                    try {
                        usageRepository.incrementRequests(1)
                        val wordCount = transcribeResult.data.split("\\s+".toRegex()).size
                        if (wordCount > 0) usageRepository.incrementWords(wordCount)
                        val usage = usageRepository.getTodayUsage()
                        notificationManager.updateForegroundNotification(usage)
                    } catch (_: Exception) { }
                }

                val handleResult = {
                    when (transcribeResult) {
                        is com.example.whispry.domain.util.Result.Success<String> -> {
                            val insertResult = textInserter.insertText(transcribeResult.data, this@BubbleService, ServiceLocator.triggerService)
                            serviceBridge.emit(ServiceBridge.TriggerEvent.TranscriptionResult(transcribeResult.data))
                            bubbleState.value = BubbleState.Success
                            hapticHelper.vibrateSuccess()
                            soundManager.play(SoundEvent.SUCCESS)
                            audioDuckingManager.restore()
                            message.value = if (insertResult == TextInserter.InsertResult.PASTED) "Pasted ✓" else "Copied ✓"
                            scheduleBubbleDismissal(SUCCESS_DISMISS_DELAY_MS)
                            lastAudioFilePath = null
                        }
                        is com.example.whispry.domain.util.Result.Error -> {
                            val isNoInternet = transcribeResult.message == "no_internet"
                            bubbleState.value = BubbleState.Error(transcribeResult.message, isNoInternet)
                            message.value = if (isNoInternet) "No Internet" else transcribeResult.message
                            hapticHelper.vibrateError()
                            soundManager.play(SoundEvent.ERROR)
                            audioDuckingManager.restore()
                            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
                        }
                        else -> {}
                    }
                }
                if (isMiniMode) expandFromMini { handleResult() } else handleResult()
            } catch (e: CancellationException) {
                audioDuckingManager.restore()
            } catch (e: Exception) {
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
                setContent {
                    val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                    val accentPreset = remember(accentName) { AccentPreset.entries.find { it.name == accentName } ?: AccentPreset.Purple }
                    WhispryTheme(accentPreset = accentPreset) {
                        Box(
                            modifier = Modifier.wrapContentSize()
                        ) {
                            BubbleOverlay(
                                state = bubbleState.value,
                                amplitudeProvider = { amplitude.value },
                                message = message.value,
                                onRetry = { retryTranscription() },
                                onCancel = {
                                    transcriptionJob?.cancel()
                                    audioRecorder.cancel()
                                    hapticHelper.vibrateShort()
                                    bubbleState.value = BubbleState.Idle
                                    audioDuckingManager.restore()
                                    scheduleBubbleDismissal(0)
                                },
                                onStop = { if (bubbleState.value is BubbleState.Listening) serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped) },
                                onDrag = { dx, dy -> handleBubbleDrag(dx, dy) },
                                onDragEnd = { handleBubbleDragEnd() }
                            )
                        }
                    }
                }
                setViewTreeLifecycleOwner(this@BubbleService)
                setViewTreeViewModelStoreOwner(this@BubbleService)
                setViewTreeSavedStateRegistryOwner(this@BubbleService)
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
                val safeBounds = getSafeWindowBounds()
                val savedX = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_X]
                val savedY = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_Y]
                
                layoutParams?.let { lp ->
                    // Default to bottom center (centered X, near bottom Y)
                    lp.x = savedX ?: (safeBounds.left + (safeBounds.width() - 280.dpToPx()) / 2)
                    lp.y = savedY ?: (safeBounds.bottom - 140.dpToPx())
                }
                
                try {
                    windowManager.addView(composeView, layoutParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding bubble view", e)
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
        mainHandler.postDelayed({
            bubbleState.value = BubbleState.Idle
            mainHandler.postDelayed({ removeBubble() }, 600)
        }, delayMs)
    }



    private fun snapToNearestEdge() {
        val params = layoutParams ?: return
        val view = composeView ?: return
        val safeBounds = getSafeWindowBounds()
        val bubbleWidth = view.width
        val bubbleCenterX = params.x + bubbleWidth / 2
        val safeWidth = safeBounds.width()
        
        // Snapping logic: Snap to left or right edge with 16dp margin
        val targetX = if (bubbleCenterX < safeBounds.left + safeWidth / 2) {
            safeBounds.left + 16.dpToPx()
        } else {
            safeBounds.right - bubbleWidth - 16.dpToPx()
        }
        
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 300
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                try { windowManager.updateViewLayout(view, params) } catch (e: Exception) {}
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    saveBubblePosition()
                }
            })
            start()
        }
    }

    private fun saveBubblePosition() {
        val params = layoutParams ?: return
        serviceScope.launch {
            settingsProvider.dataStore.edit { prefs ->
                prefs[DataStoreKeys.BUBBLE_POSITION_X] = params.x
                prefs[DataStoreKeys.BUBBLE_POSITION_Y] = params.y
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
