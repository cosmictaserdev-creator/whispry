package com.example.whispry.domain.usecase


import com.example.whispry.domain.repository.TranscriptRepository
import javax.inject.Inject

class ClearTranscriptHistoryUseCase @Inject constructor(
    private val repository: TranscriptRepository
) {
    suspend operator fun invoke() =
        repository.clearAll()
}