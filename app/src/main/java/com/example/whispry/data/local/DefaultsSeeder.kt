package com.example.whispry.data.local

import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import com.example.whispry.features.myinfo.domain.repository.MyInfoRepository
import com.example.whispry.features.voicecommand.domain.model.VoiceCommandAction
import com.example.whispry.features.voicecommand.domain.repository.VoiceCommandRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds useful, fully-editable example rows the first time the app runs so the new
 * productivity features are self-explanatory. Guarded by a one-time DataStore flag.
 */
@Singleton
class DefaultsSeeder @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val expanderRepository: TextExpanderRepository,
    private val myInfoRepository: MyInfoRepository,
    private val voiceCommandRepository: VoiceCommandRepository
) {
    suspend fun seedIfNeeded() {
        val alreadySeeded = settingsProvider.dataStore.data.first()[DataStoreKeys.DEFAULTS_SEEDED] ?: false
        if (alreadySeeded) return

        // Text Expander — working snippets, usable immediately via "expand <shortcut>".
        expanderRepository.saveExpander("ty", "Thank you so much, I really appreciate it!")
        expanderRepository.saveExpander("omw", "On my way, see you soon!")
        expanderRepository.saveExpander("meet", "Could we schedule a quick meeting? What time works for you?")

        // My Info — labeled empty templates the user fills in (no fake placeholder values).
        myInfoRepository.save("address", "")
        myInfoRepository.save("email", "")
        myInfoRepository.save("phone", "")
        myInfoRepository.save("name", "")

        // Voice Commands — ready-to-use examples.
        voiceCommandRepository.save("search", VoiceCommandAction.WEB_SEARCH.name, "", "")
        voiceCommandRepository.save("chrome", VoiceCommandAction.WEB_SEARCH.name, "", "")
        voiceCommandRepository.save("youtube", VoiceCommandAction.YOUTUBE_SEARCH.name, "", "")
        voiceCommandRepository.save("maps", VoiceCommandAction.MAPS_SEARCH.name, "", "")

        settingsProvider.dataStore.edit { it[DataStoreKeys.DEFAULTS_SEEDED] = true }
    }
}
