package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import com.nonen.Bookkeeping.stats.aggregateCategoryRows
import com.nonen.Bookkeeping.stats.aggregateDayForms
import com.nonen.Bookkeeping.stats.aggregateMonthForms
import com.nonen.Bookkeeping.stats.aggregateTagStats
import com.nonen.Bookkeeping.ui.components.CategoryIconCatalog
import com.nonen.Bookkeeping.ui.components.StatisticsBarUi
import com.nonen.Bookkeeping.ui.components.StatisticsMonthUi
import com.nonen.Bookkeeping.ui.components.StatisticsNormalUi
import com.nonen.Bookkeeping.ui.components.StatisticsSelfUi
import com.nonen.Bookkeeping.ui.components.StatisticsYearUi
import com.nonen.Bookkeeping.ui.components.StatisticsUiState
import com.nonen.Bookkeeping.ui.components.YimuDateDialog
import com.nonen.Bookkeeping.ui.components.YimuStatisticsNativeView
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun YimuStatisticsScreen(
    container: AppContainer,
    bookId: Long,
    onBudget: () -> Unit,
    onBills: (StatisticsBillFilter) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expenseMode by rememberSaveable { mutableStateOf(true) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var yearIndex by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current
    val today = LocalDate.now()
    val start = today.minusDays(6)
    val zone = ZoneId.systemDefault()
    val transactions by remember(bookId, start, today) {
        container.transactionRepository.observeBookRange(
            start.atStartOfDay(zone).toInstant().toEpochMilli(),
            today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            bookId,
        )
    }.collectAsState(initial = emptyList())
    val assets by remember(container) { container.assetRepository.observeAll() }.collectAsState(initial = emptyList())
    val budgetMonth = today.year to today.monthValue
    val budget by remember(bookId, budgetMonth) {
        container.budgetRepository.observeBudgetOverview(bookId, budgetMonth.first, budgetMonth.second)
    }.collectAsState(initial = null)
    val month = YearMonth.now().minusMonths(monthOffset.toLong())
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    var selfStartDay by rememberSaveable { mutableLongStateOf(today.minusMonths(1).toEpochDay()) }
    var selfEndDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selfRawStart = LocalDate.ofEpochDay(selfStartDay)
    val selfRawEnd = LocalDate.ofEpochDay(selfEndDay)
    val selfStart = minOf(selfRawStart, selfRawEnd)
    val selfEnd = maxOf(selfRawStart, selfRawEnd)
    val selfTransactions by remember(bookId, selfStart, selfEnd) {
        container.transactionRepository.observeBookRange(
            selfStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            selfEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            bookId,
        )
    }.collectAsState(initial = emptyList())
    val displayYear = today.year - (yearIndex - 1).coerceAtLeast(0)
    val yearStart = if (yearIndex == 0) LocalDate.of(2000, 1, 1) else LocalDate.of(displayYear, 1, 1)
    val yearEnd = if (yearIndex == 0) LocalDate.of(today.year, 12, 31) else LocalDate.of(displayYear, 12, 31)
    val yearTransactions by remember(bookId, yearIndex, displayYear) {
        container.transactionRepository.observeBookRange(
            yearStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            yearEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            bookId,
        )
    }.collectAsState(initial = emptyList())
    val monthTransactions by remember(bookId, month) {
        container.transactionRepository.observeBookRange(
            monthStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            monthEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            bookId,
        )
    }.collectAsState(initial = emptyList())
    val dates = (0..6).map { start.plusDays(it.toLong()) }
    val payValues = dates.map { date ->
        -transactions.filter { it.timestamp in dayRange(date, zone) && it.amount < 0 }.sumOf { it.amount }.toFloat()
    }
    val incomeValues = dates.map { date ->
        transactions.filter { it.timestamp in dayRange(date, zone) && it.amount > 0 }.sumOf { it.amount }.toFloat()
    }
    val totalPay = payValues.sumOf { it.toDouble() }
    val totalIncome = incomeValues.sumOf { it.toDouble() }
    val labels = dates.map { date -> if (date == today) "今天" else weekday(date) }
    val assetEntries = assets.filter { it.intoTotalAsset && it.assetNumber > 0 }.map { it.assetName to it.assetNumber }
    val budgetEntries = budget?.let {
        if (it.totalBudget <= 0.0) emptyList()
        else listOf("已使用" to it.totalExpense, "剩余" to it.remainingBudget.coerceAtLeast(0.0))
    }.orEmpty()
    val monthDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val monthDailyPay = monthDays.map { date ->
        -monthTransactions.filter { it.timestamp in dayRange(date, zone) && it.amount < 0 }.sumOf { it.amount }.toFloat()
    }
    val monthDailyIncome = monthDays.map { date ->
        monthTransactions.filter { it.timestamp in dayRange(date, zone) && it.amount > 0 }.sumOf { it.amount }.toFloat()
    }
    val monthPay = monthDailyPay.sumOf { it.toDouble() }
    val monthIncome = monthDailyIncome.sumOf { it.toDouble() }
    val iconCatalog = remember { CategoryIconCatalog(context) }
    val tags by remember(container) { container.tagDao.getAllFlow() }.collectAsState(initial = emptyList())
    var tagRefs by remember { mutableStateOf(emptyList<TransactionTagCrossRef>()) }
    LaunchedEffect(container) { tagRefs = container.tagDao.getAllRefs() }
    val monthEntries = monthTransactions.filter { it.amount < 0 }
        .groupBy { iconCatalog.names(it.category, it.subCategory, false).first }
        .map { (category, records) -> category to -records.sumOf { it.amount } }
        .sortedByDescending { it.second }
    val monthlyPay = (1..12).map { m ->
        -yearTransactions.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().monthValue == m && it.amount < 0 }.sumOf { it.amount }.toFloat()
    }
    val monthlyIncome = (1..12).map { m ->
        yearTransactions.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().monthValue == m && it.amount > 0 }.sumOf { it.amount }.toFloat()
    }
    val yearDailyPay = (1..12).map { m ->
        val ym = YearMonth.of(displayYear, m)
        (1..ym.lengthOfMonth()).map { d ->
            -yearTransactions.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == ym.atDay(d) && it.amount < 0 }.sumOf { it.amount }.toFloat()
        }
    }
    val yearDailyIncome = (1..12).map { m ->
        val ym = YearMonth.of(displayYear, m)
        (1..ym.lengthOfMonth()).map { d ->
            yearTransactions.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == ym.atDay(d) && it.amount > 0 }.sumOf { it.amount }.toFloat()
        }
    }
    val selfDates = generateSequence(selfStart) { it.plusDays(1) }.takeWhile { it <= selfEnd }.toList()
    val selfDailyPay = selfDates.map { date ->
        -selfTransactions.filter { it.timestamp in dayRange(date, zone) && it.amount < 0 }.sumOf { it.amount }.toFloat()
    }
    val selfDailyIncome = selfDates.map { date ->
        selfTransactions.filter { it.timestamp in dayRange(date, zone) && it.amount > 0 }.sumOf { it.amount }.toFloat()
    }
    val selfPayTotal = selfDailyPay.sumOf { it.toDouble() }
    val selfIncomeTotal = selfDailyIncome.sumOf { it.toDouble() }
    val monthCategories = aggregateCategoryRows(monthTransactions, isIncome = false) {
        iconCatalog.names(it.category, it.subCategory, false).first
    }.map { it.copy(iconName = iconCatalog.get(it.name, null, false, null)) }
    val yearCategories = aggregateCategoryRows(yearTransactions, isIncome = false) {
        iconCatalog.names(it.category, it.subCategory, false).first
    }.map { it.copy(iconName = iconCatalog.get(it.name, null, false, null)) }
    val selfCategories = aggregateCategoryRows(selfTransactions, isIncome = false) {
        iconCatalog.names(it.category, it.subCategory, false).first
    }.map { it.copy(iconName = iconCatalog.get(it.name, null, false, null)) }
    val monthIncomeCategories = aggregateCategoryRows(monthTransactions, isIncome = true) {
        iconCatalog.names(it.category, it.subCategory, true).first
    }.map { it.copy(iconName = iconCatalog.get(it.name, null, true, null)) }
    val monthIncomeCategoryEntries = monthIncomeCategories.map { it.name to it.amount }
    val yearCategoryEntries = yearCategories.map { it.name to it.amount }
    val selfCategoryEntries = selfCategories.map { it.name to it.amount }
    val monthTags = aggregateTagStats(monthTransactions, tags, tagRefs)
    val yearTags = aggregateTagStats(yearTransactions, tags, tagRefs)
    val selfTags = aggregateTagStats(selfTransactions, tags, tagRefs)
    val monthDayForms = aggregateDayForms(monthTransactions)
    val yearDayForms = aggregateMonthForms(yearTransactions)
    val yearLabels = buildList {
        add("所有"); add("今年"); add("去年")
        for (y in today.year - 2 downTo today.year - 6) add("${y}年")
    }
    val monthLabels = buildList {
        add("本月")
        add("上月")
        var cursor = month.minusMonths(2)
        repeat(10) {
            add("${cursor.monthValue}月")
            cursor = cursor.minusMonths(1)
        }
    }
    val state = StatisticsUiState(
        tab = tab,
        normal = StatisticsNormalUi(
            bar = StatisticsBarUi(
                labels = labels,
                pay = payValues,
                income = incomeValues,
                expenseMode = expenseMode,
                totalPay = totalPay,
                totalIncome = totalIncome,
                averagePay = totalPay / 7,
                averageIncome = totalIncome / 7,
            ),
            assetEntries = assetEntries,
            budgetEntries = budgetEntries,
        ),
        month = StatisticsMonthUi(
            year = month.year,
            month = month.monthValue,
            monthLabels = monthLabels,
            selectedIndex = monthOffset,
            pay = monthPay,
            income = monthIncome,
            balance = monthIncome - monthPay,
            dailyPay = monthDailyPay,
            dailyIncome = monthDailyIncome,
            assetTrend = monthDays.map { 0f },
            categoryEntries = monthEntries,
            categories = monthCategories,
            incomeCategoryEntries = monthIncomeCategoryEntries,
            incomeCategories = monthIncomeCategories,
            dayForms = monthDayForms,
            tags = monthTags,
        ),
        year = StatisticsYearUi(
            year = displayYear,
            yearLabels = yearLabels,
            selectedIndex = yearIndex,
            pay = monthlyPay.sumOf { it.toDouble() },
            income = monthlyIncome.sumOf { it.toDouble() },
            balance = monthlyIncome.sumOf { it.toDouble() } - monthlyPay.sumOf { it.toDouble() },
            monthlyPay = monthlyPay,
            monthlyIncome = monthlyIncome,
            dailyPay = yearDailyPay,
            dailyIncome = yearDailyIncome,
            maxPay = yearDailyPay.flatten().maxOrNull()?.toDouble() ?: 0.0,
            maxIncome = yearDailyIncome.flatten().maxOrNull()?.toDouble() ?: 0.0,
            categoryEntries = yearCategoryEntries,
            categories = yearCategories,
            dayForms = yearDayForms,
            tags = yearTags,
            assetTrend = List(monthlyPay.size) { 0f },
        ),
        self = StatisticsSelfUi(
            startLabel = selfStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            endLabel = selfEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            pay = selfPayTotal,
            income = selfIncomeTotal,
            balance = selfIncomeTotal - selfPayTotal,
            dailyPay = selfDailyPay,
            dailyIncome = selfDailyIncome,
            categoryEntries = selfCategoryEntries,
            categories = selfCategories,
            tags = selfTags,
        ),
    )
    val openBudget = onBudget
    fun pickSelfDate(current: LocalDate, onPick: (LocalDate) -> Unit) {
        YimuDateDialog(context, current.atStartOfDay(zone).toInstant().toEpochMilli()) { millis ->
            onPick(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
        }.show()
    }
    AndroidView(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        factory = { YimuStatisticsNativeView(it) },
        update = { view ->
            view.render(state, object : YimuStatisticsNativeView.Listener {
                override fun onTabSelected(selected: Int) {
                    tab = selected
                }
                override fun onBudget() {
                    openBudget()
                }
                override fun onMonthSelected(index: Int) {
                    monthOffset = index
                }
                override fun onYearSelected(index: Int) {
                    yearIndex = index
                }
                override fun onSelfStartClick() {
                    pickSelfDate(selfStart) { picked -> selfStartDay = picked.toEpochDay() }
                }
                override fun onSelfEndClick() {
                    pickSelfDate(selfEnd) { picked -> selfEndDay = picked.toEpochDay() }
                }
            })
        },
    )
}

private fun dayRange(date: LocalDate, zone: ZoneId): LongRange =
    date.atStartOfDay(zone).toInstant().toEpochMilli() until
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

private fun weekday(date: LocalDate): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]
