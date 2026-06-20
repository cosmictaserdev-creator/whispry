package com.example.whispry.data.local.datasource

import androidx.datastore.preferences.core.*

object DataStoreKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val DOUBLE_PRESS_INTERVAL = longPreferencesKey("double_press_interval")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val CUSTOM_VOCABULARY = stringPreferencesKey("custom_vocabulary")
    val TEMPERATURE = floatPreferencesKey("temperature")
    val BUBBLE_SIZE = stringPreferencesKey("bubble_size")
    val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
    val ACCENT_COLOR = stringPreferencesKey("accent_color")

    // Bug 1: Smart Suppression
    val SMART_TRIGGER_SUPPRESSION = booleanPreferencesKey("smart_trigger_suppression")

    // Feature 1: Floating Widget
    val FLOATING_WIDGET_ENABLED = booleanPreferencesKey("floating_widget_enabled")

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

    // Feature 5: Sound System
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val SOUND_START = stringPreferencesKey("sound_start")
    val SOUND_SUCCESS = stringPreferencesKey("sound_success")
    val SOUND_ERROR = stringPreferencesKey("sound_error")

    // Feature 4: Mini Bubble
    val PROCESSING_MINI_MODE = booleanPreferencesKey("processing_mini_mode")
    val GLASS_NAVBAR = booleanPreferencesKey("glass_navbar")
    val GLASS_LIQUID_BACKDROP = booleanPreferencesKey("glass_liquid_backdrop")

    // Bubble Position
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
}
