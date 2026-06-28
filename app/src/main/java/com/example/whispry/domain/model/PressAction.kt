package com.example.whispry.domain.model

/**
 * What a trigger press does, when the universal "Press Actions" feature is enabled. The user can
 * assign one of these to a single press and another to a double press (same or different).
 *
 * In Press-Actions mode recording is tap-to-toggle (press to start, press again to stop), so it is
 * inherently hands-free — no holding the key.
 *
 * Serialized to a compact string for DataStore / intent extras.
 */
sealed interface PressAction {
    /** Transcribe and paste using the default preset + voice-command routing (normal behavior). */
    data object Normal : PressAction

    /** Transcribe and format with a specific [preset], then paste. */
    data class Preset(val preset: OutputPreset) : PressAction

    /** Transcribe, open [packageName], and copy the text to the clipboard for a one-tap paste. */
    data class OpenApp(val packageName: String, val label: String) : PressAction

    fun serialize(): String = when (this) {
        Normal -> NORMAL
        is Preset -> "$PRESET${preset.name}"
        is OpenApp -> "$OPEN_APP$packageName$SEP$label"
    }

    /** A short human-readable label for the assigned action. */
    fun displayLabel(): String = when (this) {
        Normal -> "Transcribe & paste"
        is Preset -> "Format: ${preset.displayName}"
        is OpenApp -> "Open $label + paste"
    }

    companion object {
        private const val NORMAL = "NORMAL"
        private const val PRESET = "PRESET:"
        private const val OPEN_APP = "OPEN_APP:"
        private const val SEP = "|"

        fun parse(raw: String?): PressAction {
            if (raw.isNullOrBlank()) return Normal
            return when {
                raw == NORMAL -> Normal
                raw.startsWith(PRESET) -> {
                    val preset = try {
                        OutputPreset.valueOf(raw.removePrefix(PRESET))
                    } catch (e: Exception) {
                        OutputPreset.NONE
                    }
                    Preset(preset)
                }
                raw.startsWith(OPEN_APP) -> {
                    val rest = raw.removePrefix(OPEN_APP)
                    val parts = rest.split(SEP, limit = 2)
                    val pkg = parts.getOrElse(0) { "" }
                    OpenApp(pkg, parts.getOrElse(1) { pkg })
                }
                else -> Normal
            }
        }
    }
}
