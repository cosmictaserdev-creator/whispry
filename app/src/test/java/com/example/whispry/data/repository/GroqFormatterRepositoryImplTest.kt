package com.example.whispry.data.repository

import androidx.datastore.preferences.core.Preferences
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatCompletionResponse
import com.example.whispry.data.remote.api.dto.ChatChoice
import com.example.whispry.data.remote.api.dto.ChatMessage
import com.example.whispry.domain.model.FormattingProviderPreset
import com.example.whispry.domain.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GroqFormatterRepositoryImplTest {

    private val chatApiService: GroqChatApiService = mockk()
    private val apiKeyProvider: ApiKeyProvider = mockk()
    private val settingsProvider: SettingsProvider = mockk()
    private lateinit var repository: GroqFormatterRepositoryImpl

    @Before
    fun setUp() {
        repository = GroqFormatterRepositoryImpl(chatApiService, apiKeyProvider, settingsProvider)
    }

    private fun stubProvider(
        preset: FormattingProviderPreset = FormattingProviderPreset.GROQ,
        customBaseUrl: String = "",
        customModel: String = "",
        apiKey: String = "test-key"
    ) {
        val prefs: Preferences = mockk()
        every { prefs[DataStoreKeys.FORMATTING_PROVIDER_PRESET] } returns preset.name
        every { prefs[DataStoreKeys.FORMATTING_CUSTOM_BASE_URL] } returns customBaseUrl
        every { prefs[DataStoreKeys.FORMATTING_CUSTOM_MODEL] } returns customModel
        every { settingsProvider.dataStore } returns mockk {
            every { data } returns flowOf(prefs)
        }
        every { apiKeyProvider.getFormattingApiKey(preset) } returns apiKey
    }

    @Test
    fun `formatText calls the resolved provider's URL and model`() = runTest {
        stubProvider(preset = FormattingProviderPreset.OPENROUTER, apiKey = "or-key")
        val urlSlot = slot<String>()
        val authSlot = slot<String>()
        val requestSlot = slot<ChatCompletionRequest>()
        coEvery {
            chatApiService.chatCompletion(capture(urlSlot), capture(authSlot), capture(requestSlot))
        } returns Response.success(
            ChatCompletionResponse(choices = listOf(ChatChoice(message = ChatMessage("assistant", "cleaned up"))))
        )

        val result = repository.formatText("raw text", "system prompt")

        assertEquals("https://openrouter.ai/api/v1/chat/completions", urlSlot.captured)
        assertEquals("Bearer or-key", authSlot.captured)
        assertEquals("openai/gpt-4o-mini", requestSlot.captured.model)
        assertTrue(result is Result.Success)
        assertEquals("cleaned up", (result as Result.Success).data)
    }

    @Test
    fun `blank api key fails fast without calling the network`() = runTest {
        stubProvider(apiKey = "")

        val result = repository.formatText("raw text", "system prompt")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `network failure falls back to the clean fallback text, never the wrapped content`() = runTest {
        stubProvider()
        coEvery { chatApiService.chatCompletion(any(), any(), any()) } throws RuntimeException("boom")

        val result = repository.formatText("<transcript>raw</transcript>", "system prompt", fallbackText = "raw")

        assertTrue(result is Result.Success)
        assertEquals("raw", (result as Result.Success).data)
    }
}
