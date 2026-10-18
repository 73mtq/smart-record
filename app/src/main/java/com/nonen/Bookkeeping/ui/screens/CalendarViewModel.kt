package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.ReportRepository
import com.nonen.Bookkeeping.stats.CalendarDaySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/**
 * 收支日历视图模型：
 * 维护月份切换、日期选中、月度每日汇总聚合与当日账单流水。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val reportRepo: ReportRepository,
    externalScope: CoroutineScope? = null,
    initialDate: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _selectedDate = MutableStateFlow(initialDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.from(initialDate))
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    val monthSummaries: StateFlow<List<CalendarDaySummary>> =
        _currentMonth.flatMapLatest { ym ->
            reportRepo.observeCalendarMonth(ym)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDayTransactions: StateFlow<List<TransactionEntity>> =
        _selectedDate.flatMapLatest { date ->
            reportRepo.observeDayTransactions(date)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthTotalExpense: StateFlow<Double> =
        monthSummaries.map { list ->
            list.sumOf { it.totalExpense }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val monthTotalIncome: StateFlow<Double> =
        monthSummaries.map { list ->
            list.sumOf { it.totalIncome }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun prevMonth() {
        val prev = _currentMonth.value.minusMonths(1)
        _currentMonth.value = prev
        val curDay = _selectedDate.value.dayOfMonth
        val safeDay = curDay.coerceAtMost(prev.lengthOfMonth())
        _selectedDate.value = prev.atDay(safeDay)
    }

    fun nextMonth() {
        val next = _currentMonth.value.plusMonths(1)
        _currentMonth.value = next
        val curDay = _selectedDate.value.dayOfMonth
        val safeDay = curDay.coerceAtMost(next.lengthOfMonth())
        _selectedDate.value = next.atDay(safeDay)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val ym = YearMonth.from(date)
        if (ym != _currentMonth.value) {
            _currentMonth.value = ym
        }
    }

    fun goToToday(today: LocalDate = LocalDate.now()) {
        _selectedDate.value = today
        _currentMonth.value = YearMonth.from(today)
    }
}
