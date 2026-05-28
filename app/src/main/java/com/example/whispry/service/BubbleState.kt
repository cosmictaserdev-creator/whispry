package com.example.whispry.service

sealed interface BubbleState {
    object Idle : BubbleState
    object Listening : BubbleState
    object Loading : BubbleState
    object Success : BubbleState
    data class Error(val message: String? = null) : BubbleState
}
