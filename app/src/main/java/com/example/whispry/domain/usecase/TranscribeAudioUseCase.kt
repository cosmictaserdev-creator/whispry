package com.example.whispry.domain.usecase

import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject


class TranscribeAudioUseCase @Inject constructor(
    private val audioRepository: AudioRepository,
    private val transcriptRepository: TranscriptRepository
) {
    suspend operator fun invoke(
        audioFilePath: String,
        durationMs: Long,
        language: String? = null
    ): Result<String> {

        val finalLanguageCode = language ?: "en"

        // Step 1 — send audio to Groq, get transcript text back
        val transcribeResult = audioRepository.transcribeAudio(
            audioFilePath = audioFilePath,
            languageCode = finalLanguageCode
        )

        // Step 2 — if transcription succeeded, save it to local DB
        if (transcribeResult is Result.Success) {
            transcriptRepository.saveTranscript(
                text = transcribeResult.data,
                durationMs = durationMs,
                languageCode = finalLanguageCode,
            )
        }

        // Step 3 — return the result either way
        // UI gets the text immediately if success, error message if not
        return transcribeResult
    }
}