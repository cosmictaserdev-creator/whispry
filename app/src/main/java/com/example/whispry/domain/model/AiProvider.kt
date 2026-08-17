// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.model

/**
 * Curated OpenAI-compatible providers for the transcription (speech-to-text) step. Not every
 * provider offers a transcription endpoint, so this list is deliberately narrower than
 * [FormattingProviderPreset]. CUSTOM lets an advanced user point at any OpenAI-compatible
 * endpoint (including a local/LAN server) by typing their own base URL and model.
 */
enum class TranscriptionProviderPreset(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String
) {
    GROQ("Groq", "https://api.groq.com/openai/v1/", "whisper-large-v3"),
    OPENAI("OpenAI", "https://api.openai.com/v1/", "whisper-1"),
    CUSTOM("Custom", "", "");

    companion object {
        fun fromName(name: String?): TranscriptionProviderPreset =
            entries.find { it.name == name } ?: GROQ
    }
}

/**
 * Curated OpenAI-compatible providers for the formatting (transcript cleanup/preset LLM) step.
 * CUSTOM lets an advanced user point at any OpenAI-compatible endpoint (including a local/LAN
 * server) by typing their own base URL and model.
 */
enum class FormattingProviderPreset(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String
) {
    GROQ("Groq", "https://api.groq.com/openai/v1/", "openai/gpt-oss-120b"),
    OPENAI("OpenAI", "https://api.openai.com/v1/", "gpt-4o-mini"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/", "openai/gpt-4o-mini"),
    TOGETHER("Together", "https://api.together.xyz/v1/", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
    CUSTOM("Custom", "", "");

    companion object {
        fun fromName(name: String?): FormattingProviderPreset =
            entries.find { it.name == name } ?: GROQ
    }
}

/** Fully resolved connection details for one step's request, ready to hand to the HTTP client. */
data class ResolvedProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String
)
