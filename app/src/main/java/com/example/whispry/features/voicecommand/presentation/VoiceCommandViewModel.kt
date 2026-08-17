// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.voicecommand.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import com.example.whispry.features.voicecommand.domain.model.VoiceCommandAction
import com.example.whispry.features.voicecommand.domain.repository.VoiceCommandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledApp(val label: String, val packageName: String)

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VoiceCommandRepository
) : ViewModel() {

    val commands: StateFlow<List<VoiceCommandEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    /** "expand"/"insert" are reserved first-word prefixes and cannot be triggers. */
    fun isReserved(word: String): Boolean {
        val w = word.trim().lowercase()
        return w == "expand" || w == "insert"
    }

    /** Loads launchable apps (CATEGORY_LAUNCHER) for the Open-App picker. No QUERY_ALL_PACKAGES. */
    fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .mapNotNull { ri ->
                        val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                        val label = ri.loadLabel(pm).toString()
                        InstalledApp(label, pkg)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun save(
        triggerWord: String,
        action: VoiceCommandAction,
        targetPackage: String,
        targetLabel: String
    ) {
        if (triggerWord.isBlank() || isReserved(triggerWord)) return
        if (action.needsTargetApp && targetPackage.isBlank()) return
        viewModelScope.launch {
            repository.save(triggerWord, action.name, targetPackage, targetLabel)
        }
    }

    fun delete(entity: VoiceCommandEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}
