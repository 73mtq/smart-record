package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.ui.components.TransactionRow
import com.nonen.Bookkeeping.ui.components.YimuTopAppBar
import com.nonen.Bookkeeping.ui.components.formatPlainAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsBillListScreen(
    vm: StatisticsBillListViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onSubCategory: (StatisticsBillFilter) -> Unit,
) {
    val records by vm.transactions.collectAsState()
    val filter = vm.filter
    val title = filter.tag ?: filter.subCategory?.ifBlank { filter.category.orEmpty() }
        ?: filter.category ?: stringResource(R.string.yimu_statistics_bills)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            YimuTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("data_list")) {
            item("summary") {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("${filter.start} — ${filter.end}", fontSize = 12.sp)
                    Text(stringResource(R.string.yimu_statistics_bill_total, records.size,
                        formatPlainAmount(records.sumOf { abs(it.amount) })), Modifier.padding(top = 8.dp), fontSize = 14.sp)
                }
            }
            if (filter.category != null && filter.subCategory == null) {
                records.groupBy { it.subCategory.orEmpty() }.forEach { (child, children) ->
                    item("category_$child") {
                        Row(Modifier.fillMaxWidth().clickable { onSubCategory(filter.copy(subCategory = child)) }
                            .padding(horizontal = 15.dp, vertical = 10.dp)) {
                            Text(child.ifEmpty { requireNotNull(filter.category) }, Modifier.weight(1f), fontSize = 13.sp)
                            Text(formatPlainAmount(children.sumOf { abs(it.amount) }), fontSize = 13.sp)
                        }
                    }
                }
            }
            if (records.isEmpty()) item("empty") {
                Text(stringResource(R.string.search_empty), Modifier.padding(15.dp), fontSize = 14.sp)
            }
            items(records, key = { "bill_${it.id}" }) { tx -> TransactionRow(tx, onClick = { onEdit(tx.id) }) }
        }
    }
}
