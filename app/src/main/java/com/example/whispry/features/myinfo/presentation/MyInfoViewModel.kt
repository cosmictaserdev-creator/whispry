package com.example.whispry.features.myinfo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import com.example.whispry.features.myinfo.domain.repository.MyInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyInfoViewModel @Inject constructor(
    private val repository: MyInfoRepository
) : ViewModel() {

    val items: StateFlow<List<MyInfoEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** "expand"/"insert" are reserved first-word prefixes and cannot be keys. */
    fun isReserved(key: String): Boolean {
        val k = key.trim().lowercase()
        return k == "expand" || k == "insert"
    }

    fun save(key: String, value: String) {
        if (key.isBlank() || isReserved(key)) return
        viewModelScope.launch { repository.save(key, value) }
    }

    fun delete(entity: MyInfoEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }
}
