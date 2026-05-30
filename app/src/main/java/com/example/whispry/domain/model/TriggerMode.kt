package com.example.whispry.domain.model

sealed class TriggerMode {
    object VolumeButton : TriggerMode()
    object ActionButton : TriggerMode()   // OEM-specific physical button
    object WakeWord : TriggerMode()
    object FloatingWidget : TriggerMode() // tap the widget to record
    object Manual : TriggerMode()         // only tap in-app button
    
    fun toStringId(): String = when(this) {
        is VolumeButton -> "volume_button"
        is ActionButton -> "action_button"
        is WakeWord -> "wake_word"
        is FloatingWidget -> "floating_widget"
        is Manual -> "manual"
    }
    
    companion object {
        fun fromStringId(id: String?): TriggerMode = when(id) {
            "volume_button" -> VolumeButton
            "action_button" -> ActionButton
            "wake_word" -> WakeWord
            "floating_widget" -> FloatingWidget
            "manual" -> Manual
            else -> VolumeButton
        }
    }
}
