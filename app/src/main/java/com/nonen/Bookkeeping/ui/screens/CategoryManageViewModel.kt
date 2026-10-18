package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 分类管理视图模型，支持支出/收入分类层级树维护、增删改查、排序及隐藏控制。
 */
class CategoryManageViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    /** 0 为支出，1 为收入 */
    var selectedType by mutableIntStateOf(0)
        private set

    var categories by mutableStateOf<List<CategoryEntity>>(emptyList())
        private set

    var expandedParentIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    init {
        loadCategories()
    }

    fun setType(type: Int) {
        if (selectedType == type) return
        selectedType = type
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.observeByType(selectedType).collectLatest { list ->
                categories = list
                // 默认展开前两个一级分类
                if (expandedParentIds.isEmpty()) {
                    expandedParentIds = list.filter { it.parentId == 0 }.take(2).map { it.categoryId }.toSet()
                }
            }
        }
    }

    fun toggleExpand(parentId: Int) {
        expandedParentIds = if (parentId in expandedParentIds) {
            expandedParentIds - parentId
        } else {
            expandedParentIds + parentId
        }
    }

    fun toggleHide(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.toggleHide(category.id, !category.hide)
        }
    }

    fun toggleBookVisibility(category: CategoryEntity, bookId: Long) {
        viewModelScope.launch {
            val isHidden = category.isHiddenInBook(bookId)
            categoryRepository.toggleBookVisibility(category.id, bookId, !isHidden)
        }
    }

    fun moveUp(category: CategoryEntity, siblings: List<CategoryEntity>) {
        val index = siblings.indexOfFirst { it.id == category.id }
        if (index <= 0) return
        val mutable = siblings.toMutableList()
        val temp = mutable[index - 1]
        mutable[index - 1] = mutable[index]
        mutable[index] = temp
        viewModelScope.launch {
            categoryRepository.updatePositionWeights(mutable)
        }
    }

    fun moveDown(category: CategoryEntity, siblings: List<CategoryEntity>) {
        val index = siblings.indexOfFirst { it.id == category.id }
        if (index < 0 || index >= siblings.size - 1) return
        val mutable = siblings.toMutableList()
        val temp = mutable[index + 1]
        mutable[index + 1] = mutable[index]
        mutable[index] = temp
        viewModelScope.launch {
            categoryRepository.updatePositionWeights(mutable)
        }
    }

    fun createCategory(name: String, parentId: Int, iconUrl: String, onDone: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.createCustomCategory(
                name = name.trim(),
                parentId = parentId,
                categoryType = selectedType,
                iconUrl = iconUrl.ifEmpty { "ic_parent_other" },
            )
            if (parentId != 0) {
                expandedParentIds = expandedParentIds + parentId
            }
            onDone()
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, newIconUrl: String, onDone: () -> Unit) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            categoryRepository.update(
                category.copy(
                    categoryName = newName.trim(),
                    iconUrl = newIconUrl.ifEmpty { category.iconUrl },
                ),
            )
            onDone()
        }
    }

    fun checkUsage(categoryId: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = categoryRepository.countTransactionsUsingCategory(categoryId)
            onResult(count)
        }
    }

    fun deleteCategory(category: CategoryEntity, unlinkTransactions: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            categoryRepository.deleteCustomCategory(
                id = category.id,
                categoryId = category.categoryId,
                unlinkTransactions = unlinkTransactions,
            )
            onDone()
        }
    }
}
