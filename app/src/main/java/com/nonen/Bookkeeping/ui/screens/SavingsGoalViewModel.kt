package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.repo.SavingsGoalDetail
import com.nonen.Bookkeeping.data.repo.SavingsGoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalViewModel(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    val goals: StateFlow<List<SavingsGoalDetail>> = savingsGoalRepository.observeAllGoals()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun createGoal(
        name: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        targetDays: Int = 30,
        remark: String? = null,
    ) {
        scope.launch {
            val endTime = System.currentTimeMillis() + targetDays.toLong() * 24 * 3600 * 1000
            savingsGoalRepository.createGoal(
                name = name,
                targetAmount = targetAmount,
                initialAmount = initialAmount,
                endTime = endTime,
                remark = remark,
            )
        }
    }

    fun deposit(goalId: Long, amount: Double, remark: String? = null) {
        scope.launch {
            savingsGoalRepository.deposit(dreamId = goalId, amount = amount, remark = remark)
        }
    }

    fun withdraw(goalId: Long, amount: Double, remark: String? = null) {
        scope.launch {
            savingsGoalRepository.withdraw(dreamId = goalId, amount = amount, remark = remark)
        }
    }

    fun deleteGoal(goalId: Long) {
        scope.launch {
            savingsGoalRepository.deleteGoal(goalId)
        }
    }
}
