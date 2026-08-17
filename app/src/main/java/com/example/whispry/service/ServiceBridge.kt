// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.graphics.Rect
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceBridge @Inject constructor() {

    private val _triggerEvent = MutableSharedFlow<TriggerEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val triggerEvent: SharedFlow<TriggerEvent> = _triggerEvent.asSharedFlow()

    fun emit(event: TriggerEvent) {
        _triggerEvent.tryEmit(event)
    }

    /** Current soft-keyboard screen bounds (null when no keyboard is showing). Reported by the
     *  accessibility service; consumed by the keyboard-logo overlay. */
    private val _imeBounds = MutableStateFlow<Rect?>(null)
    val imeBounds: StateFlow<Rect?> = _imeBounds.asStateFlow()

    fun setImeBounds(bounds: Rect?) {
        _imeBounds.value = bounds
    }

    /** Foreground app package, updated on window-state changes by the accessibility service.
     *  Drives hidden-apps suppression (and is the source the on-demand resolver falls back to). */
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    fun setForegroundPackage(packageName: String?) {
        _foregroundPackage.value = packageName
    }

    sealed class TriggerEvent {
        object Idle : TriggerEvent()
        object RecordingStarted : TriggerEvent()
        object RecordingStopped : TriggerEvent()

        /** Abort the in-flight recording and discard it (widget drag-down cancel). */
        object RecordingCancelled : TriggerEvent()

        /**
         * The widget's drag-down cancel crossed (true) or un-crossed (false) its threshold;
         * the pill mirrors this as a red "release to cancel" state.
         */
        data class CancelArming(val armed: Boolean) : TriggerEvent()
        data class TranscriptionResult(val text: String) : TriggerEvent()
        data class TranscriptionFailed(val message: String) : TriggerEvent()
    }
}