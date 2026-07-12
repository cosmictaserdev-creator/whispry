package com.example.whispry.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.util.Result

class TranscribeAudioUseCaseTest {

    private val audioRepository: AudioRepository = mockk()
    private val transcriptRepository: TranscriptRepository = mockk(relaxed = true)
    private val formatTranscriptUseCase: FormatTranscriptUseCase = mockk(relaxed = true)
    private val hinglishTransliterationUseCase: HinglishTransliterationUseCase = mockk(relaxed = true)
    private val settingsProvider: SettingsProvider = mockk()
    private lateinit var useCase: TranscribeAudioUseCase

    @Before
    fun setUp() {
        every { settingsProvider.hinglishOutputEnabled } returns flowOf(false)
        useCase = TranscribeAudioUseCase(
            audioRepository, transcriptRepository, formatTranscriptUseCase,
            hinglishTransliterationUseCase, settingsProvider
        )
    }

    @Test
    fun `success - saves transcript and returns text`() = runTest {
        // Given
        coEvery {
            audioRepository.transcribeAudio(any(), any())
        } returns Result.Success("Hello world")

        // When
        val result = useCase("/path/audio.m4a", durationMs = 3000L)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("Hello world", (result as Result.Success).data)

        // verify it was saved to DB
        coVerify {
            transcriptRepository.saveTranscript(
                text = "Hello world",
                rawText = "Hello world",
                durationMs = 3000L,
                languageCode = "en",
                preset = OutputPreset.NONE.name
            )
        }
    }

    @Test
    fun `api failure - does NOT save transcript, returns error`() = runTest {
        // Given
        coEvery {
            audioRepository.transcribeAudio(any(), any())
        } returns Result.Error("Network error")

        // When
        val result = useCase("/path/audio.m4a", durationMs = 2000L)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).message)

        // verify nothing was saved
        coVerify(exactly = 0) {
            transcriptRepository.saveTranscript(
                text = any(),
                rawText = any(),
                durationMs = any(),
                languageCode = any(),
                preset = any()
            )
        }
    }

    @Test
    fun `custom language code is passed through`() = runTest {
        coEvery {
            audioRepository.transcribeAudio(any(), "hi")
        } returns Result.Success("नमस्ते")

        val result = useCase("/path/audio.m4a", 1000L, language = "hi")

        assertTrue(result is Result.Success)
        coVerify {
            transcriptRepository.saveTranscript(
                text = "नमस्ते",
                rawText = "नमस्ते",
                durationMs = 1000L,
                languageCode = "hi",
                preset = OutputPreset.NONE.name
            )
        }
    }

    @Test
    fun `hinglish output romanizes hi transcript but keeps raw text devanagari`() = runTest {
        every { settingsProvider.hinglishOutputEnabled } returns flowOf(true)
        coEvery {
            audioRepository.transcribeAudio(any(), "hi")
        } returns Result.Success("नमस्ते")
        coEvery {
            hinglishTransliterationUseCase("नमस्ते")
        } returns Result.Success("Namaste")

        val result = useCase("/path/audio.m4a", 1000L, language = "hi")

        assertTrue(result is Result.Success)
        assertEquals("Namaste", (result as Result.Success).data)
        coVerify {
            transcriptRepository.saveTranscript(
                text = "Namaste",
                rawText = "नमस्ते",
                durationMs = 1000L,
                languageCode = "hi",
                preset = OutputPreset.NONE.name
            )
        }
    }
}