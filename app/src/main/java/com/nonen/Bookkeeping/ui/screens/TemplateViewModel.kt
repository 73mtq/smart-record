package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateViewModel(
    private val templateRepository: TemplateRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    val templates: StateFlow<List<ModuleBillEntity>> = templateRepository.observeAll(bookId = 1L)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun createTemplate(
        name: String,
        cost: Double,
        moduleType: Int = ModuleBillEntity.MODULE_TYPE_BILL,
        parentCategoryId: Int? = 7,
        childCategoryId: Int? = 707,
        assetId: Long = 1L,
        remark: String? = null,
    ) {
        scope.launch {
            templateRepository.createTemplate(
                ModuleBillEntity(
                    templateName = name,
                    cost = cost,
                    moduleType = moduleType,
                    parentCategoryId = parentCategoryId,
                    childCategoryId = childCategoryId,
                    assetId = assetId,
                    remark = remark,
                ),
            )
        }
    }

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
