package com.example.whispry.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the two overlay windows. The floating widget is a persistent surface —
 * visible whenever enabled, including while a recording runs — and the recording pill
 * layers on top of it as a second, independent overlay. (The old behavior of swapping
 * one overlay for the other is retired.)
 */
@Singleton
class WindowOverlayCoordinator @Inject constructor() {

    private val _widgetEnabled = MutableStateFlow(false)
    val widgetEnabled: StateFlow<Boolean> = _widgetEnabled.asStateFlow()

    /** True from recording start until the pill fully dismisses — the widget's "session active" signal. */
    private val _bubbleVisible = MutableStateFlow(false)
    val bubbleVisible: StateFlow<Boolean> = _bubbleVisible.asStateFlow()

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
