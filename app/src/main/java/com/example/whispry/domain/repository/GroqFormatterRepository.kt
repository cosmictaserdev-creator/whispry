// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.repository

import com.example.whispry.domain.util.Result

interface GroqFormatterRepository {
    /**
     * @param userContent the text delivered as the user message (may be delimiter-wrapped).
     * @param systemPrompt the assembled system prompt.
     * @param fallbackText clean text returned if formatting fails — never the wrapped [userContent],
     *        so users never see delimiter tags on error. Defaults to [userContent].
     */
    suspend fun formatText(
        userContent: String,
        systemPrompt: String,
        fallbackText: String = userContent
    ): Result<String>
}
