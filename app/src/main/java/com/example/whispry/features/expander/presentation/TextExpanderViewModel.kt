// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.features.expander.domain.usecase.DeleteExpanderUseCase
import com.example.whispry.features.expander.domain.usecase.GetExpandersUseCase
import com.example.whispry.features.expander.domain.usecase.SaveExpanderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TextExpanderViewModel @Inject constructor(
    getExpandersUseCase: GetExpandersUseCase,
    private val saveExpanderUseCase: SaveExpanderUseCase,
    private val deleteExpanderUseCase: DeleteExpanderUseCase
) : ViewModel() {

    val expanders: StateFlow<List<TextExpanderEntity>> = getExpandersUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveExpander(shortcut: String, expansion: String) {
        if (shortcut.isBlank() || expansion.isBlank()) return
        viewModelScope.launch {
            saveExpanderUseCase(shortcut.trim().lowercase(), expansion)
        }
    }

    fun deleteExpander(expander: TextExpanderEntity) {
        viewModelScope.launch {
            deleteExpanderUseCase(expander)
        }
    }
}
