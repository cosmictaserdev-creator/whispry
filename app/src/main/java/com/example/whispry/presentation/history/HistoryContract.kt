package com.example.whispry.presentation.history

import com.example.whispry.domain.model.Transcript

enum class HistorySortOrder {
    DATE_DESC, // Newest
    DATE_ASC,  // Oldest
    ALPHA_ASC, // A-Z
    ALPHA_DESC // Z-A
}

data class HistoryState(
    val transcripts: List<Transcript> = emptyList(),
    val filteredTranscripts: List<Transcript> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: HistorySortOrder = HistorySortOrder.DATE_DESC,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface HistoryIntent {
    data object LoadTranscripts : HistoryIntent
    data class DeleteTranscript(val id: Long) : HistoryIntent
    data class TogglePin(val id: Long) : HistoryIntent
    data object ClearHistory : HistoryIntent
    data class CopyToClipboard(val text: String) : HistoryIntent
    data class Search(val query: String) : HistoryIntent
    data class ChangeSortOrder(val order: HistorySortOrder) : HistoryIntent
}
