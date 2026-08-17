// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the two overlay windows. The floating widget is a persistent surface —
 * visible whenever enabled, including while a recording runs — and the recording pill
 * layers on top of it as a second, independent overlay. (The old behavior of swapping
 * one overlay for the other is retired.)
 */
@Singleton
class WindowOverlayCoordinator @Inject constructor(
    settingsProvider: SettingsProvider,
    serviceBridge: ServiceBridge
) {

    private val _widgetEnabled = MutableStateFlow(false)
    val widgetEnabled: StateFlow<Boolean> = _widgetEnabled.asStateFlow()

    /** True from recording start until the pill fully dismisses — the widget's "session active" signal. */
    private val _bubbleVisible = MutableStateFlow(false)
    val bubbleVisible: StateFlow<Boolean> = _bubbleVisible.asStateFlow()

    /** Hidden-apps suppression: true while a listed app is foreground — both widgets go away.
     *  Re-resolves the foreground app through the shared on-demand resolver on every window-state
     *  emission so it never clobbers on the IME window. */
    val widgetsHidden: StateFlow<Boolean> = combine(
        settingsProvider.dataStore.data.map { prefs -> prefs[DataStoreKeys.HIDDEN_APPS] ?: emptySet() },
        serviceBridge.foregroundPackage
    ) { hiddenApps, _ ->
        val foreground = ServiceLocator.currentForegroundApp()
        foreground != null && foreground in hiddenApps
    }.stateIn(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    fun setWidgetEnabled(enabled: Boolean) {
        _widgetEnabled.value = enabled
    }

    fun showBubble() {
        _bubbleVisible.value = true
    }

    fun hideBubble() {
        _bubbleVisible.value = false
    }
}
