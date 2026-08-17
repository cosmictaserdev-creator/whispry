// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.usecase


import com.example.whispry.domain.repository.TranscriptRepository
import javax.inject.Inject

class ClearTranscriptHistoryUseCase @Inject constructor(
    private val repository: TranscriptRepository
) {
    suspend operator fun invoke() =
        repository.clearAll()
}