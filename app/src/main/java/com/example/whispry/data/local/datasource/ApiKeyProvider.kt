package com.example.whispry.data.local.datasource

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class ApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApiKey(): String =
        prefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiKey(apiKey: String) =
        prefs.edit { putString(KEY_API_KEY, apiKey) }

    fun clearApiKey() =
        prefs.edit { remove(KEY_API_KEY) }

    /**
     * Per-step provider keys, falling back to the original single [getApiKey] ONLY while that
     * step is still on its default GROQ preset — so an existing user's one Groq key keeps working
     * until they explicitly switch providers. Once a different provider is selected, an unset
     * per-step key must NOT fall back to the Groq key, since that would send it to a different
     * provider's server as a bearer token (a credential leak, not just an auth failure).
     */
    fun getTranscriptionApiKey(preset: com.example.whispry.domain.model.TranscriptionProviderPreset): String {
        val stepKey = prefs.getString(KEY_TRANSCRIPTION_API_KEY, "") ?: ""
        if (stepKey.isNotBlank()) return stepKey
        return if (preset == com.example.whispry.domain.model.TranscriptionProviderPreset.GROQ) getApiKey() else ""
    }

    fun saveTranscriptionApiKey(apiKey: String) =
        prefs.edit { putString(KEY_TRANSCRIPTION_API_KEY, apiKey) }

    fun getFormattingApiKey(preset: com.example.whispry.domain.model.FormattingProviderPreset): String {
        val stepKey = prefs.getString(KEY_FORMATTING_API_KEY, "") ?: ""
        if (stepKey.isNotBlank()) return stepKey
        return if (preset == com.example.whispry.domain.model.FormattingProviderPreset.GROQ) getApiKey() else ""
    }

    fun saveFormattingApiKey(apiKey: String) =
        prefs.edit { putString(KEY_FORMATTING_API_KEY, apiKey) }

    /** Raw stored per-step key, no GROQ fallback — for displaying in its own Settings field. */
    fun getRawTranscriptionApiKey(): String = prefs.getString(KEY_TRANSCRIPTION_API_KEY, "") ?: ""
    fun getRawFormattingApiKey(): String = prefs.getString(KEY_FORMATTING_API_KEY, "") ?: ""

    fun getFingerprint(): String? = prefs.getString(KEY_FINGERPRINT, null)
    fun saveFingerprint(fp: String) = prefs.edit { putString(KEY_FINGERPRINT, fp) }
    fun clearFingerprint() = prefs.edit { remove(KEY_FINGERPRINT) }

    companion object {
        private const val KEY_API_KEY = "groq_api_key"
        private const val KEY_TRANSCRIPTION_API_KEY = "transcription_api_key"
        private const val KEY_FORMATTING_API_KEY = "formatting_api_key"
        private const val KEY_FINGERPRINT = "voice_fingerprint_v1"
    }
}