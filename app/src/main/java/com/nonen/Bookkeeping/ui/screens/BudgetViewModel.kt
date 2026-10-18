package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.repo.BudgetOverview
import com.nonen.Bookkeeping.data.repo.BudgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val currentCal = Calendar.getInstance()
    private val _year = MutableStateFlow(currentCal.get(Calendar.YEAR))
    private val _month = MutableStateFlow(currentCal.get(Calendar.MONTH) + 1)

    val currentYear: StateFlow<Int> = _year
    val currentMonth: StateFlow<Int> = _month

    val budgetOverview: StateFlow<BudgetOverview> = _year.flatMapLatest { y ->
        _month.flatMapLatest { m ->
            budgetRepository.observeBudgetOverview(bookId = 1L, year = y, month = m)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetOverview(
            budgetId = 0L,
            year = _year.value,
            month = _month.value,
            totalBudget = 0.0,
            totalExpense = 0.0,
            remainingBudget = 0.0,
            usageRatio = 0f,
            isOverBudget = false,
            categoryDetails = emptyList(),
        ),
    )

    fun changeMonth(year: Int, month: Int) {
        _year.value = year
        _month.value = month
    }

    fun prevMonth() {
        if (_month.value == 1) {
            _year.value -= 1
            _month.value = 12
        } else {
            _month.value -= 1
        }
    }

    fun nextMonth() {
        if (_month.value == 12) {
            _year.value += 1
            _month.value = 1
        } else {
            _month.value += 1
        }
    }

    fun setTotalBudget(amount: Double, addNum: Double = 0.0) {
        scope.launch {
            budgetRepository.setMonthBudget(
                bookId = 1L,
                year = _year.value,
                month = _month.value,
                amount = amount,
                addNum = addNum,
            )
        }
    }

    fun setCategoryBudget(parentCategoryId: Int, childCategoryId: Int = 0, amount: Double) {
        scope.launch {
            val budget = budgetRepository.getOrInitMonthBudget(
                bookId = 1L,
                year = _year.value,
                month = _month.value,
            )
            budgetRepository.setCategoryBudget(
                budgetId = budget.budgetId,
                bookId = 1L,
                year = _year.value,
                month = _month.value,
                parentCategoryId = parentCategoryId,
                childCategoryId = childCategoryId,
                amount = amount,
            )
        }
    }

    fun deleteCategoryBudget(categoryBudgetId: Long) {
        scope.launch {
            budgetRepository.deleteCategoryBudget(categoryBudgetId)
        }
    }
}
