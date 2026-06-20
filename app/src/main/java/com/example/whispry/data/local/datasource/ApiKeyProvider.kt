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

    fun getFingerprint(): String? = prefs.getString(KEY_FINGERPRINT, null)
    fun saveFingerprint(fp: String) = prefs.edit { putString(KEY_FINGERPRINT, fp) }
    fun clearFingerprint() = prefs.edit { remove(KEY_FINGERPRINT) }

    companion object {
        private const val KEY_API_KEY = "groq_api_key"
        private const val KEY_FINGERPRINT = "voice_fingerprint_v1"
    }
}