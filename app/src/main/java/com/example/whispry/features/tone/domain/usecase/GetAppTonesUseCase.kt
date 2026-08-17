// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.tone.domain.usecase

import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.tone.domain.repository.AppToneRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppTonesUseCase @Inject constructor(
    private val repository: AppToneRepository
) {
    operator fun invoke(): Flow<List<AppToneEntity>> = repository.getAllAppTones()
}
