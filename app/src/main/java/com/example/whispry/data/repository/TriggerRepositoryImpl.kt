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

        modes.add(TriggerMode.VolumeButton)

        // ActionButton retired; FloatingWidget is no longer offered either (the widget coexists
        // with every trigger and is controlled by its own enable toggle in Settings).
        modes.add(TriggerMode.Manual)

        return modes
    }
}
