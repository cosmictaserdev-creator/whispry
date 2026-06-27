package com.example.whispry.domain.model

/**
 * A concrete app action decided by [com.example.whispry.domain.usecase.ProcessTranscriptUseCase].
 * The service layer turns this into an Android Intent and launches it.
 */
sealed interface VoiceAppAction {
    data class WebSearch(val query: String) : VoiceAppAction
    data class YoutubeSearch(val query: String) : VoiceAppAction
    data class MapsSearch(val query: String) : VoiceAppAction
    data class PlayStoreSearch(val query: String) : VoiceAppAction
    /** Launch [packageName]; [clipboardPayload] (the query) is copied for manual paste. */
    data class OpenApp(val packageName: String, val label: String, val clipboardPayload: String) : VoiceAppAction
    /**
     * Open a notes app with [text] pre-filled as a new note (via ACTION_SEND).
     * [packageName] pins a specific note app; blank shows the system chooser.
     */
    data class CreateNote(val text: String, val packageName: String, val label: String) : VoiceAppAction
}

/**
 * The decision the router makes for a finished transcript: either text to paste, or an app
 * action to run. [RunCommand] carries the original transcript so the service can fall back to
 * pasting it if the app turns out to be missing.
 */
sealed interface TranscriptOutcome {
    data class InsertText(val text: String) : TranscriptOutcome
    data class RunCommand(val action: VoiceAppAction, val originalTranscript: String) : TranscriptOutcome
}
