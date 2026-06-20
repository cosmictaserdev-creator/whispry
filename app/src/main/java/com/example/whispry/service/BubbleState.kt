package com.example.whispry.service

import com.example.whispry.domain.model.OutputPreset

sealed interface BubbleState {
    object Idle : BubbleState
    object Listening : BubbleState
    data class Processing(val miniMode: Boolean = false, val showCancelHint: Boolean = false) : BubbleState
    data class Formatting(val preset: OutputPreset) : BubbleState
    object Success : BubbleState
    data class Error(val message: String, val isNetworkError: Boolean = false) : BubbleState
}
