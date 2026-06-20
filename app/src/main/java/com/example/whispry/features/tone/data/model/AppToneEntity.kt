package com.example.whispry.features.tone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_tone_mappings")
data class AppToneEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val presetName: String, // Matches OutputPreset name (e.g. "INTELLIGENT_FORMAT", "NONE", etc.)
    val customPromptOverride: String = "" // Optional custom prompt if presetName is "CUSTOM"
)
