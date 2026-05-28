package com.example.whispry.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.util.Result

class TranscribeAudioUseCaseTest {

    private val audioRepository: AudioRepository = mockk()
    private val transcriptRepository: TranscriptRepository = mockk(relaxed = true)
    private lateinit var useCase: TranscribeAudioUseCase

    @Before
    fun setUp() {
        useCase = TranscribeAudioUseCase(audioRepository, transcriptRepository)
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
                durationMs = 3000L,
                languageCode = "en"
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
            transcriptRepository.saveTranscript(any(), any(), any())
        }
    }

    @Test
    fun `custom language code is passed through`() = runTest {
        coEvery {
            audioRepository.transcribeAudio(any(), "hi")
        } returns Result.Success("नमस्ते")

        val result = useCase("/path/audio.m4a", 1000L, languageCode = "hi")

        assertTrue(result is Result.Success)
        coVerify {
            transcriptRepository.saveTranscript(
                text = "नमस्ते",
                durationMs = 1000L,
                languageCode = "hi"
            )
        }
    }
}