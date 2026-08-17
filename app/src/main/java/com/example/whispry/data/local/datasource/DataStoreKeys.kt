// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.local.datasource

import androidx.datastore.preferences.core.*

object DataStoreKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val DOUBLE_PRESS_INTERVAL = longPreferencesKey("double_press_interval")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    /** Last onboarding screen the user was on, so leaving mid-flow resumes there instead of restarting at Intro. Cleared on completion. */
    val ONBOARDING_RESUME_ROUTE = stringPreferencesKey("onboarding_resume_route")
    val CUSTOM_VOCABULARY = stringPreferencesKey("custom_vocabulary")
    val TEMPERATURE = floatPreferencesKey("temperature")
    val BUBBLE_SIZE = stringPreferencesKey("bubble_size")
    val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
    // Faster pill/widget animation timing - still animated, just abbreviated (not a snap-to-instant).
    val INSTANT_MODE_ENABLED = booleanPreferencesKey("instant_mode_enabled")

    // Bug 1: Smart Suppression
    val SMART_TRIGGER_SUPPRESSION = booleanPreferencesKey("smart_trigger_suppression")

    // Feature 1: Floating Widget
    val FLOATING_WIDGET_ENABLED = booleanPreferencesKey("floating_widget_enabled")

    /**
     * Default for [FLOATING_WIDGET_ENABLED]. Off by default: the volume-key trigger (accessibility)
     * is the primary path, so the widget is opt-in and never appears on screen uninvited.
     */
    const val DEFAULT_FLOATING_WIDGET_ENABLED = false

    // Feature 2 & 6: Wake Word
    val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    val WAKE_WORD_PHRASE = stringPreferencesKey("wake_word_phrase")
    val WAKE_WORD_MODE = stringPreferencesKey("wake_word_mode")
    val WAKE_WORD_SCREEN_ONLY = booleanPreferencesKey("wake_word_screen_only")
    val TRAINING_COMPLETED_SAMPLES = intPreferencesKey("training_completed_samples")

    // Feature 3: Multi-Trigger
    val TRIGGER_MODE = stringPreferencesKey("trigger_mode")
    val CONSUME_VOLUME_KEYS = booleanPreferencesKey("consume_volume_keys")
    val SINGLE_PRESS_TRIGGER = booleanPreferencesKey("single_press_trigger")

    /**
     * Default for [SINGLE_PRESS_TRIGGER]. `true` = press-and-hold (hold the key while speaking) is
     * the default gesture; double-press-then-hold remains available by toggling this off in Settings.
     */
    const val DEFAULT_SINGLE_PRESS_TRIGGER = true

    // Plain press-and-hold arming delay (hands-free OFF): how long the key must be held before
    // recording starts, so a quick tap still passes through as a normal volume press. Separate
    // from HANDS_FREE_ARMING_DELAY_MS below, which only applies when hands-free is on.
    val SINGLE_PRESS_HOLD_DELAY_MS = longPreferencesKey("single_press_hold_delay_ms")
    const val DEFAULT_SINGLE_PRESS_HOLD_DELAY_MS = 450L

    // Hands-free trigger: press to start, press again to stop (no hold).
    val HANDS_FREE_MODE = booleanPreferencesKey("hands_free_mode")

    // Hands-free single-press arming delay: how long a press must be held before it starts a
    // recording, so a quick tap can act as a normal key press instead. Matches the legacy
    // single-press hold logic's own delay by default.
    val HANDS_FREE_ARMING_DELAY_MS = longPreferencesKey("hands_free_arming_delay_ms")
    const val DEFAULT_HANDS_FREE_ARMING_DELAY_MS = 450L

    // Feature 5: Sound System
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val SOUND_START = stringPreferencesKey("sound_start")
    val SOUND_SUCCESS = stringPreferencesKey("sound_success")
    val SOUND_ERROR = stringPreferencesKey("sound_error")

    // Feature 4: Mini Bubble
    val PROCESSING_MINI_MODE = booleanPreferencesKey("processing_mini_mode")
    val GLASS_NAVBAR = booleanPreferencesKey("glass_navbar")
    val GLASS_LIQUID_BACKDROP = booleanPreferencesKey("glass_liquid_backdrop")

    // Bubble Position (stored as integer percentage 0-100 of safe bounds)
    val BUBBLE_POSITION_X = intPreferencesKey("bubble_position_x")
    val BUBBLE_POSITION_Y = intPreferencesKey("bubble_position_y")

    // Trigger Key
    val TRIGGER_VOLUME_KEY = stringPreferencesKey("trigger_volume_key")

    // Retention Policy
    val RETENTION_POLICY = stringPreferencesKey("retention_policy")

    // Audio Ducking
    val DUCKING_ENABLED = booleanPreferencesKey("ducking_enabled")
    val DUCKING_PERCENT = intPreferencesKey("ducking_percent")

    // Output Preset
    val DEFAULT_OUTPUT_PRESET = stringPreferencesKey("default_output_preset")
    val CUSTOM_AI_INSTRUCTIONS = stringPreferencesKey("custom_ai_instructions")
    val APP_AWARE_TONE_ENABLED = booleanPreferencesKey("app_aware_tone_enabled")

    // Translation target language (for the Translate preset)
    val TRANSLATE_TARGET_LANGUAGE = stringPreferencesKey("translate_target_language")

    // Voice Commands + expand/insert first-word router (global on/off)
    val VOICE_COMMANDS_ENABLED = booleanPreferencesKey("voice_commands_enabled")

    // One-time seeding of default expanders / commands / my-info rows
    val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")

    // Universal Press Actions: assign single/double volume-key press to an action (opt-in).
    val PRESS_ACTIONS_ENABLED = booleanPreferencesKey("press_actions_enabled")
    val SINGLE_PRESS_ACTION = stringPreferencesKey("single_press_action")
    val DOUBLE_PRESS_ACTION = stringPreferencesKey("double_press_action")

    // First-visit coach-marks that have already been shown (stored by CoachMark.name).
    val COACH_MARKS_SEEN = stringSetPreferencesKey("coach_marks_seen")

    // ------------------------------------------------------------------
    // Floating widget (physical switch). Own position keys — deliberately
    // NOT shared with the recording pill's BUBBLE_POSITION_X/Y.
    // ------------------------------------------------------------------
    val WIDGET_POSITION_X = intPreferencesKey("widget_position_x")
    val WIDGET_POSITION_Y = intPreferencesKey("widget_position_y")

    /**
     * How far (dp) the widget window sits inward from the physical screen edge. OEM gesture nav
     * (Realme UI/ColorOS, Poco/HyperOS) ignores systemGestureExclusionRects and owns any horizontal
     * swipe that starts in the edge zone, so the widget needs to physically clear it for the
     * swipe-to-reveal gesture to win. Values: 0 = Flush, 12 = Default, 24 = Wide.
     */
    val WIDGET_EDGE_CLEARANCE = intPreferencesKey("widget_edge_clearance")
    const val DEFAULT_WIDGET_EDGE_CLEARANCE = 12

    val WIDGET_BASE_HEIGHT_DP = intPreferencesKey("widget_base_height_dp")     // inner-face height
    val WIDGET_PROTRUSION_DP = intPreferencesKey("widget_protrusion_dp")       // visible width off the edge
    val WIDGET_IDLE_OPACITY_PCT = intPreferencesKey("widget_idle_opacity_pct")
    val WIDGET_FADE_DELAY_MS = longPreferencesKey("widget_fade_delay_ms")
    val WIDGET_ARMING_DELAY_MS = longPreferencesKey("widget_arming_delay_ms")  // anti-accident slider
    val WIDGET_CUSTOM_TRIGGERS = booleanPreferencesKey("widget_custom_triggers") // false = "same as default"
    val WIDGET_SINGLE_TAP_ACTION = stringPreferencesKey("widget_single_tap_action")
    val WIDGET_DOUBLE_TAP_ACTION = stringPreferencesKey("widget_double_tap_action")
    val WIDGET_SOUND_MUTED = booleanPreferencesKey("widget_sound_muted")
    val WIDGET_REDUCED_MOTION = stringPreferencesKey("widget_reduced_motion")  // AUTO | ON | OFF

    /** When true (default), the widget nudges up above the IME instead of staying parked under the keyboard. */
    val WIDGET_AVOID_KEYBOARD = booleanPreferencesKey("widget_avoid_keyboard")
    const val DEFAULT_WIDGET_AVOID_KEYBOARD = true

    /** Packages that suppress both widgets entirely while foreground (gaming/fullscreen apps). */
    val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")

    /** Opt-in premium reminder notifications from PremiumReminderWorker. */
    val PREMIUM_REMINDERS_ENABLED = booleanPreferencesKey("premium_reminders_enabled")
    const val DEFAULT_PREMIUM_REMINDERS_ENABLED = false

    /** One-shot migration of the retired TriggerMode.FloatingWidget (see FloatingWidgetManager). */
    val WIDGET_TRIGGER_MODE_MIGRATED = booleanPreferencesKey("widget_trigger_mode_migrated")

    // ------------------------------------------------------------------
    // Keyboard logo: a standalone record toggle that floats above the soft keyboard.
    // Not a trigger mode — its own enable toggle; press = start, press again = stop.
    // ------------------------------------------------------------------
    val KEYBOARD_LOGO_ENABLED = booleanPreferencesKey("keyboard_logo_enabled")
    // On by default: the keyboard widget is the primary trigger, replacing the volume key.
    const val DEFAULT_KEYBOARD_LOGO_ENABLED = true

    // X position as a percentage (0-100) of the safe width; right-side default.
    val KEYBOARD_LOGO_X = intPreferencesKey("keyboard_logo_x")
    const val DEFAULT_KEYBOARD_LOGO_X = 80

    // Vertical offset (px) between the IME's top edge and the logo's bottom. The logo rides the
    // keyboard: this offset is what "anchors" it to the keyboard as it slides in/out. Persisted
    // so the user's drag placement survives across keyboard opens.
    val KEYBOARD_LOGO_Y_OFFSET = intPreferencesKey("keyboard_logo_y_offset")

    // ------------------------------------------------------------------
    // Multi-provider AI support: transcription and formatting each resolve independently.
    // Empty preset = GROQ (today's default, unchanged behavior for existing users).
    // ------------------------------------------------------------------
    val TRANSCRIPTION_PROVIDER_PRESET = stringPreferencesKey("transcription_provider_preset")
    val TRANSCRIPTION_CUSTOM_BASE_URL = stringPreferencesKey("transcription_custom_base_url")
    val TRANSCRIPTION_CUSTOM_MODEL = stringPreferencesKey("transcription_custom_model")

    val FORMATTING_PROVIDER_PRESET = stringPreferencesKey("formatting_provider_preset")
    val FORMATTING_CUSTOM_BASE_URL = stringPreferencesKey("formatting_custom_base_url")
    val FORMATTING_CUSTOM_MODEL = stringPreferencesKey("formatting_custom_model")

    // Hinglish output: romanizes a Hindi ("hi") transcript via the formatting LLM instead of
    // leaving it in Devanagari script. Whisper itself has no transliteration mode.
    val HINGLISH_OUTPUT_ENABLED = booleanPreferencesKey("hinglish_output_enabled")
}
