// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.hiddenapps.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.features.tone.presentation.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiddenAppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    val hiddenApps: StateFlow<Set<String>> = settingsProvider.dataStore.data
        .map { prefs -> prefs[DataStoreKeys.HIDDEN_APPS] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val ownPackage = context.packageName
            _installedApps.value = pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == ownPackage) null
                else AppInfo(packageName, resolveInfo.loadLabel(pm).toString())
            }.distinctBy { it.packageName }.sortedBy { it.appName }
        }
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            settingsProvider.dataStore.edit { prefs ->
                val current = prefs[DataStoreKeys.HIDDEN_APPS] ?: emptySet()
                prefs[DataStoreKeys.HIDDEN_APPS] = if (hidden) {
                    current + packageName
                } else {
                    current - packageName
                }
            }
        }
    }
}
