// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
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
    WHISPRY_D("Whispry Default"),
    WHISPRY_C("Whispry Chime"),
    SIRI("Siri Style");

    companion object {
        fun fromName(name: String?): TriggerSound {
            return entries.find { it.name == name } ?: WHISPRY_D
        }
    }
}

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {
    private val TAG = "Whispry_SoundManager"
    
    private var soundPool: SoundPool? = null
    private val soundMap = ConcurrentHashMap<String, Int>() // CacheKey to SoundId
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
    private var activeStreamId: Int = 0
    
    private var soundEnabled = true
    private var selectedSoundPack = TriggerSound.WHISPRY_D
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        initSoundPool()
        scope.launch {
            try {
                observeSettings()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SoundManager", e)
            }
        }
    }

    private fun initSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSounds[sampleId] = true
                    }
                }
            }
    }
    
    private fun getResourceName(event: SoundEvent, pack: TriggerSound): String? {
        return when (pack) {
            TriggerSound.WHISPRY_D -> when (event) {
                SoundEvent.TRIGGER_START     -> "whispry_wakeup_d"
                SoundEvent.TRIGGER_STOP      -> "whispry_listen_d"
                SoundEvent.SUCCESS           -> "whispry_end_d"
                SoundEvent.ERROR             -> "whispry_error_d"
                SoundEvent.WAKE_WORD_DETECTED -> "whispry_wakeup_d"
            }
            TriggerSound.WHISPRY_C -> when (event) {
                SoundEvent.TRIGGER_START     -> "whispry_wakeup_c"
                SoundEvent.TRIGGER_STOP      -> "whispry_listen_c"
                SoundEvent.SUCCESS           -> "whispry_end_c"
                SoundEvent.ERROR             -> "whispry_error_c"
                SoundEvent.WAKE_WORD_DETECTED -> "whispry_wakeup_c"
            }
            TriggerSound.SIRI -> when (event) {
                SoundEvent.TRIGGER_START     -> "siri_wakeup"
                SoundEvent.TRIGGER_STOP      -> "siri_listen"
                SoundEvent.SUCCESS           -> "siri_end"
                SoundEvent.ERROR             -> "siri_error"
                SoundEvent.WAKE_WORD_DETECTED -> "siri_wakeup"
            }
            else -> null
        }
    }

    private fun loadSound(event: SoundEvent, pack: TriggerSound): Int {
        val cacheKey = "${event.name}_${pack.name}"
        soundMap[cacheKey]?.let { return it }

        val resName = getResourceName(event, pack) ?: return -1
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return -1

        val soundId = soundPool?.load(context, resId, 1) ?: -1
        if (soundId != -1) {
            soundMap[cacheKey] = soundId
        }
        return soundId
    }

    private fun preloadPack(pack: TriggerSound) {
        if (pack == TriggerSound.NONE) return
        SoundEvent.entries.forEach { event ->
            loadSound(event, pack)
        }
    }
    
    fun play(event: SoundEvent) {
        if (!soundEnabled) return
        play(event, selectedSoundPack)
    }

    fun play(event: SoundEvent, pack: TriggerSound) {
        if (pack == TriggerSound.NONE) return
        
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        
        val cacheKey = "${event.name}_${pack.name}"
        val soundId = soundMap[cacheKey] ?: loadSound(event, pack)
        
        if (soundId != -1) {
            // Stop previous sound to prevent overlap "mess"
            if (activeStreamId != 0) {
                soundPool?.stop(activeStreamId)
            }

            if (loadedSounds[soundId] == true) {
                activeStreamId = soundPool?.play(soundId, 1f, 1f, 1, 0, 1f) ?: 0
            } else {
                // If not loaded yet, we don't update activeStreamId
            }
        }
    }

    private suspend fun observeSettings() {
        settingsProvider.dataStore.data.collect { prefs ->
            soundEnabled = prefs[DataStoreKeys.SOUND_ENABLED] ?: true
            val newPack = TriggerSound.fromName(prefs[DataStoreKeys.SOUND_START])
            
            selectedSoundPack = newPack
            preloadPack(selectedSoundPack)
        }
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        loadedSounds.clear()
        scope.cancel()
    }
}
