package com.example.whispry.domain.usecase

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import com.example.whispry.features.tone.domain.repository.AppToneRepository
import com.example.whispry.service.ServiceLocator
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormatTranscriptUseCaseTest {

    private val groqFormatterRepository: GroqFormatterRepository = mockk()
    private val settingsProvider: SettingsProvider = mockk()
    private val appToneRepository: AppToneRepository = mockk(relaxed = true)
    private val getActiveMemoriesUseCase: GetActiveMemoriesUseCase = mockk()
    private lateinit var useCase: FormatTranscriptUseCase

    @Before
    fun setUp() {
        // Default: app-aware tone OFF so the passed-in preset is used verbatim.
        every { settingsProvider.appAwareToneEnabled } returns flowOf(false)
        every { settingsProvider.customAiInstructions } returns flowOf("MY CUSTOM RULES")
        every { settingsProvider.translateTargetLanguage } returns flowOf("English")
        coEvery { getActiveMemoriesUseCase() } returns emptyList()
        ServiceLocator.lastForegroundPackage = null
        ServiceLocator.triggerService = null

        useCase = FormatTranscriptUseCase(
            groqFormatterRepository,
            settingsProvider,
            appToneRepository,
            getActiveMemoriesUseCase
        )
    }

    private fun stubFormat(): Triple<CapturingSlot<String>, CapturingSlot<String>, CapturingSlot<String>> {
        val userSlot = slot<String>()
        val promptSlot = slot<String>()
        val fallbackSlot = slot<String>()
        coEvery {
            groqFormatterRepository.formatText(capture(userSlot), capture(promptSlot), capture(fallbackSlot))
        } returns Result.Success("formatted output")
        return Triple(userSlot, promptSlot, fallbackSlot)
    }

    @Test
    fun `built-in preset - injects anti-answer guard and wraps transcript`() = runTest {
        val (userSlot, promptSlot, fallbackSlot) = stubFormat()

        useCase("what time is it", OutputPreset.STORYTELLER)

        // System prompt carries both the preset's own instructions and the universal guard.
        assertTrue(promptSlot.captured.contains("narrative"))
        assertTrue(promptSlot.captured.contains(FormatTranscriptUseCase.ANTI_ANSWER_GUARD))

        // Transcript is delimiter-wrapped, fallback stays clean (no tags on failure).
        assertEquals(
            "${FormatTranscriptUseCase.TRANSCRIPT_OPEN}\nwhat time is it\n${FormatTranscriptUseCase.TRANSCRIPT_CLOSE}",
            userSlot.captured
        )
        assertEquals("what time is it", fallbackSlot.captured)
    }

    @Test
    fun `custom preset - honored verbatim with no guard`() = runTest {
        val (userSlot, promptSlot, _) = stubFormat()

        useCase("hello there", OutputPreset.CUSTOM)

        assertEquals("MY CUSTOM RULES", promptSlot.captured)
        assertFalse(promptSlot.captured.contains(FormatTranscriptUseCase.ANTI_ANSWER_GUARD))
        // Transcript is still wrapped so the boundary is consistent even for custom prompts.
        assertTrue(userSlot.captured.contains("hello there"))
    }

    @Test
    fun `none preset - passes through untouched, never calls formatter`() = runTest {
        stubFormat()

        val result = useCase("just the raw words", OutputPreset.NONE)

        assertTrue(result is Result.Success)
        assertEquals("just the raw words", (result as Result.Success).data)
        coVerify(exactly = 0) { groqFormatterRepository.formatText(any(), any(), any()) }
    }

    @Test
    fun `blank input - returns raw and never calls formatter`() = runTest {
        stubFormat()

        val result = useCase("   ", OutputPreset.STORYTELLER)

        assertTrue(result is Result.Success)
        assertEquals("   ", (result as Result.Success).data)
        coVerify(exactly = 0) { groqFormatterRepository.formatText(any(), any(), any()) }
    }
}
