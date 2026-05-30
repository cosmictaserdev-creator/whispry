package com.example.whispry.service

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.WakeWordMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordService : Service() {

    private val TAG = "Whispry_WakeWord"

    @Inject lateinit var serviceBridge: ServiceBridge
    @Inject lateinit var settingsProvider: SettingsProvider
    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var trainedModelMatcher: TrainedModelMatcher
    @Inject lateinit var apiKeyProvider: ApiKeyProvider
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startListeningIfEnabled()
                Intent.ACTION_SCREEN_OFF -> {
                    if (!isHeadphonesConnected()) stopListening()
                }
            }
        }
    }
    
    private val headphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", 0)
                    if (state == 1) startListeningIfEnabled()
                    else if (!isScreenOn()) stopListening()
                }
                android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        android.bluetooth.BluetoothProfile.EXTRA_STATE, 
                        android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
                    )
                    if (state == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        startListeningIfEnabled()
                    } else if (!isScreenOn()) {
                        stopListening()
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        
        val savedFp = apiKeyProvider.getFingerprint()
        if (savedFp != null) {
            trainedModelMatcher.loadFromPrefsString(savedFp)
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        
        val headFilter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(headphoneReceiver, headFilter)
        
        startListeningIfEnabled()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startListening() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        private var lastBuffer = mutableListOf<Byte>()

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                            
                            serviceScope.launch {
                                val modeId = settingsProvider.dataStore.data.map { it[DataStoreKeys.WAKE_WORD_MODE] ?: "DEFAULT" }.first()
                                val mode = try { WakeWordMode.valueOf(modeId) } catch(e: Exception) { WakeWordMode.DEFAULT }
                                
                                val detected = matches.any { matchesWakeWord(it, mode, lastBuffer.toByteArray()) }
                                
                                if (detected) {
                                    val vibrator = getSystemService(Vibrator::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(50)
                                    }
                                    soundManager.play(SoundEvent.WAKE_WORD_DETECTED)
                                    serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
                                }
                                lastBuffer.clear()
                                restartListening()
                            }
                        }
                        
                        override fun onError(error: Int) {
                            when (error) {
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> stopListening()
                                else -> restartListening()
                            }
                        }
                        
                        override fun onBufferReceived(buffer: ByteArray?) {
                            buffer?.let { 
                                if (lastBuffer.size > 64000) lastBuffer.clear()
                                lastBuffer.addAll(it.toList())
                            }
                        }

                        override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                        override fun onEndOfSpeech() { isListening = false }
                        override fun onBeginningOfSpeech() {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onRmsChanged(rmsdB: Float) {}
                    })
                }
                
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                restartListening()
            }
        }
    }

    private suspend fun matchesWakeWord(result: String, mode: WakeWordMode, buffer: ByteArray): Boolean {
        val normalized = result.lowercase().trim()
        return when (mode) {
            WakeWordMode.DEFAULT -> {
                val acceptedPhrases = listOf("hey whispry", "hey whisper", "hey whispery", "whispry", "hi whispry")
                acceptedPhrases.any { normalized.contains(it) }
            }
            WakeWordMode.CUSTOM -> {
                val customPhrase = settingsProvider.dataStore.data.map { it[DataStoreKeys.WAKE_WORD_PHRASE] ?: "hey whispry" }.first().lowercase()
                normalized.contains(customPhrase) || levenshteinDistance(normalized, customPhrase) <= 2
            }
            WakeWordMode.TRAINED -> {
                if (buffer.isEmpty()) return false
                val samples = FloatArray(buffer.size / 2) { i ->
                    ((buffer[i * 2 + 1].toInt() shl 8) or (buffer[i * 2].toInt() and 0xFF)).toShort().toFloat() / 32768f
                }
                trainedModelMatcher.matches(samples)
            }
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i-1] == s2[j-1]) dp[i-1][j-1]
                           else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
            }
        }
        return dp[s1.length][s2.length]
    }
    
    private fun restartListening() {
        isListening = false
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
            serviceScope.launch {
                delay(300)
                if (shouldBeListening()) startListening()
            }
        }
    }
    
    private fun stopListening() {
        isListening = false
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
    
    private fun isHeadphonesConnected(): Boolean {
        val am = getSystemService(AudioManager::class.java)
        return am.isWiredHeadsetOn || am.isBluetoothA2dpOn
    }
    
    private fun isScreenOn(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isInteractive
    }
    
    private fun shouldBeListening(): Boolean {
        return isScreenOn() || isHeadphonesConnected()
    }
    
    private fun startListeningIfEnabled() {
        serviceScope.launch {
            val enabled = settingsProvider.dataStore.data.map { it[DataStoreKeys.WAKE_WORD_ENABLED] ?: false }.first()
            if (enabled && shouldBeListening()) {
                startListening()
            }
        }
    }

    override fun onDestroy() {
        stopListening()
        unregisterReceiver(screenReceiver)
        unregisterReceiver(headphoneReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
