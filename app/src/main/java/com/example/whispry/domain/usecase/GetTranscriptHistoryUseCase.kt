// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.usecase


import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTranscriptHistoryUseCase @Inject constructor(
    private val repository: TranscriptRepository
) {
    operator fun invoke(): Flow<List<Transcript>> =
        repository.getAllTranscripts()
}