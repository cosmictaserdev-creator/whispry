package com.example.whispry.domain.usecase

import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject

/**
 * Whisper has no Hinglish (Latin-script Hindi) output mode — a "hi" language hint still
 * transcribes in Devanagari. This romanizes that transcript via the formatting LLM instead,
 * reusing [FormatTranscriptUseCase]'s content-boundary guard so the model reshapes the text
 * rather than replying to it.
 */
class HinglishTransliterationUseCase @Inject constructor(
    private val groqFormatterRepository: GroqFormatterRepository
) {
    suspend operator fun invoke(rawText: String): Result<String> {
        if (rawText.isBlank()) return Result.Success(rawText)

        val wrapped =
            "${FormatTranscriptUseCase.TRANSCRIPT_OPEN}\n$rawText\n${FormatTranscriptUseCase.TRANSCRIPT_CLOSE}"
        val prompt = "$SYSTEM_PROMPT\n\n${FormatTranscriptUseCase.ANTI_ANSWER_GUARD}"

        return groqFormatterRepository.formatText(
            userContent = wrapped,
            systemPrompt = prompt,
            fallbackText = rawText
        )
    }

    private companion object {
        val SYSTEM_PROMPT = """
            Transliterate this Hindi voice transcript from Devanagari script into Roman letters (Hinglish) — the way Hindi speakers casually type it in English chat. This is a script conversion, not a translation.

            Rules:
            - Convert each Devanagari word to its common Hinglish spelling, word for word, same order. Examples: "कैसे हो" -> "kaise ho", "मुझे नहीं पता" -> "mujhe nahi pata", "ठीक है, चलते हैं" -> "theek hai, chalte hain".
            - Never translate, paraphrase, summarize, or add/remove/reorder words — the output must have the exact same words and meaning as the input, only the script changes.
            - Use the standard, most common Hinglish spelling for each word (as Hindi speakers actually type it), not an invented or overly phonetic one.
            - Leave any word already in English exactly as spoken — don't re-spell it or swap in a synonym.
            - Keep punctuation and sentence structure exactly as in the original.

            Output only the transliterated transcript, nothing else — no notes, no explanation.
        """.trimIndent()
    }
}
