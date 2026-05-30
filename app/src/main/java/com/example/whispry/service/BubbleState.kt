package com.example.whispry.service

sealed interface BubbleState {
    object Idle : BubbleState
    object Listening : BubbleState
    data class Processing(val miniMode: Boolean = false, val showCancelHint: Boolean = false) : BubbleState
    object Success : BubbleState
    data class Error(val message: String, val isNetworkError: Boolean = false) : BubbleState
}
