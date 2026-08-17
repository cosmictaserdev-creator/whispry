// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.domain.usecase

import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import javax.inject.Inject

class DeleteExpanderUseCase @Inject constructor(
    private val repository: TextExpanderRepository
) {
    suspend operator fun invoke(expander: TextExpanderEntity) {
        repository.deleteExpander(expander)
    }
}
