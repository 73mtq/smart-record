package com.nonen.Bookkeeping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.core.TodayWatcher
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(context: Context, private val repo: TransactionRepository) : ViewModel() {
    private val todayWatcher = TodayWatcher.of(context)

    // null 表示跟随系统日期；用户选择其他月份后保留选择。
    private val pinnedMonth = MutableStateFlow<YearMonth?>(null)
    private val selectedBookId = MutableStateFlow<Long?>(null)

    val month: StateFlow<YearMonth> =
        combine(todayWatcher.today, pinnedMonth) { today, pinned -> pinned ?: YearMonth.from(today) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.from(todayWatcher.today.value))

    val transactions: StateFlow<List<TransactionEntity>> =
        combine(month, selectedBookId) { month, book -> month to book }
            .flatMapLatest { (month, book) -> repo.observeMonth(month, book) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: StateFlow<LocalDate> = todayWatcher.today

    // 重新订阅时刷新日期，使跨天后的近七日查询使用当前窗口。
    val weekTransactions: StateFlow<List<TransactionEntity>> =
        todayWatcher.today
            .onStart {
                todayWatcher.refresh()
                emit(todayWatcher.today.value)
            }
            .combine(selectedBookId) { date, book -> weekRangeOf(date) to book }
            .flatMapLatest { (range, book) ->
                val (start, end) = range
                if (book == null) repo.observeRange(start, end) else repo.observeBookRange(start, end, book)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        todayWatcher.wakeUps()
            .onStart { todayWatcher.register(viewModelScope) }
            .launchIn(viewModelScope)
    }

    fun selectBook(id: Long) {
        selectedBookId.value = id
    }

    fun deleteSelected(ids: Set<Long>) {
        viewModelScope.launch {
            transactions.value.filter { it.id in ids }.forEach { repo.delete(it) }
        }
    }

    fun prevMonth() {
        pinnedMonth.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        pinnedMonth.value = month.value.plusMonths(1)
    }

    fun selectMonth(m: YearMonth) {
        pinnedMonth.value = pinnedMonthFor(m, todayWatcher.today.value)
    }

    fun backToCurrentMonth() {
        refreshToday()
        pinnedMonth.value = null
    }

    fun refreshToday() {
        todayWatcher.refresh()
    }
}
