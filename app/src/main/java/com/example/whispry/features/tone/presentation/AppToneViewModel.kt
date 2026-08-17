// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.tone.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.tone.domain.usecase.DeleteAppToneUseCase
import com.example.whispry.features.tone.domain.usecase.GetAppTonesUseCase
import com.example.whispry.features.tone.domain.usecase.SaveAppToneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val appName: String
)

@HiltViewModel
class AppToneViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getAppTonesUseCase: GetAppTonesUseCase,
    private val saveAppToneUseCase: SaveAppToneUseCase,
    private val deleteAppToneUseCase: DeleteAppToneUseCase
) : ViewModel() {

    val appTones: StateFlow<List<AppToneEntity>> = getAppTonesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            // Query for launchable apps, filtering out our own app package
            val ownPackage = context.packageName
            val apps = pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == ownPackage) null else {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    AppInfo(packageName, appName)
                }
            }.distinctBy { it.packageName }.sortedBy { it.appName }

            _installedApps.value = apps
        }
    }

    fun saveAppTone(packageName: String, appName: String, presetName: String, customPromptOverride: String = "") {
        if (packageName.isBlank() || appName.isBlank() || presetName.isBlank()) return
        viewModelScope.launch {
            saveAppToneUseCase(AppToneEntity(packageName, appName, presetName, customPromptOverride))
        }
    }

    fun deleteAppTone(packageName: String) {
        viewModelScope.launch {
            deleteAppToneUseCase(packageName)
        }
    }
}
