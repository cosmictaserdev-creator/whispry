package com.example.whispry.features.expander.domain.usecase

import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import javax.inject.Inject

class ExpandTextUseCase @Inject constructor(
    private val repository: TextExpanderRepository
) {
    suspend operator fun invoke(text: String): String {
        // Sanitize: trim, lowercase, remove trailing common punctuation
        val sanitized = text.trim()
            .lowercase()
            .removeSuffix(".")
            .removeSuffix("?")
            .removeSuffix("!")
            .removeSuffix(",")
            .trim()
            
        val expansion = repository.getExpansionForShortcut(sanitized)
        return expansion ?: text
    }
}
