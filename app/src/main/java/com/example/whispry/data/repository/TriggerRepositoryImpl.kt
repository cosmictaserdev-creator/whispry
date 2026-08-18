// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.repository

import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.TriggerMode
import com.example.whispry.domain.repository.TriggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerRepositoryImpl @Inject constructor(
    private val settingsProvider: SettingsProvider
) : TriggerRepository {

    override fun getActiveTriggerMode(): Flow<TriggerMode> {
        return settingsProvider.dataStore.data.map {
            TriggerMode.fromStringId(it[DataStoreKeys.TRIGGER_MODE])
        }
    }

    override suspend fun setTriggerMode(mode: TriggerMode) {
        settingsProvider.dataStore.edit {
            it[DataStoreKeys.TRIGGER_MODE] = mode.toStringId()
        }
    }

    override fun getAvailableTriggerModes(): List<TriggerMode> {
        val modes = mutableListOf<TriggerMode>()

        // Manual is the recommended default (recording via the keyboard widget / in-app button).
        // Volume Button is a demoted opt-in for users who want physical-button control.
        modes.add(TriggerMode.Manual)

        // ActionButton retired; FloatingWidget is no longer offered either (the widget coexists
        // with every trigger and is controlled by its own enable toggle in Settings).
        // Volume Button hidden from Settings (Play Store accessibility-service review risk) —
        // TriggerService.onKeyEvent still handles it so existing users with it persisted as their
        // active mode keep working, it's just no longer selectable going forward.
        // modes.add(TriggerMode.VolumeButton)

        return modes
    }
}
