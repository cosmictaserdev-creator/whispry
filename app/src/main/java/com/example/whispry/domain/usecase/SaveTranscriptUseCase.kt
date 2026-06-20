package com.example.whispry.domain.usecase

import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.repository.TranscriptRepository
import javax.inject.Inject


class SaveTranscriptUseCase @Inject constructor(
    val repository: TranscriptRepository
)  {

    suspend operator fun invoke(
        text: String,
        durationMs: Long,
        languageCode: String = "en",
        rawText: String = "",
        preset: String = "NONE"
    ) {
        repository.saveTranscript(
            text = text,
            rawText = rawText,
            durationMs = durationMs,
            languageCode = languageCode,
            preset = preset
        )
    }
}