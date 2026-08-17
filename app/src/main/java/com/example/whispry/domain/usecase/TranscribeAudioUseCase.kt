// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.usecase

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.model.TransliterationLanguage
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject


class TranscribeAudioUseCase @Inject constructor(
    private val audioRepository: AudioRepository,
    private val transcriptRepository: TranscriptRepository,
    private val formatTranscriptUseCase: FormatTranscriptUseCase,
    private val transliterationUseCase: TransliterationUseCase,
    private val settingsProvider: SettingsProvider
) {
    suspend operator fun invoke(
        audioFilePath: String,
        durationMs: Long,
        language: String? = null,
        outputPreset: OutputPreset = OutputPreset.NONE
    ): Result<String> {

        val finalLanguageCode = language ?: "en"

        // Step 1 — send audio to Groq, get transcript text back
        val transcribeResult = audioRepository.transcribeAudio(
            audioFilePath = audioFilePath,
            languageCode = finalLanguageCode
        )

        if (transcribeResult !is Result.Success) return transcribeResult

        val rawText = transcribeResult.data

        // Step 1.5 — romanize a native-script transcript before any further formatting, so a
        // downstream output preset always sees Latin-script text.
        val transliterationLanguage = TransliterationLanguage.fromCode(finalLanguageCode)
        val workingText = if (transliterationLanguage != null && settingsProvider.hinglishOutputEnabled.first()) {
            val transliterateResult = transliterationUseCase(rawText, transliterationLanguage)
            if (transliterateResult is Result.Success) transliterateResult.data else rawText
        } else {
            rawText
        }

        // Step 2 — format if preset is not NONE
        val finalText = if (outputPreset != OutputPreset.NONE) {
            val formatResult = formatTranscriptUseCase(workingText, outputPreset)
            if (formatResult is Result.Success) formatResult.data else workingText
        } else {
            workingText
        }

        // Step 3 — save to Room (save the FORMATTED text, not raw)
        transcriptRepository.saveTranscript(
            text = finalText,
            rawText = rawText,
            durationMs = durationMs,
            languageCode = finalLanguageCode,
            preset = outputPreset.name
        )

        return Result.Success(finalText)
    }
}