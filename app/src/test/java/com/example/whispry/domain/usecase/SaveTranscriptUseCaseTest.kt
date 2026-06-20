package com.example.whispry.domain.usecase

import com.example.whispry.domain.repository.TranscriptRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveTranscriptUseCaseTest {

    private val repository: TranscriptRepository = mockk(relaxed = true)
    private val useCase = SaveTranscriptUseCase(repository)

    @Test
    fun `invoke should call repository with correct parameters`() = runTest {
        useCase("Hello world", durationMs = 3000L, languageCode = "en")

        coVerify {
            repository.saveTranscript(
                text = "Hello world",
                rawText = "",
                durationMs = 3000L,
                languageCode = "en",
                preset = "NONE"
            )
        }
    }

    @Test
    fun `invoke with default language should use en`() = runTest {
        useCase("Test", durationMs = 1000L)

        coVerify {
            repository.saveTranscript(
                text = "Test",
                rawText = "",
                durationMs = 1000L,
                languageCode = "en",
                preset = "NONE"
            )
        }
    }

    @Test
    fun `invoke with empty text should still call repository`() = runTest {
        useCase("", durationMs = 0L)

        coVerify {
            repository.saveTranscript(
                text = "",
                rawText = "",
                durationMs = 0L,
                languageCode = "en",
                preset = "NONE"
            )
        }
    }
}