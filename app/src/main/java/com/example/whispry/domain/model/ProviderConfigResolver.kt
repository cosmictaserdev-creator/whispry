package com.example.whispry.domain.model

/**
 * Pure resolution of a stored provider selection (curated preset, or Custom with user-typed
 * fields) into the concrete base URL/model/key an HTTP call needs. No networking, no DataStore —
 * testable as a plain JVM unit test, same as [com.example.whispry.service.WidgetGestureResolver].
 */
object ProviderConfigResolver {

    fun resolveTranscription(
        preset: TranscriptionProviderPreset,
        customBaseUrl: String,
        customModel: String,
        apiKey: String
    ): ResolvedProviderConfig = resolve(
        isCustom = preset == TranscriptionProviderPreset.CUSTOM,
        presetBaseUrl = preset.baseUrl,
        presetModel = preset.defaultModel,
        customBaseUrl = customBaseUrl,
        customModel = customModel,
        apiKey = apiKey
    )

    fun resolveFormatting(
        preset: FormattingProviderPreset,
        customBaseUrl: String,
        customModel: String,
        apiKey: String
    ): ResolvedProviderConfig = resolve(
        isCustom = preset == FormattingProviderPreset.CUSTOM,
        presetBaseUrl = preset.baseUrl,
        presetModel = preset.defaultModel,
        customBaseUrl = customBaseUrl,
        customModel = customModel,
        apiKey = apiKey
    )

    private fun resolve(
        isCustom: Boolean,
        presetBaseUrl: String,
        presetModel: String,
        customBaseUrl: String,
        customModel: String,
        apiKey: String
    ): ResolvedProviderConfig {
        val baseUrl = if (isCustom) customBaseUrl else presetBaseUrl
        val model = if (isCustom) customModel else presetModel
        return ResolvedProviderConfig(
            // A blank base URL (Custom preset with nothing typed yet) must stay blank so the
            // caller's isBlank() "endpoint not set" guard actually fires — appending "/" to ""
            // would produce "/", which is no longer blank.
            baseUrl = if (baseUrl.isBlank() || baseUrl.endsWith("/")) baseUrl else "$baseUrl/",
            model = model,
            apiKey = apiKey
        )
    }
}
