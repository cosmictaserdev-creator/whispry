package com.example.whispry.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
    private val togglePinUseCase: TogglePinUseCase,
    private val deleteTranscriptUseCase: DeleteTranscriptUseCase,
    private val clearTranscriptHistoryUseCase: ClearTranscriptHistoryUseCase,
    private val copyToClipboardUseCase: CopyToClipboardUseCase,
    private val formatTranscriptUseCase: FormatTranscriptUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _sortOrder = MutableStateFlow(HistorySortOrder.DATE_DESC)
    private val _searchQuery = MutableStateFlow("")

    init {
        loadTranscripts()
    }

    private fun loadTranscripts() {
        combine(
            transcriptRepository.getAllTranscripts(),
            _sortOrder,
            _searchQuery
        ) { transcripts, order, query ->
            val filtered = transcripts.filter {
                it.text.contains(query, ignoreCase = true) || 
                it.rawText.contains(query, ignoreCase = true)
            }.let { list ->
                when (order) {
                    HistorySortOrder.DATE_DESC -> list.sortedByDescending { it.timestampMs }
                    HistorySortOrder.DATE_ASC -> list.sortedBy { it.timestampMs }
                    HistorySortOrder.ALPHA_ASC -> list.sortedBy { it.text }
                    HistorySortOrder.ALPHA_DESC -> list.sortedByDescending { it.text }
                }
            }

            HistoryState(
                transcripts = transcripts,
                filteredTranscripts = filtered,
                pinnedTranscripts = transcripts.filter { it.isPinned },
                recentTranscripts = transcripts.take(10),
                searchQuery = query,
                sortOrder = order
            )
        }
        .onEach { newState -> _state.value = newState }
        .launchIn(viewModelScope)
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.LoadTranscripts -> loadTranscripts()
            is HistoryIntent.DeleteTranscript -> deleteTranscript(intent.id)
            is HistoryIntent.TogglePin -> togglePin(intent.id)
            is HistoryIntent.ClearHistory -> clearHistory()
            is HistoryIntent.CopyToClipboard -> copyToClipboard(intent.text)
            is HistoryIntent.Search -> {
                _searchQuery.value = intent.query
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HistoryIntent.ChangeSortOrder -> {
                _sortOrder.value = intent.order
                _state.update { it.copy(sortOrder = intent.order) }
            }
            is HistoryIntent.ChangePreset -> {
                viewModelScope.launch {
                    val transcriptId = intent.transcriptId
                    _state.update { it.copy(reformattingIds = it.reformattingIds + transcriptId) }
                    
                    try {
                        val transcript = transcriptRepository.getTranscriptById(transcriptId) ?: return@launch
                        val result = formatTranscriptUseCase(
                            transcript.rawText.ifBlank { transcript.text },
                            intent.preset,
                            skipAppAware = true
                        )
                        if (result is com.example.whispry.domain.util.Result.Success) {
                            val updatedTranscript = transcript.copy(
                                text = result.data,
                                preset = intent.preset.name
                            )
                            transcriptRepository.updateTranscript(updatedTranscript)
                            
                            // Also update selected transcript if it's the one being reformatted
                            if (_state.value.selectedTranscript?.id == transcriptId) {
                                _state.update { it.copy(selectedTranscript = updatedTranscript) }
                            }
                        }
                    } finally {
                        _state.update { it.copy(reformattingIds = it.reformattingIds - transcriptId) }
                    }
                }
            }
            is HistoryIntent.OpenDetail -> {
                _state.update { it.copy(selectedTranscript = intent.transcript) }
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
