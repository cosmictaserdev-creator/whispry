// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.domain.repository

import com.example.whispry.features.expander.data.model.TextExpanderEntity
import kotlinx.coroutines.flow.Flow

interface TextExpanderRepository {
    fun getAllExpanders(): Flow<List<TextExpanderEntity>>
    suspend fun getExpansionForShortcut(shortcut: String): String?
    suspend fun saveExpander(shortcut: String, expansion: String)
    suspend fun deleteExpander(expander: TextExpanderEntity)
}
