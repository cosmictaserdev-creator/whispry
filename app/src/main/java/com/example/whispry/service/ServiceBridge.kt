package com.example.whispry.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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