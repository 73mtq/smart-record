package com.nonen.Bookkeeping.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.data.repo.ReportRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.stats.AssetTrendPoint
import com.nonen.Bookkeeping.stats.StatsCalculator
import com.nonen.Bookkeeping.stats.StatsData
import com.nonen.Bookkeeping.stats.StatsPeriod
import com.nonen.Bookkeeping.stats.StatsQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class StatsViewModel(
    private val repo: TransactionRepository,
    private val reportRepo: ReportRepository? = null,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private var selectedBookId: Long? = null

    var period by mutableStateOf(StatsPeriod.MONTH)
    var isIncome by mutableStateOf(false)
    var statsViewMode by mutableIntStateOf(0) // 0=分类分析，1=资产走势
    var expandedCategory by mutableStateOf<String?>(null)
    var assetTrend by mutableStateOf<List<AssetTrendPoint>>(emptyList())
        private set
    var monthSel by mutableStateOf<Int?>(null)
    var weekSel by mutableStateOf<Int?>(null)
    var customStart by mutableStateOf<LocalDate?>(null)
    var customEnd by mutableStateOf<LocalDate?>(null)
    var customRequest by mutableIntStateOf(0)
        private set
    var stats by mutableStateOf<StatsData?>(null)
        private set

    init {
        load()
    }

    fun selectBook(id: Long) {
        if (selectedBookId == id) return
        selectedBookId = id
        load()
    }

    fun toggleCategoryExpand(catName: String) {
        expandedCategory = if (expandedCategory == catName) null else catName
    }

    fun switchStatsViewMode(mode: Int) {
        if (statsViewMode == mode) return
        statsViewMode = mode
        load()
    }

    fun updatePeriod(p: StatsPeriod) {
        if (period == p) return
        period = p
        if (p == StatsPeriod.YEAR) weekSel = null
        if (p == StatsPeriod.MONTH) monthSel = null
        load()
    }

    fun requestCustom() {
        period = StatsPeriod.CUSTOM
        customRequest++
    }

    fun setType(income: Boolean) {
        if (isIncome == income) return
        isIncome = income
        load()
    }

    fun selectMonth(m: Int?) {
        if (monthSel == m) return
        monthSel = m
        load()
    }

    fun selectWeek(w: Int?) {
        if (weekSel == w) return
        weekSel = w
        load()
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        customStart = minOf(start, end)
        customEnd = maxOf(start, end)
        period = StatsPeriod.CUSTOM
        load()
    }

    fun load() {
        scope.launch {
            stats = withContext(Dispatchers.Default) { computeStats() }
            if (reportRepo != null && statsViewMode == 1) {
                val (startDate, endDate) = assetTrendRange(LocalDate.now())
                assetTrend = withContext(Dispatchers.Default) {
                    reportRepo.getAssetTrend(startDate, endDate)
                }
            }
        }
    }

    private fun assetTrendRange(today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
        StatsPeriod.WEEK -> today.minusDays(6) to today
        StatsPeriod.MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        StatsPeriod.YEAR -> today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
        StatsPeriod.CUSTOM -> {
            val start = customStart ?: today.minusMonths(1)
            val end = customEnd ?: today
            minOf(start, end) to maxOf(start, end)
        }
    }

    private suspend fun computeStats(): StatsData? = StatsCalculator.compute(
        query = StatsQuery(
            period = period,
            isIncome = isIncome,
            monthSel = monthSel,
            weekSel = weekSel,
            customStart = customStart,
            customEnd = customEnd,
        ),
        fetch = { start, end ->
            val book = selectedBookId
            if (book == null) repo.getRange(start, end) else repo.getBookRange(start, end, book)
        },
    )
}
