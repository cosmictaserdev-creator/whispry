package com.example.whispry.domain.usecase

import com.example.whispry.domain.model.TransliterationLanguage
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TransliterationUseCaseTest {

    private val repository: GroqFormatterRepository = mockk()
    private val useCase = TransliterationUseCase(repository)

    @Test
    fun `clean romanized output passes through on first try`() = runTest {
        coEvery { repository.formatText(any(), any(), any()) } returns Result.Success("kaise ho")

        val result = useCase("कैसे हो", TransliterationLanguage.HINDI)

        assertEquals(Result.Success("kaise ho"), result)
    }

    @Test
    fun `retries once when output regresses to native script, then succeeds`() = runTest {
        coEvery { repository.formatText(any(), any(), any()) } returnsMany listOf(
            Result.Success("कैसे हो"), // still Devanagari — guardrail should retry
            Result.Success("kaise ho")
        )

        val result = useCase("कैसे हो", TransliterationLanguage.HINDI)

        assertEquals(Result.Success("kaise ho"), result)
    }

    @Test
    fun `falls back to raw transcript if guardrail keeps failing`() = runTest {
        coEvery { repository.formatText(any(), any(), any()) } returns Result.Success("कैसे हो")

        val result = useCase("कैसे हो", TransliterationLanguage.HINDI)

        assertEquals(Result.Success("कैसे हो"), result)
    }

    @Test
    fun `fromCode resolves known and rejects unknown language codes`() {
        assertEquals(TransliterationLanguage.HINDI, TransliterationLanguage.fromCode("hi"))
        assertEquals(TransliterationLanguage.RUSSIAN, TransliterationLanguage.fromCode("ru"))
        assertEquals(null, TransliterationLanguage.fromCode("en"))
    }
}
