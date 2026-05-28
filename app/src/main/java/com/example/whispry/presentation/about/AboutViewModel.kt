package com.example.whispry.presentation.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.domain.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AboutState(
    val totalWords: Int = 0,
    val totalRecordings: Int = 0,
    val totalTimeSavedSeconds: Long = 0,
    val version: String = "1.0.0",
    val buildNumber: String = "12"
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val repository: TranscriptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AboutState())
    val state: StateFlow<AboutState> = _state.asStateFlow()

    init {
        repository.getAllTranscripts().onEach { transcripts ->
            val words = transcripts.sumOf { it.text.split("\\s+".toRegex()).size }
            // Assuming 130 wpm typing speed: time saved = (words / 130) * 60 seconds
            val timeSaved = if (words > 0) (words.toDouble() / 130.0 * 60.0).toLong() else 0L
            
            _state.update {
                it.copy(
                    totalWords = words,
                    totalRecordings = transcripts.size,
                    totalTimeSavedSeconds = timeSaved
                )
            }
        }.launchIn(viewModelScope)
    }
}
