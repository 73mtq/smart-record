package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class StatisticsBillFilter(
    val bookId: Long,
    val start: LocalDate,
    val end: LocalDate,
    val income: Boolean,
    val category: String? = null,
    val subCategory: String? = null,
    val tag: String? = null,
) {
    init {
        require(!end.isBefore(start))
    }
}

class StatisticsBillListViewModel(
    repository: TransactionRepository,
    val filter: StatisticsBillFilter,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    val transactions = repository.observeBookRange(
        filter.start.atStartOfDay(zone).toInstant().toEpochMilli(),
        filter.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        filter.bookId,
    ).map { records ->
        records.filter { tx ->
            (tx.amount > 0) == filter.income &&
                (filter.category == null || tx.category == filter.category) &&
                (filter.subCategory == null || tx.subCategory.orEmpty() == filter.subCategory) &&
                (filter.tag == null || tx.tag == filter.tag)
        }.sortedWith(compareByDescending<TransactionEntity> { it.timestamp }.thenByDescending { it.id })
    }.stateIn(externalScope ?: viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
