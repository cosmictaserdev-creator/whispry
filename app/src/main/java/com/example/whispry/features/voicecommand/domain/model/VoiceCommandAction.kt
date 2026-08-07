package com.example.whispry.features.voicecommand.domain.model

/**
 * What a voice command does with the words that follow its trigger.
 *
 * Search actions fire the matching app/intent with the remaining words as the query.
 * OPEN_APP just launches the chosen app and leaves the query on the clipboard for
 * manual paste (the target field isn't focused while the app is still opening).
 */
enum class VoiceCommandAction(val label: String, val description: String) {
    WEB_SEARCH("Web Search", "Search the web (Chrome if installed)"),
    YOUTUBE_SEARCH("YouTube Search", "Search on YouTube"),
    MAPS_SEARCH("Maps Search", "Search on Google Maps"),
    PLAYSTORE_SEARCH("Play Store Search", "Search on the Play Store"),
    NEW_NOTE("New Note", "Open a notes app with your idea pre-filled"),
    OPEN_APP("Open App", "Launch an app, query copied to clipboard"),
    CALCULATE("Calculate", "Opens the calculator and types out the operation"),
    CALL("Call", "Opens the dialer with the number ready to call"),
    SMS("Text", "Opens messages with your words pre-filled"),
    SET_ALARM("Set Alarm", "Opens the alarm screen"),
    SET_TIMER("Set Timer", "Starts a countdown timer"),
    CALENDAR_EVENT("Calendar Event", "Opens a new calendar event with a title"),
    EMAIL("Email", "Opens email with your words pre-filled");

    /** OPEN_APP must have a target app; NEW_NOTE can optionally pin one (else a chooser). */
    val needsTargetApp: Boolean get() = this == OPEN_APP

    /** Whether the user may pick a specific app for this action (required or optional). */
    val supportsTargetApp: Boolean get() = this == OPEN_APP || this == NEW_NOTE
}
