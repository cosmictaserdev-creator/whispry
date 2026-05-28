package com.example.whispry.data.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // ------------------------------------------------------------------
    // Keys
    // ------------------------------------------------------------------

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val DOUBLE_PRESS_INTERVAL = longPreferencesKey("double_press_interval")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CUSTOM_VOCABULARY = stringPreferencesKey("custom_vocabulary")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val BUBBLE_SIZE = stringPreferencesKey("bubble_size")
        val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
    }

    // ------------------------------------------------------------------
    // Flows
    // ------------------------------------------------------------------

    val language: Flow<String> = dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val doublePressInterval: Flow<Long> = dataStore.data.map { it[Keys.DOUBLE_PRESS_INTERVAL] ?: 400L }
    val hapticFeedback: Flow<Boolean> = dataStore.data.map { it[Keys.HAPTIC_FEEDBACK] ?: true }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val customVocabulary: Flow<String> = dataStore.data.map { it[Keys.CUSTOM_VOCABULARY] ?: "" }
    val temperature: Flow<Float> = dataStore.data.map { it[Keys.TEMPERATURE] ?: 0.0f }
    val bubbleSize: Flow<String> = dataStore.data.map { it[Keys.BUBBLE_SIZE] ?: "Medium" }
    val autoStartBoot: Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_START_BOOT] ?: true }
    val accentColor: Flow<String> = dataStore.data.map { it[Keys.ACCENT_COLOR] ?: "Purple" }

    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------

    suspend fun setLanguage(value: String) = dataStore.edit { it[Keys.LANGUAGE] = value }
    suspend fun setDoublePressInterval(value: Long) = dataStore.edit { it[Keys.DOUBLE_PRESS_INTERVAL] = value }
    suspend fun setHapticFeedback(value: Boolean) = dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = value }
    suspend fun setOnboardingCompleted(value: Boolean) = dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = value }
    suspend fun setCustomVocabulary(value: String) = dataStore.edit { it[Keys.CUSTOM_VOCABULARY] = value }
    suspend fun setTemperature(value: Float) = dataStore.edit { it[Keys.TEMPERATURE] = value }
    suspend fun setBubbleSize(value: String) = dataStore.edit { it[Keys.BUBBLE_SIZE] = value }
    suspend fun setAutoStartBoot(value: Boolean) = dataStore.edit { it[Keys.AUTO_START_BOOT] = value }
    suspend fun setAccentColor(value: String) = dataStore.edit { it[Keys.ACCENT_COLOR] = value }
}
