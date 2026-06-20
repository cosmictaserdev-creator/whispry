package com.example.whispry.service

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDuckingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Store original volume before ducking so we can restore exactly
    private var originalVolume: Int = -1
    private var isDucked: Boolean = false

    fun duck(duckPercent: Int) {
        // only duck if something is actually playing
        if (!audioManager.isMusicActive) return
        
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        
        // store original — we restore this exact value later
        originalVolume = currentVolume
        
        // calculate target volume
        // duckPercent = 70 means reduce by 70%, keep 30%
        val reductionFactor = duckPercent / 100f
        val targetVolume = (currentVolume * (1f - reductionFactor))
            .toInt()
            .coerceAtLeast(0)
        
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            targetVolume,
            0  // no UI flag — silent volume change, no popup
        )
        isDucked = true
    }

    fun restore() {
        if (!isDucked || originalVolume == -1) return
        
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            originalVolume,
            0  // silent restore
        )
        isDucked = false
        originalVolume = -1
    }

    // Call in BubbleService.onDestroy() — safety net
    fun restoreIfNeeded() {
        if (isDucked) restore()
    }
}
