package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.TriggerMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.whispry.data.local.datasource.DataStoreKeys
import kotlinx.coroutines.flow.flowOf

class TriggerRepositoryTest {

    @Mock
    lateinit var settingsProvider: SettingsProvider

    @Mock
    lateinit var dataStore: DataStore<Preferences>

    lateinit var repository: TriggerRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(settingsProvider.dataStore).thenReturn(dataStore)
        repository = TriggerRepositoryImpl(settingsProvider)
    }

    @Test
    fun `test trigger mode persistence`() = runBlocking {
        val prefs = mutablePreferencesOf(DataStoreKeys.TRIGGER_MODE to "wake_word")
        `when`(dataStore.data).thenReturn(flowOf(prefs))
        
        val mode = repository.getActiveTriggerMode().first()
        assertEquals(TriggerMode.WakeWord, mode)
    }

    @Test
    fun `test default trigger mode`() = runBlocking {
        `when`(dataStore.data).thenReturn(flowOf(mutablePreferencesOf()))
        
        val mode = repository.getActiveTriggerMode().first()
        assertEquals(TriggerMode.VolumeButton, mode)
    }
}
