package com.example.whispry.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
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
import com.example.whispry.R
import com.example.whispry.domain.usecase.TranscribeAudioUseCase
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.util.HapticHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.abs
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@AndroidEntryPoint
class BubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val TAG = "Whispry_BubbleService"

    // ------------------------------------------------------------------
    // Injected dependencies
    // ------------------------------------------------------------------

    @Inject lateinit var serviceBridge: ServiceBridge
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var transcribeAudioUseCase: TranscribeAudioUseCase
    @Inject lateinit var textInserter: TextInserter
    @Inject lateinit var hapticHelper: HapticHelper
    @Inject lateinit var settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    @Inject lateinit var overlayCoordinator: WindowOverlayCoordinator
    @Inject lateinit var floatingWidgetManager: FloatingWidgetManager

    // ------------------------------------------------------------------
    // WindowManager & Compose
    // ------------------------------------------------------------------

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Compose states
    private val bubbleState = mutableStateOf<BubbleState>(BubbleState.Idle)
    private val amplitude = mutableStateOf(0f)
    private val message = mutableStateOf("")
    private val isRecording = mutableStateOf(false)
    private val showCancelHint = mutableStateOf(false)

    // Retry state
    private var lastAudioFilePath: String? = null
    private var lastAudioDurationMs: Long = 0L

    // ------------------------------------------------------------------
    // Lifecycle / VM Boilerplate for Compose in Service
    // ------------------------------------------------------------------

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    override val viewModelStore = ViewModelStore()
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // ------------------------------------------------------------------
    // Coroutine scope
    // ------------------------------------------------------------------

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var amplitudeJob: Job? = null
    private var transcriptionJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bubble_service_channel"
        private const val SUCCESS_DISMISS_DELAY_MS = 2200L
        private const val ERROR_DISMISS_DELAY_MS = 4000L
        private const val AMPLITUDE_POLL_INTERVAL_MS = 120L // Reduced from 80ms for battery efficiency
        private const val CANCEL_HINT_DELAY_MS = 3000L
    }

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observeTriggerEvents()
        observeSettings()
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsProvider.smartTriggerSuppression.collect { /* already handled in TriggerService */ }
        }
        serviceScope.launch {
            settingsProvider.dataStore.data.map { it[DataStoreKeys.FLOATING_WIDGET_ENABLED] ?: true }.collect { enabled ->
                overlayCoordinator.setWidgetEnabled(enabled)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        createNotificationChannel()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        audioRecorder.cancel()
        removeBubble()
        serviceScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, BubbleService::class.java).apply {
            action = "ACTION_RESTART_FROM_TASK_REMOVED"
        }
        val pendingIntent = android.app.PendingIntent.getService(
            applicationContext,
            1001,
            restartIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 2000L, // restart after 2 seconds
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    // ------------------------------------------------------------------
    // Observe trigger events
    // ------------------------------------------------------------------

    private fun observeTriggerEvents() {
        serviceScope.launch {
            serviceBridge.triggerEvent.collect { event ->
                Log.d(TAG, "Received event: $event")
                when (event) {
                    is ServiceBridge.TriggerEvent.RecordingStarted -> onRecordingStarted()
                    is ServiceBridge.TriggerEvent.RecordingStopped -> onRecordingStopped()
                    is ServiceBridge.TriggerEvent.Idle -> { /* nothing */ }
                    else -> {}
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Recording flow
    // ------------------------------------------------------------------

    private fun onRecordingStarted() {
        mainHandler.removeCallbacksAndMessages(null)
        overlayCoordinator.showBubble() // Notify coordinator
        showBubble()

        // On API 34+, we need to promote the FGS type to microphone to access the mic from background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        }

        message.value = ""
        bubbleState.value = BubbleState.Listening
        isRecording.value = true

        val path = audioRecorder.startRecording()
        if (path != null) {
            startAmplitudePolling()
        } else {
            bubbleState.value = BubbleState.Error("Mic failed")
            message.value = "Mic failed"
            isRecording.value = false
            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
        }
    }

    private fun onRecordingStopped() {
        stopAmplitudePolling()
        bubbleState.value = BubbleState.Processing()
        isRecording.value = false

        // On API 34+, demote back to specialUse after recording finishes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        }

        val result = audioRecorder.stopRecording()
        if (result == null) {
            bubbleState.value = BubbleState.Error("Too short")
            message.value = "Too short"
            scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
            return
        }

        // Cache for retry
        lastAudioFilePath = result.filePath
        lastAudioDurationMs = result.durationMs

        performTranscription(result.filePath, result.durationMs)
    }

    private fun performTranscription(filePath: String, durationMs: Long) {
        showCancelHint.value = false
        transcriptionJob?.cancel()
        
        transcriptionJob = serviceScope.launch {
            // Show cancel hint after 3 seconds
            val hintJob = launch {
                delay(CANCEL_HINT_DELAY_MS)
                showCancelHint.value = true
                bubbleState.value = BubbleState.Processing(showCancelHint = true)
            }

            try {
                // Get selected language from DataStore
                val language = settingsProvider.language.first()
                
                val transcribeResult = withContext(Dispatchers.IO) {
                    transcribeAudioUseCase(
                        audioFilePath = filePath,
                        durationMs = durationMs,
                        language = if (language == "Auto") null else language
                    )
                }

                hintJob.cancel()
                showCancelHint.value = false

                when (transcribeResult) {
                    is com.example.whispry.domain.util.Result.Success -> {
                        val insertResult = textInserter.insertText(
                            text = transcribeResult.data,
                            context = this@BubbleService,
                            accessibilityService = ServiceLocator.triggerService
                        )

                        // Emit result for potential observers (like Tutorial)
                        serviceBridge.emit(ServiceBridge.TriggerEvent.TranscriptionResult(transcribeResult.data))

                        bubbleState.value = BubbleState.Success
                        hapticHelper.vibrateSuccess()
                        message.value = if (insertResult == TextInserter.InsertResult.PASTED) "Pasted ✓" else "Copied ✓"
                        scheduleBubbleDismissal(SUCCESS_DISMISS_DELAY_MS)
                        
                        // Clear cache on success
                        lastAudioFilePath = null
                    }
                    is com.example.whispry.domain.util.Result.Error -> {
                        val isNoInternet = transcribeResult.message == "no_internet"
                        bubbleState.value = BubbleState.Error(
                            message = transcribeResult.message,
                            isNetworkError = isNoInternet
                        )
                        message.value = if (isNoInternet) "No Internet" else transcribeResult.message
                        hapticHelper.vibrateError()
                        scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
                    }
                    else -> {}
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Transcription cancelled by user")
            } catch (e: Exception) {
                hintJob.cancel()
                showCancelHint.value = false
                bubbleState.value = BubbleState.Error("Error")
                message.value = "Error"
                scheduleBubbleDismissal(ERROR_DISMISS_DELAY_MS)
            }
        }
    }

    private fun retryTranscription() {
        val path = lastAudioFilePath ?: return
        bubbleState.value = BubbleState.Processing()
        message.value = ""
        mainHandler.removeCallbacksAndMessages(null) // Cancel dismissal
        performTranscription(path, lastAudioDurationMs)
    }

    private fun startAmplitudePolling() {

        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive) {
                val rawAmp = audioRecorder.getCurrentAmplitude()
                // Log.d(TAG, "Amplitude: $rawAmp") // For debugging if needed
                amplitude.value = rawAmp
                delay(AMPLITUDE_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        amplitude.value = 0f
    }

    // ------------------------------------------------------------------
    // WindowManager management
    // ------------------------------------------------------------------

    private fun showBubble() {
        mainHandler.post {
            // Force state to Listening if we are just starting
            if (bubbleState.value == BubbleState.Idle) {
                bubbleState.value = BubbleState.Listening
            }

            if (composeView != null) return@post

            composeView = ComposeView(this).apply {
                setContent {
                    val accentName by settingsProvider.accentColor.collectAsState(initial = "Purple")
                    val accentPreset = remember(accentName) {
                        AccentPreset.entries.find { it.name == accentName } ?: AccentPreset.Purple
                    }

                    WhispryTheme(accentPreset = accentPreset) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Floating Siri-style Morphing Ring
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
                            ) {
                                BubbleOverlay(
                                    state = bubbleState.value,
                                    amplitudeProvider = { amplitude.value },
                                    message = message.value,
                                    onRetry = { retryTranscription() },
                                    onCancel = {
                                        transcriptionJob?.cancel()
                                        bubbleState.value = BubbleState.Idle
                                        scheduleBubbleDismissal(0)
                                    }
                                )
                            }
                        }
                    }
                }
                // Set required owners for Compose in Service
                setViewTreeLifecycleOwner(this@BubbleService)
                setViewTreeViewModelStoreOwner(this@BubbleService)
                setViewTreeSavedStateRegistryOwner(this@BubbleService)
                
                setOnTouchListener(BubbleTouchListener())
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(composeView, layoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding bubble view", e)
                composeView = null
            }
        }
    }

    private fun removeBubble() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        composeView = null
        layoutParams = null
        isRecording.value = false
        overlayCoordinator.hideBubble() // Notify coordinator
    }

    private fun scheduleBubbleDismissal(delayMs: Long) {
        mainHandler.postDelayed({
            // First trigger the exit animation by setting state to Idle
            bubbleState.value = BubbleState.Idle
            
            // Wait for animation to finish before removing view from WindowManager
            mainHandler.postDelayed({
                removeBubble()
            }, 600) // 600ms is enough for most spring/tween exit animations
        }, delayMs)
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            // Since we are now MATCH_PARENT, we need to handle drag differently if needed,
            // or just allow pass-through. For now, we'll keep it simple.
            return false
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Whispry Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Whispry is active")
            .setContentText("Volume trigger ready")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
