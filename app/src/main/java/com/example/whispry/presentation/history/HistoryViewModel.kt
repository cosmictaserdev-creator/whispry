package com.example.whispry.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.domain.usecase.ClearTranscriptHistoryUseCase
import com.example.whispry.domain.usecase.CopyToClipboardUseCase
import com.example.whispry.domain.usecase.DeleteTranscriptUseCase
import com.example.whispry.domain.usecase.GetTranscriptHistoryUseCase
import com.example.whispry.domain.usecase.TogglePinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getTranscriptHistoryUseCase: GetTranscriptHistoryUseCase,
    private val deleteTranscriptUseCase: DeleteTranscriptUseCase,
    private val clearTranscriptHistoryUseCase: ClearTranscriptHistoryUseCase,
    private val copyToClipboardUseCase: CopyToClipboardUseCase,
    private val togglePinUseCase: TogglePinUseCase
) : ViewModel() {


    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(HistorySortOrder.DATE_DESC)

    init {
        loadTranscripts()
    }

    private fun loadTranscripts() {
        viewModelScope.launch {
            combine(
                getTranscriptHistoryUseCase(),
                _searchQuery.debounce(200).distinctUntilChanged(),
                _sortOrder
            ) { transcripts, query, order ->
                val filtered = if (query.isEmpty()) {
                    transcripts
                } else {
                    transcripts.filter { it.text.contains(query, ignoreCase = true) }
                }

                val sorted = when (order) {
                    HistorySortOrder.DATE_DESC -> filtered.sortedByDescending { it.timestampMs }
                    HistorySortOrder.DATE_ASC -> filtered.sortedBy { it.timestampMs }
                    HistorySortOrder.ALPHA_ASC -> filtered.sortedBy { it.text.lowercase() }
                    HistorySortOrder.ALPHA_DESC -> filtered.sortedByDescending { it.text.lowercase() }
                }
                
                Triple(transcripts, sorted, query)
            }
            .flowOn(Dispatchers.Default)
            .onStart { _state.update { it.copy(isLoading = true) } }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            .collect { (all, filtered, query) ->
                val pinned = filtered.filter { it.isPinned }
                val recent = filtered.filter { !it.isPinned }
                _state.update { 
                    it.copy(
                        isLoading = false,
                        transcripts = all,
                        filteredTranscripts = filtered,
                        pinnedTranscripts = pinned,
                        recentTranscripts = recent,
                        searchQuery = query,
                        error = null
                    )
                }
            }
        }
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.LoadTranscripts -> loadTranscripts()
            is HistoryIntent.DeleteTranscript -> deleteTranscript(intent.id)
            is HistoryIntent.ClearHistory -> clearHistory()
            is HistoryIntent.CopyToClipboard -> copyToClipboard(intent.text)
            is HistoryIntent.Search -> {
                _searchQuery.value = intent.query
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HistoryIntent.TogglePin -> togglePin(intent.id)
            is HistoryIntent.ChangeSortOrder -> {
                _sortOrder.value = intent.order
                _state.update { it.copy(sortOrder = intent.order) }
            }
        }
    }

    private fun togglePin(id: Long) {
        viewModelScope.launch {
            val transcript = _state.value.transcripts.find { it.id == id } ?: return@launch
            togglePinUseCase(id, !transcript.isPinned)
        }
    }

    private fun deleteTranscript(id: Long) {
        viewModelScope.launch {
            deleteTranscriptUseCase(id)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            clearTranscriptHistoryUseCase()
        }
    }

    private fun copyToClipboard(text: String) {
        viewModelScope.launch {
            copyToClipboardUseCase(text)
        }
    }
}
