package com.example.whispry.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Presets : Route

    @Serializable
    data object FavoriteDetails : Route

    @Serializable
    data object RecentDetails : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object About : Route

    @Serializable
    data object TextExpander : Route

    @Serializable
    data object AppTones : Route

    @Serializable
    data object Memory : Route

    @Serializable
    data object MyInfo : Route

    @Serializable
    data object VoiceCommands : Route

    companion object {
        fun fromDeepLinkHost(host: String): Route {
            return when (host.lowercase()) {
                "home" -> Home
                "history", "library" -> Library
                "presets" -> Presets
                "settings" -> Settings
                "about" -> About
                "text-expander" -> TextExpander
                "app-tones" -> AppTones
                "memory" -> Memory
                "my-info" -> MyInfo
                "voice-commands" -> VoiceCommands
                else -> Home
            }
        }
    }
}
