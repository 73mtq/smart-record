package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.repo.PeriodicRepository
import com.nonen.Bookkeeping.data.service.PeriodicTransactionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeriodicViewModel(
    private val periodicRepository: PeriodicRepository,
    private val periodicTransactionExecutor: PeriodicTransactionExecutor,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    val cycles: StateFlow<List<CycleEntity>> = periodicRepository.observeAllCycles(bookId = 1L)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun togglePause(cycleId: Long) {
        scope.launch {
            periodicRepository.togglePause(cycleId)
        }
    }

    fun deleteCycle(cycleId: Long) {
        scope.launch {
            periodicRepository.deleteCycle(cycleId)
        }
    }

    fun createCycle(
        cost: Double,
        cycleType: Int = CycleEntity.CYCLE_TYPE_BILL,
        dateMode: Int = CycleEntity.MODE_EVERY_MONTH,
        monthDay: Int = 1,
        monthLast: Boolean = false,
        weekdays: String? = null,
        num: Int = 1,
        assetId: Long = 1L,
        fromAssetId: Long = 1L,
        toAssetId: Long = 1L,
        parentCategoryId: Int? = 7,
        childCategoryId: Int? = 707,
        remark: String? = null,
    ) {
        scope.launch {
            val cycle = CycleEntity(
                cost = cost,
                cycleType = cycleType,
                dateMode = dateMode,
                monthDay = monthDay,
                monthLast = monthLast,
                weekdays = weekdays,
                num = num,
                assetId = assetId,
                fromAssetId = fromAssetId,
                toAssetId = toAssetId,
                parentCategoryId = parentCategoryId,
                childCategoryId = childCategoryId,
                remark = remark,
                addTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis(),
            )
            periodicRepository.createCycle(cycle)
            // 创建后立即检查并入账
            periodicTransactionExecutor.executePendingCycles()
        }
    }

    fun triggerExecution(onFinished: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = periodicTransactionExecutor.executePendingCycles()
            onFinished?.invoke(count)
        }
    }
}
