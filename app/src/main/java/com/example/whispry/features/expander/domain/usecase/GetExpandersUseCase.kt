// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.domain.usecase

import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpandersUseCase @Inject constructor(
    private val repository: TextExpanderRepository
) {
    operator fun invoke(): Flow<List<TextExpanderEntity>> = repository.getAllExpanders()
}
