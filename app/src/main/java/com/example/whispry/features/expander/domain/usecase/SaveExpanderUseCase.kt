package com.example.whispry.features.expander.domain.usecase

import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import javax.inject.Inject

class SaveExpanderUseCase @Inject constructor(
    private val repository: TextExpanderRepository
) {
    suspend operator fun invoke(shortcut: String, expansion: String) {
        repository.saveExpander(shortcut, expansion)
    }
}
