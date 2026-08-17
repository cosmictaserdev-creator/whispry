// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.model

/**
 * Curated set of target languages for the Translate preset's OUTPUT.
 *
 * Intentionally broader than the Whisper speech-INPUT list (LANGUAGES in the picker):
 * the LLM can write far more languages than Whisper can transcribe. Stored/used as the
 * plain English language name, which is injected directly into the translation prompt.
 */
object TranslationLanguages {
    const val DEFAULT = "English"

    val all: List<String> = listOf(
        "English",
        "Hindi",
        "Spanish",
        "French",
        "German",
        "Italian",
        "Portuguese",
        "Dutch",
        "Russian",
        "Ukrainian",
        "Polish",
        "Turkish",
        "Arabic",
        "Hebrew",
        "Persian",
        "Mandarin Chinese",
        "Cantonese",
        "Japanese",
        "Korean",
        "Vietnamese",
        "Thai",
        "Indonesian",
        "Malay",
        "Filipino",
        "Bengali",
        "Tamil",
        "Telugu",
        "Marathi",
        "Urdu",
        "Greek",
        "Swedish",
        "Norwegian",
        "Danish",
        "Finnish",
        "Czech",
        "Romanian",
        "Hungarian",
        "Swahili"
    )
}
