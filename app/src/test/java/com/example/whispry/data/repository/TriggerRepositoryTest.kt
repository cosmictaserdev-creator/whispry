package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.TriggerMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.whispry.data.local.datasource.DataStoreKeys
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class TriggerRepositoryTest {

    private val settingsProvider = mockk<SettingsProvider>()
    private val dataStore = mockk<DataStore<Preferences>>()

    private lateinit var repository: TriggerRepositoryImpl

    @Before
    fun setup() {
        every { settingsProvider.dataStore } returns dataStore
        repository = TriggerRepositoryImpl(settingsProvider)
    }

    @Test
    fun `test trigger mode persistence`() = runBlocking {
        val prefs = mutablePreferencesOf(DataStoreKeys.TRIGGER_MODE to "wake_word")
        every { dataStore.data } returns flowOf(prefs)
        
        val mode = repository.getActiveTriggerMode().first()
        assertEquals(TriggerMode.WakeWord, mode)
    }

    @Test
    fun `test default trigger mode`() = runBlocking {
        every { dataStore.data } returns flowOf(mutablePreferencesOf())
        
        val mode = repository.getActiveTriggerMode().first()
        assertEquals(TriggerMode.VolumeButton, mode)
    }
}
