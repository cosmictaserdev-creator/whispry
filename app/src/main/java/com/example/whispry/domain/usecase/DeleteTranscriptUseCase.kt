package com.example.whispry.domain.usecase

import com.example.whispry.domain.repository.TranscriptRepository
import javax.inject.Inject


class DeleteTranscriptUseCase @Inject constructor(
    private val repository: TranscriptRepository
) {
    suspend operator fun invoke(id: Long) =
        repository.deleteTranscript(id)
}