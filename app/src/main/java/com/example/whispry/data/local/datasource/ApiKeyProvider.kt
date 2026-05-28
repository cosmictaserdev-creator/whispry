package com.example.whispry.data.local.datasource

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()

    fun clearApiKey() =
        prefs.edit().remove(KEY_API_KEY).apply()

    companion object {
        private const val KEY_API_KEY = "groq_api_key"
    }
}