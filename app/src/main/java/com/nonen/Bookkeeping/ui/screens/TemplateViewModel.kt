package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateViewModel(
    private val templateRepository: TemplateRepository,
    selectedBookId: Flow<Long>,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    val templates: StateFlow<List<ModuleBillEntity>> = selectedBookId.flatMapLatest { templateRepository.observeAll(it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun deleteTemplate(id: Long) {
        scope.launch {
            templateRepository.deleteTemplate(id)
        }
    }

    fun applyTemplate(id: Long, onDone: (() -> Unit)? = null) {
        scope.launch {
            templateRepository.applyTemplate(id)
            onDone?.invoke()
        }
    }
}
