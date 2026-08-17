// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.usecase

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.model.TranscriptOutcome
import com.example.whispry.domain.model.VoiceAppAction
import com.example.whispry.domain.util.Result
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import com.example.whispry.features.myinfo.domain.repository.MyInfoRepository
import com.example.whispry.features.voicecommand.domain.model.VoiceCommandAction
import com.example.whispry.features.voicecommand.domain.repository.VoiceCommandRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The single decision point for a finished transcript, used by every voice entry point.
 *
 * First-word router (only when voice commands are enabled):
 *  - "expand <key>"  -> Text Expander lookup; exact match pastes the expansion.
 *  - "insert <key>"  -> My Info lookup; exact match pastes the saved value.
 *  - "<command> ..."  -> matched voice command runs with the rest as its query.
 *  - anything else, or no exact match -> normal path (preset formatting), original text untouched.
 *
 * The router can only ever ADD behavior on an exact match; a miss falls through to normal
 * transcription so it can never corrupt ordinary dictation.
 */
class ProcessTranscriptUseCase @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val textExpanderRepository: TextExpanderRepository,
    private val myInfoRepository: MyInfoRepository,
    private val voiceCommandRepository: VoiceCommandRepository,
    private val formatTranscriptUseCase: FormatTranscriptUseCase
) {
    companion object {
        const val PREFIX_EXPAND = "expand"
        const val PREFIX_INSERT = "insert"
        private val PUNCT = charArrayOf('.', ',', '!', '?', ';', ':', '"', '\'')
        private val DURATION_REGEX = Regex("(\\d+)\\s*(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)\\b")
    }

    suspend operator fun invoke(
        rawText: String,
        preset: OutputPreset
    ): TranscriptOutcome {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return TranscriptOutcome.InsertText(rawText)

        if (settingsProvider.voiceCommandsEnabled.first()) {
            val tokens = trimmed.split(Regex("\\s+"))
            val firstWord = tokens.first().lowercase().trim(*PUNCT)
            val rest = tokens.drop(1).joinToString(" ").trim()

            when (firstWord) {
                PREFIX_EXPAND -> {
                    lookupKey(rest) { textExpanderRepository.getExpansionForShortcut(it) }
                        ?.let { return TranscriptOutcome.InsertText(it) }
                    // no match -> fall through to normal path
                }
                PREFIX_INSERT -> {
                    lookupKey(rest) { myInfoRepository.getValueForKey(it) }
                        ?.let { return TranscriptOutcome.InsertText(it) }
                    // no match -> fall through to normal path
                }
                else -> {
                    val command = voiceCommandRepository.getByTrigger(firstWord)
                    if (command != null) {
                        val action = buildAction(command.action, rest, command.targetPackage, command.targetAppLabel)
                        if (action != null) {
                            return TranscriptOutcome.RunCommand(action, originalTranscript = trimmed)
                        }
                    }
                    // no command -> fall through to normal path
                }
            }
        }

        // Normal path: format per the selected preset and paste.
        val formatted = (formatTranscriptUseCase(rawText, preset) as? Result.Success)?.data ?: rawText
        return TranscriptOutcome.InsertText(formatted)
    }

    /** Try the full remainder as a key, then fall back to just the second word. */
    private suspend fun lookupKey(rest: String, lookup: suspend (String) -> String?): String? {
        if (rest.isBlank()) return null
        val full = rest.lowercase().trim(*PUNCT).trim()
        lookup(full)?.let { return it }
        val secondWord = rest.split(Regex("\\s+")).firstOrNull()?.lowercase()?.trim(*PUNCT)
        if (!secondWord.isNullOrBlank() && secondWord != full) {
            lookup(secondWord)?.let { return it }
        }
        return null
    }

    private fun buildAction(
        actionName: String,
        query: String,
        targetPackage: String,
        targetLabel: String
    ): VoiceAppAction? {
        val action = try { VoiceCommandAction.valueOf(actionName) } catch (e: Exception) { return null }
        return when (action) {
            VoiceCommandAction.WEB_SEARCH -> VoiceAppAction.WebSearch(query)
            VoiceCommandAction.YOUTUBE_SEARCH -> VoiceAppAction.YoutubeSearch(query)
            VoiceCommandAction.MAPS_SEARCH -> VoiceAppAction.MapsSearch(query)
            VoiceCommandAction.PLAYSTORE_SEARCH -> VoiceAppAction.PlayStoreSearch(query)
            VoiceCommandAction.NEW_NOTE -> VoiceAppAction.CreateNote(query, targetPackage, targetLabel.ifBlank { "Notes" })
            VoiceCommandAction.OPEN_APP -> {
                if (targetPackage.isBlank()) null
                else VoiceAppAction.OpenApp(targetPackage, targetLabel.ifBlank { targetPackage }, query)
            }
            VoiceCommandAction.CALCULATE -> if (query.isBlank()) null else VoiceAppAction.Calculate(query)
            VoiceCommandAction.CALL -> VoiceAppAction.Call(query)
            VoiceCommandAction.SMS -> VoiceAppAction.Sms(query)
            VoiceCommandAction.SET_ALARM -> VoiceAppAction.SetAlarm(query)
            VoiceCommandAction.SET_TIMER -> VoiceAppAction.SetTimer(query, parseDurationSeconds(query))
            VoiceCommandAction.CALENDAR_EVENT -> VoiceAppAction.CalendarEvent(query)
            VoiceCommandAction.EMAIL -> VoiceAppAction.Email(query)
        }
    }

    /** "5 minutes" / "90 seconds" / "1 hour" -> seconds; null if no duration is found in the words. */
    private fun parseDurationSeconds(query: String): Int? {
        val match = DURATION_REGEX.find(query.lowercase()) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        val unitSeconds = when {
            match.groupValues[2].startsWith("h") -> 3600
            match.groupValues[2].startsWith("m") -> 60
            else -> 1
        }
        return amount * unitSeconds
    }
}
