package com.example.whispry.domain.model

/**
 * Languages whose Whisper transcript comes back in a native script that most speakers actually
 * type in Latin letters day-to-day (Hinglish, Arabizi, Urdu-Roman, Russian translit, Greeklish,
 * Finglish). Each carries the Unicode range used both to build the prompt and to guardrail-check
 * the model didn't regress to native script.
 */
enum class TransliterationLanguage(
    val languageCode: String,
    val displayName: String,
    val scriptName: String,
    private val nativeScriptRange: Regex
) {
    HINDI("hi", "Hindi", "Devanagari", Regex("[ऀ-ॿ]")),
    ARABIC("ar", "Arabic", "Arabic", Regex("[؀-ۿݐ-ݿﭐ-﷿ﹰ-﻿]")),
    URDU("ur", "Urdu", "Arabic", Regex("[؀-ۿݐ-ݿﭐ-﷿ﹰ-﻿]")),
    RUSSIAN("ru", "Russian", "Cyrillic", Regex("[Ѐ-ӿ]")),
    GREEK("el", "Greek", "Greek", Regex("[Ͱ-Ͽ]")),
    PERSIAN("fa", "Persian", "Arabic", Regex("[؀-ۿݐ-ݿﭐ-﷿ﹰ-﻿]"));

    fun containsNativeScript(text: String): Boolean = nativeScriptRange.containsMatchIn(text)

    companion object {
        fun fromCode(code: String): TransliterationLanguage? = entries.firstOrNull { it.languageCode == code }
    }
}
