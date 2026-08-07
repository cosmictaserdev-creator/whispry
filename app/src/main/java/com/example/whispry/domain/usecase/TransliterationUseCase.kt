package com.example.whispry.domain.usecase

import com.example.whispry.domain.model.TransliterationLanguage
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject

/**
 * Whisper has no romanized output mode for these languages — a language hint still transcribes
 * in the native script. This romanizes that transcript via the formatting LLM instead, reusing
 * [FormatTranscriptUseCase]'s content-boundary guard so the model reshapes the text rather than
 * replying to it.
 *
 * Guards against the model regressing to native script (or answering instead of transliterating)
 * by checking the output script and retrying once before falling back to the raw transcript —
 * never worse than doing nothing.
 */
class TransliterationUseCase @Inject constructor(
    private val groqFormatterRepository: GroqFormatterRepository
) {
    suspend operator fun invoke(rawText: String, language: TransliterationLanguage): Result<String> {
        if (rawText.isBlank()) return Result.Success(rawText)

        val wrapped =
            "${FormatTranscriptUseCase.TRANSCRIPT_OPEN}\n$rawText\n${FormatTranscriptUseCase.TRANSCRIPT_CLOSE}"
        val prompt = "${systemPrompt(language)}\n\n${FormatTranscriptUseCase.ANTI_ANSWER_GUARD}"

        repeat(2) { attempt ->
            val result = groqFormatterRepository.formatText(
                userContent = wrapped,
                systemPrompt = prompt,
                fallbackText = rawText
            )
            val clean = (result as? Result.Success)?.data
            if (clean != null && !language.containsNativeScript(clean)) return result
            if (attempt == 1) return Result.Success(rawText)
        }
        return Result.Success(rawText)
    }

    private fun systemPrompt(language: TransliterationLanguage): String = """
        Transliterate this ${language.displayName} voice transcript from ${language.scriptName} script into Roman letters — the way ${language.displayName} speakers casually type it in Latin script chat. This is a script conversion, not a translation.

        Rules:
        - Convert each ${language.scriptName}-script word to its common romanized spelling, word for word, same order.
        - Never translate, paraphrase, summarize, or add/remove/reorder words — the output must have the exact same words and meaning as the input, only the script changes.
        - Use the standard, most common romanized spelling for each word (as speakers actually type it), not an invented or overly phonetic one.
        - Leave any word already in Latin script exactly as spoken — don't re-spell it or swap in a synonym.
        - Keep punctuation and sentence structure exactly as in the original.

        Output only the transliterated transcript, nothing else — no notes, no explanation.
    """.trimIndent()
}
