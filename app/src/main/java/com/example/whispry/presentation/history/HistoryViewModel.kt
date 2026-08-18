// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.usecase.*
import com.example.whispry.service.UploadTranscribeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            is HistoryIntent.UploadAudioFile -> uploadAudioFile(intent.uri, intent.displayName)
        }
    }

    /** Copies the picked audio file into app cache and enqueues background transcription, which
     *  survives the app being backgrounded or killed mid-upload; result lands in history via
     *  [UploadTranscribeWorker]. Runs in [viewModelScope], not a UI-scoped coroutine, so switching
     *  away from the History tab mid-copy doesn't silently cancel the upload. */
    private fun uploadAudioFile(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val extension = displayName.substringAfterLast('.', "").lowercase().ifBlank { "m4a" }
            val destDir = File(context.cacheDir, "uploads").apply { mkdirs() }
            val destFile = File(destDir, "upload_${System.currentTimeMillis()}.$extension")
            try {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
                if (!copied) {
                    destFile.delete()
                    return@launch
                }
            } catch (e: Exception) {
                destFile.delete()
                return@launch
            }

            val request = OneTimeWorkRequestBuilder<UploadTranscribeWorker>()
                .setInputData(
                    workDataOf(
                        UploadTranscribeWorker.KEY_FILE_PATH to destFile.absolutePath,
                        UploadTranscribeWorker.KEY_DISPLAY_NAME to displayName
                    )
                )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
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
