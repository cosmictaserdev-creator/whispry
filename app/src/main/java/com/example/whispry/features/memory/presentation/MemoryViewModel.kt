package com.example.whispry.features.memory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.data.local.db.MemoryEntity
import com.example.whispry.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: MemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<MemoryEntity>> = repository.getAllMemoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMemory(key: String, value: String, category: String = "Personal") {
        viewModelScope.launch {
            val memory = MemoryEntity(key = key, value = value, category = category)
            repository.saveMemory(memory)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }
    
    fun toggleMemoryActive(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.saveMemory(memory.copy(isActive = !memory.isActive))
        }
    }
}
