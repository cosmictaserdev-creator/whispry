package com.example.whispry.service

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AudioRecorderTest {

    private val context: Context = mockk()
    private lateinit var audioRecorder: AudioRecorder

    @Before
    fun setUp() {
        every { context.cacheDir } returns Files.createTempDirectory("test_cache").toFile()
        audioRecorder = AudioRecorder(context)
    }

    @Test
    fun `isRecording is false initially`() {
        assertFalse(audioRecorder.isRecording)
    }

    @Test
    fun `stopRecording returns null when not recording`() {
        assertNull(audioRecorder.stopRecording())
    }

    @Test
    fun `getCurrentAmplitude returns 0 when not recording`() {
        assert(audioRecorder.getCurrentAmplitude() == 0f)
    }

    @Test
    fun `cancel does not throw when not recording`() {
        // should complete without exception
        audioRecorder.cancel()
    }
}