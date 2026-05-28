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
        data class TranscriptionResult(val text: String) : TriggerEvent()
    }
}