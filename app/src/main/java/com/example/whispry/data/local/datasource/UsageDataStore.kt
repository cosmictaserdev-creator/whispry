package com.example.whispry.data.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.whispry.domain.model.UsageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.usageStore: DataStore<Preferences> by preferencesDataStore(name = "usage")

@Singleton
class UsageDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DAILY_REQUESTS = intPreferencesKey("daily_requests")
        val DAILY_WORDS = intPreferencesKey("daily_words")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    }

    private val todayStr: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    suspend fun ensureNewDay() {
        context.usageStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_RESET_DATE]
            if (lastDate == null || lastDate != todayStr) {
                prefs[Keys.DAILY_REQUESTS] = 0
                prefs[Keys.DAILY_WORDS] = 0
                prefs[Keys.LAST_RESET_DATE] = todayStr
            }
        }
    }

    suspend fun incrementRequests(count: Int) {
        context.usageStore.edit { prefs ->
            val current = prefs[Keys.DAILY_REQUESTS] ?: 0
            prefs[Keys.DAILY_REQUESTS] = current + count
        }
    }

    suspend fun incrementWords(count: Int) {
        context.usageStore.edit { prefs ->
            val current = prefs[Keys.DAILY_WORDS] ?: 0
            prefs[Keys.DAILY_WORDS] = current + count
        }
    }

    fun observeUsage(): Flow<UsageInfo> {
        return context.usageStore.data.map { prefs ->
            UsageInfo(
                requestsUsed = prefs[Keys.DAILY_REQUESTS] ?: 0,
                wordsUsed = prefs[Keys.DAILY_WORDS] ?: 0
            )
        }
    }

    suspend fun getUsage(): UsageInfo {
        val prefs = context.usageStore.data.first()
        return UsageInfo(
            requestsUsed = prefs[Keys.DAILY_REQUESTS] ?: 0,
            wordsUsed = prefs[Keys.DAILY_WORDS] ?: 0
        )
    }
}
