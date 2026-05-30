package com.example.whispry.service

data class FloatingWidgetState(
    val isExpanded: Boolean = false,
    val lastTranscript: String? = null,
    val serviceActive: Boolean = true
)
