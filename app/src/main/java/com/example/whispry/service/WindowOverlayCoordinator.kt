package com.example.whispry.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindowOverlayCoordinator @Inject constructor() {
    
    sealed class OverlayType {
        object None : OverlayType()
        object Bubble : OverlayType()
        object Widget : OverlayType()
    }

    private val _visibleOverlay = MutableStateFlow<OverlayType>(OverlayType.None)
    val visibleOverlay: StateFlow<OverlayType> = _visibleOverlay.asStateFlow()

    private var widgetEnabled = true

    fun setWidgetEnabled(enabled: Boolean) {
        widgetEnabled = enabled
        if (!enabled && _visibleOverlay.value == OverlayType.Widget) {
            _visibleOverlay.value = OverlayType.None
        } else if (enabled && _visibleOverlay.value == OverlayType.None) {
            _visibleOverlay.value = OverlayType.Widget
        }
    }

    fun showBubble() {
        _visibleOverlay.value = OverlayType.Bubble
    }

    fun hideBubble() {
        if (widgetEnabled) {
            _visibleOverlay.value = OverlayType.Widget
        } else {
            _visibleOverlay.value = OverlayType.None
        }
    }
}
