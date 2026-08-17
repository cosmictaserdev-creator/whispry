// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.repository

import com.example.whispry.domain.model.TriggerMode
import kotlinx.coroutines.flow.Flow

interface TriggerRepository {
    fun getActiveTriggerMode(): Flow<TriggerMode>
    suspend fun setTriggerMode(mode: TriggerMode)
    fun getAvailableTriggerModes(): List<TriggerMode>
}
