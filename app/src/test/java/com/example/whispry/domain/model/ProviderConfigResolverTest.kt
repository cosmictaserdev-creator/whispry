package com.example.whispry.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigResolverTest {

    @Test
    fun `curated preset uses its baked-in base URL and model, ignoring custom fields`() {
        val resolved = ProviderConfigResolver.resolveFormatting(
            preset = FormattingProviderPreset.OPENROUTER,
            customBaseUrl = "https://example.com/ignored/",
            customModel = "ignored-model",
            apiKey = "key-123"
        )

        assertEquals("https://openrouter.ai/api/v1/", resolved.baseUrl)
        assertEquals("openai/gpt-4o-mini", resolved.model)
        assertEquals("key-123", resolved.apiKey)
    }

    @Test
    fun `custom preset uses the user-typed base URL and model instead of any preset default`() {
        val resolved = ProviderConfigResolver.resolveFormatting(
            preset = FormattingProviderPreset.CUSTOM,
            customBaseUrl = "http://192.168.1.50:11434/v1",
            customModel = "llama3.2",
            apiKey = ""
        )

        assertEquals("http://192.168.1.50:11434/v1/", resolved.baseUrl)
        assertEquals("llama3.2", resolved.model)
    }

    @Test
    fun `custom base URL that already ends with a slash is not doubled up`() {
        val resolved = ProviderConfigResolver.resolveTranscription(
            preset = TranscriptionProviderPreset.CUSTOM,
            customBaseUrl = "https://my-server.example/v1/",
            customModel = "whisper-local",
            apiKey = "abc"
        )

        assertEquals("https://my-server.example/v1/", resolved.baseUrl)
    }

    @Test
    fun `default GROQ preset for transcription matches today's behavior`() {
        val resolved = ProviderConfigResolver.resolveTranscription(
            preset = TranscriptionProviderPreset.GROQ,
            customBaseUrl = "",
            customModel = "",
            apiKey = "key"
        )

        assertEquals("https://api.groq.com/openai/v1/", resolved.baseUrl)
        assertEquals("whisper-large-v3", resolved.model)
    }

    @Test
    fun `blank custom base URL stays blank instead of becoming a bare slash`() {
        val resolved = ProviderConfigResolver.resolveFormatting(
            preset = FormattingProviderPreset.CUSTOM,
            customBaseUrl = "",
            customModel = "some-model",
            apiKey = "key"
        )

        assertTrue(resolved.baseUrl.isBlank())
    }

    @Test
    fun `fromName falls back to GROQ for unknown or null preset names`() {
        assertEquals(FormattingProviderPreset.GROQ, FormattingProviderPreset.fromName(null))
        assertEquals(FormattingProviderPreset.GROQ, FormattingProviderPreset.fromName("not-a-real-preset"))
        assertEquals(TranscriptionProviderPreset.GROQ, TranscriptionProviderPreset.fromName("bogus"))
    }
}
