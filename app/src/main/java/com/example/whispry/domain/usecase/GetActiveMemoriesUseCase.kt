// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.usecase

import com.example.whispry.data.local.db.MemoryEntity
import com.example.whispry.domain.repository.MemoryRepository
import javax.inject.Inject

class GetActiveMemoriesUseCase @Inject constructor(
    private val repository: MemoryRepository
) {
    suspend operator fun invoke(): List<MemoryEntity> = repository.getActiveMemories()
}
