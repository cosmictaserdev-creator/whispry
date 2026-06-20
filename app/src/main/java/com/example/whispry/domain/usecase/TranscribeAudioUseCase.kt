package com.example.whispry.domain.usecase

import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject


class TranscribeAudioUseCase @Inject constructor(
    private val audioRepository: AudioRepository,
    private val transcriptRepository: TranscriptRepository,
    private val formatTranscriptUseCase: FormatTranscriptUseCase
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

        // Step 2 — format if preset is not NONE
        val finalText = if (outputPreset != OutputPreset.NONE) {
            val formatResult = formatTranscriptUseCase(rawText, outputPreset)
            if (formatResult is Result.Success) formatResult.data else rawText
        } else {
            rawText
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