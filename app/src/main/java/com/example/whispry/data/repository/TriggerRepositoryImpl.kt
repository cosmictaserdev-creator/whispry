package com.example.whispry.data.repository

import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
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
        
        if (hasActionButton()) {
            modes.add(TriggerMode.ActionButton)
        }

        // FloatingWidget is no longer offered: the widget coexists with every trigger
        // and is controlled by its own enable toggle in Settings.
        modes.add(TriggerMode.Manual)
        
        return modes
    }

    private fun hasActionButton(): Boolean {
        // Check for Samsung's Bixby key (KEYCODE_VOICE_ASSIST = 231)
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOICE_ASSIST)) return true
        
        // Check for generic action button
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_ASSIST)) return true
        
        // Check build properties for known action button devices
        val model = Build.MODEL.lowercase()
        val knownActionButtonDevices = listOf("pixel 8 pro", "pixel 9", "iphone") 
        return knownActionButtonDevices.any { model.contains(it) }
    }
}
