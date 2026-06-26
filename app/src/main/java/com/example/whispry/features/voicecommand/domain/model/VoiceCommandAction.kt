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
    OPEN_APP("Open App", "Launch an app, query copied to clipboard");

    val needsTargetApp: Boolean get() = this == OPEN_APP
}
