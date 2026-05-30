package com.example.whispry.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEvent {
    TRIGGER_START,      // when recording begins
    TRIGGER_STOP,       // when user releases (recording stops)
    SUCCESS,            // when transcript is ready + pasted
    ERROR,              // when transcription fails
    WAKE_WORD_DETECTED  // when wake word is heard
}

enum class TriggerSound(val displayName: String) {
    NONE("Silent"),
    SIRI_CLICK("Siri Click"),
    SOFT_CHIME("Soft Chime"),
    SOFT_POP("Soft Pop"),
    DOUBLE_BEEP("Double Beep"),
    WHOOSH("Whoosh");

    companion object {
        fun fromName(name: String?): TriggerSound {
            return entries.find { it.name == name } ?: SIRI_CLICK
        }
    }
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {
    private val soundCache = mutableMapOf<TriggerSound, AudioTrack>()
    private var soundEnabled = true
    private var selectedStartSound = TriggerSound.SIRI_CLICK
    private var selectedSuccessSound = TriggerSound.SOFT_CHIME
    private var selectedErrorSound = TriggerSound.SOFT_POP
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        scope.launch {
            preloadSounds()
            observeSettings()
        }
    }
    
    private fun preloadSounds() {
        TriggerSound.entries
            .filter { it != TriggerSound.NONE }
            .forEach { sound ->
                val samples = when (sound) {
                    TriggerSound.SIRI_CLICK  -> SoundGenerator.generateSiriClick()
                    TriggerSound.SOFT_CHIME  -> SoundGenerator.generateSoftChime()
                    TriggerSound.SOFT_POP    -> SoundGenerator.generateSoftPop()
                    TriggerSound.DOUBLE_BEEP -> SoundGenerator.generateDoubleBeep()
                    TriggerSound.WHOOSH      -> SoundGenerator.generateWhoosh()
                    else -> return@forEach
                }
                
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                    
                audioTrack.write(samples, 0, samples.size)
                soundCache[sound] = audioTrack
            }
    }
    
    fun play(event: SoundEvent) {
        if (!soundEnabled) return
        
        val sound = when (event) {
            SoundEvent.TRIGGER_START     -> selectedStartSound
            SoundEvent.TRIGGER_STOP      -> TriggerSound.SOFT_POP
            SoundEvent.SUCCESS           -> selectedSuccessSound
            SoundEvent.ERROR             -> selectedErrorSound
            SoundEvent.WAKE_WORD_DETECTED -> selectedStartSound
        }
        
        if (sound == TriggerSound.NONE) return
        
        // Respect device's ringer mode
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        
        scope.launch {
            soundCache[sound]?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        }
    }

    private suspend fun observeSettings() {
        settingsProvider.dataStore.data.collect { prefs ->
            soundEnabled = prefs[DataStoreKeys.SOUND_ENABLED] ?: true
            selectedStartSound = TriggerSound.fromName(prefs[DataStoreKeys.SOUND_START])
            selectedSuccessSound = TriggerSound.fromName(prefs[DataStoreKeys.SOUND_SUCCESS])
            selectedErrorSound = TriggerSound.fromName(prefs[DataStoreKeys.SOUND_ERROR])
        }
    }
    
    fun release() {
        soundCache.values.forEach { it.release() }
        soundCache.clear()
        scope.cancel()
    }
}
