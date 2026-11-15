package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.repo.SavingsGoalRepository
import kotlinx.coroutines.launch
import java.util.Locale

/** 愿望新增/编辑视图模型：名称、目标金额、已攒金额、备注与结束日期。 */
class DreamEditViewModel(
    private val repository: SavingsGoalRepository,
    private val dreamId: Long,
) : ViewModel() {

    var name by mutableStateOf("")
    var targetText by mutableStateOf("")
    var initialText by mutableStateOf("")
    var remark by mutableStateOf("")
    var endTime by mutableLongStateOf(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
    var dream by mutableStateOf<DreamEntity?>(null)
        private set
    var loading by mutableStateOf(dreamId != 0L)
        private set

    init {
        viewModelScope.launch {
            if (dreamId != 0L) {
                val existing = repository.getGoalById(dreamId)?.dream
                dream = existing
                if (existing != null) {
                    name = existing.dreamName
                    targetText = String.format(Locale.US, "%.2f", existing.totalNum)
                    initialText = String.format(Locale.US, "%.2f", existing.startNum)
                    remark = existing.remark.orEmpty()
                    endTime = existing.endTime
                }
            }
            loading = false
        }
    }

    fun save(onDone: () -> Unit) {
        val target = targetText.toDoubleOrNull()
        if (name.isBlank() || target == null || target <= 0.0) return
        viewModelScope.launch {
            val existing = dream
            if (existing == null) {
                repository.createGoal(
                    name = name.trim(),
                    targetAmount = target,
                    initialAmount = initialText.toDoubleOrNull() ?: 0.0,
                    endTime = endTime,
                    remark = remark.trim().ifEmpty { null },
                )
            } else {
                repository.updateGoal(
                    existing.copy(
                        dreamName = name.trim(),
                        totalNum = target,
                        startNum = initialText.toDoubleOrNull() ?: existing.startNum,
                        remark = remark.trim().ifEmpty { null },
                        endTime = endTime,
                    ),
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGoal(dreamId)
            onDone()
        }
    }
}
