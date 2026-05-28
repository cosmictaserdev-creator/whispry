package com.example.whispry.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // MediaRecorder is nullable — it only exists while recording
    // null = not recording, non-null = recording in progress
    private var recorder: MediaRecorder? = null

    // track when recording started so we can calculate duration
    private var recordingStartMs: Long = 0L

    // the file we're currently writing to
    private var currentOutputFile: File? = null

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Start recording. Returns the output file path so the caller
     * knows where to find the audio when recording stops.
     * Returns null if recording is already in progress.
     */
    fun startRecording(): String? {
        // guard — don't start if already recording
        if (recorder != null) return null

        val outputFile = createOutputFile()
        currentOutputFile = outputFile

        recorder = createMediaRecorder().apply {
            // VOICE_RECOGNITION is better for transcription and 
            // often routes more reliably from Bluetooth headsets
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)   // 128kbps — good quality
            setAudioSamplingRate(44_100)        // 44.1kHz — CD quality
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                recordingStartMs = System.currentTimeMillis()
            } catch (e: Exception) {
                // if anything goes wrong, clean up immediately
                release()
                recorder = null
                currentOutputFile?.delete()
                currentOutputFile = null
                return null
            }
        }

        return outputFile.absolutePath
    }

    /**
     * Stop recording. Returns a RecordingResult with the file path
     * and duration, or null if we weren't recording.
     */
    fun stopRecording(): RecordingResult? {
        val rec = recorder ?: return null
        val file = currentOutputFile ?: return null

        val durationMs = System.currentTimeMillis() - recordingStartMs

        return try {
            rec.stop()
            RecordingResult(
                filePath = file.absolutePath,
                durationMs = durationMs
            )
        } catch (e: RuntimeException) {
            // stop() throws RuntimeException if called too quickly after start()
            // (less than ~100ms) — the recording would be empty/corrupt anyway
            file.delete()
            null
        } finally {
            // always release, regardless of success or exception
            rec.release()
            recorder = null
            currentOutputFile = null
            recordingStartMs = 0L
        }
    }

    /**
     * Get current amplitude for bubble animation.
     * Returns a normalized 0.0 - 1.0 float.
     * Returns 0 if not recording.
     */
    fun getCurrentAmplitude(): Float {
        val maxPossibleAmplitude = 32767f
        return (recorder?.maxAmplitude ?: 0) / maxPossibleAmplitude
    }

    /**
     * Whether recording is currently in progress.
     */
    val isRecording: Boolean get() = recorder != null

    /**
     * Cancel recording without producing a result — used when
     * the service is destroyed mid-recording.
     */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // ignore — we're cancelling anyway
        } finally {
            recorder?.release()
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
            recordingStartMs = 0L
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private fun createMediaRecorder(): MediaRecorder {
        // MediaRecorder constructor changed in Android S (API 31)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun createOutputFile(): File {
        // store in app's cache directory — no extra permissions needed
        // cache dir is private to our app, cleaned up by system when space needed
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        return File(dir, "recording_${System.currentTimeMillis()}.m4a")
    }

    // ------------------------------------------------------------------
    // Result model
    // ------------------------------------------------------------------

    data class RecordingResult(
        val filePath: String,
        val durationMs: Long
    )
}