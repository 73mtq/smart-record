package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.db.BookDao
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 分类编辑视图模型：新增/编辑分类的名称、图标与「仅在当前账本显示」。
 * 一级分类与二级分类共用，parentId 为 0 表示一级分类。
 */
class CategoryEditViewModel(
    private val categoryRepository: CategoryRepository,
    private val bookDao: BookDao,
    private val settings: SettingsStore,
    private val categoryId: Int,
    private val parentId: Int,
    private val categoryType: Int,
) : ViewModel() {

    var name by mutableStateOf("")
    var iconUrl by mutableStateOf("lineIcon/ic_catering_rice.png")
    var hideInOtherBooks by mutableStateOf(false)
    var category by mutableStateOf<CategoryEntity?>(null)
        private set
    var loading by mutableStateOf(categoryId != 0)
        private set

    private var bookIds: List<Long> = emptyList()

    init {
        viewModelScope.launch {
            bookIds = bookDao.getAll().map { it.bookId }
            val currentBook = settings.selectedBookId.first()
            if (categoryId != 0) {
                val found = categoryRepository.getAll()
                    .firstOrNull { it.categoryId == categoryId && it.parentId == parentId }
                category = found
                if (found != null) {
                    name = found.categoryName
                    iconUrl = found.iconUrl
                    val others = bookIds.filter { it != currentBook }
                    hideInOtherBooks = others.isNotEmpty() && others.all { found.isHiddenInBook(it) }
                }
            }
            loading = false
        }
    }

    fun save(onDone: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val currentBook = settings.selectedBookId.first()
            val others = bookIds.filter { it != currentBook }
            val hideBook = if (hideInOtherBooks && others.isNotEmpty()) others.joinToString(",") else null
            val existing = category
            if (existing == null) {
                val created = categoryRepository.createCustomCategory(
                    name = trimmed,
                    parentId = parentId,
                    categoryType = categoryType,
                    iconUrl = iconUrl,
                )
                categoryRepository.update(created.copy(hideBook = hideBook))
            } else {
                categoryRepository.update(existing.copy(categoryName = trimmed, iconUrl = iconUrl, hideBook = hideBook))
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val existing = category ?: return
        viewModelScope.launch {
            categoryRepository.deleteCustomCategory(existing.id, existing.categoryId)
            onDone()
        }
    }
}
