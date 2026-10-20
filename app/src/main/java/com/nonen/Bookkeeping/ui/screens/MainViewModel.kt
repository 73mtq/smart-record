package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.data.db.BookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab { HOME, CALENDAR, ASSET, STATISTICS, MY }

class MainViewModel(private val container: AppContainer) : ViewModel() {
    val books = container.bookDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedBookId = container.settings.selectedBookId.stateIn(viewModelScope, SharingStarted.Eagerly, 1L)
    val navigationLabelsEnabled = container.settings.navigationLabelsEnabled

    fun selectBook(book: BookEntity) {
        viewModelScope.launch { container.settings.setSelectedBookId(book.bookId) }
    }

    fun createBook(name: String) {
        require(name.isNotBlank())
        viewModelScope.launch {
            val id = container.bookDao.insert(BookEntity(bookName = name.trim()))
            container.settings.setSelectedBookId(id)
        }
    }
}
