package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.nonen.Bookkeeping.ui.components.YimuTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.stats.CalendarDaySummary
import com.nonen.Bookkeeping.ui.components.EmptyState
import com.nonen.Bookkeeping.ui.components.TransactionRow
import com.nonen.Bookkeeping.ui.components.formatPlainAmount
import com.nonen.Bookkeeping.ui.theme.ExpenseColor
import com.nonen.Bookkeeping.ui.theme.IncomeColor
import com.nonen.Bookkeeping.ui.theme.InkPrimary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth


/**
 * 收支日历屏幕：
 * 提供月度日历网格浏览、每日收支标记与选中日期明细联动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    embedded: Boolean = false,
) {
    val currentMonth by vm.currentMonth.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val summaries by vm.monthSummaries.collectAsState()
    val transactions by vm.selectedDayTransactions.collectAsState()
    val monthExpense by vm.monthTotalExpense.collectAsState()
    val monthIncome by vm.monthTotalIncome.collectAsState()

    val today = remember { LocalDate.now() }
    val summaryMap = remember(summaries) {
        summaries.associateBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(onClick = { vm.prevMonth() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "上一月",
                            )
                        }
                        Text(
                            text = "${currentMonth.year}年${currentMonth.monthValue}月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { vm.nextMonth() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下一月",
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!embedded) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    }
                },
                actions = {
                    if (selectedDate != today || currentMonth != YearMonth.from(today)) {
                        TextButton(onClick = { vm.goToToday(today) }) {
                            Text("今天")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = InkPrimary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // 月度汇总小卡片
            MonthSummaryHeader(expense = monthExpense, income = monthIncome)

            // 日历主体网格
            CalendarMonthGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                today = today,
                summaryMap = summaryMap,
                onDateSelected = { vm.selectDate(it) },
            )

            Spacer(Modifier.height(8.dp))

            // 当日明细区域头栏
            SelectedDayHeader(selectedDate = selectedDate, summaries = summaryMap[selectedDate])

            // 当日交易明细列表
            if (transactions.isEmpty()) {
                EmptyState(
                    icon = "📅",
                    text = "当日暂无收支明细",
                    modifier = Modifier.padding(top = 40.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            tx = tx,
                            onClick = { onEdit(tx.id) },
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun MonthSummaryHeader(expense: Double, income: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "月支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "¥${formatPlainAmount(expense)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ExpenseColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "月收入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "¥${formatPlainAmount(income)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = IncomeColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "结余", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val balance = income - expense
                Text(
                    text = "¥${formatPlainAmount(balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

        }
    }
}

@Composable
private fun CalendarMonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    summaryMap: Map<LocalDate, CalendarDaySummary>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) .. 7 (Sun)
    // 以周一为一周起始：偏移量为 (firstDayOfWeek - 1)
    val leadingEmptyDays = (firstDayOfWeek - 1) % 7
    val totalCells = leadingEmptyDays + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        // 星期栏表头
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // 日历行
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - leadingEmptyDays + 1
                    if (dayNum in 1..daysInMonth) {
                        val cellDate = currentMonth.atDay(dayNum)
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val summary = summaryMap[cellDate]

                        CalendarCell(
                            date = cellDate,
                            isSelected = isSelected,
                            isToday = isToday,
                            summary = summary,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onDateSelected(cellDate) },
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    summary: CalendarDaySummary?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cellShape = RoundedCornerShape(10.dp)
    val bgColor = when {
        isSelected -> InkPrimary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(cellShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )
            // 收支指示小点
            if (summary != null && summary.transactionCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp),
                ) {
                    if (summary.totalExpense > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else ExpenseColor),
                        )
                    }
                    if (summary.totalIncome > 0) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF81C784) else IncomeColor),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun SelectedDayHeader(
    selectedDate: LocalDate,
    summaries: CalendarDaySummary?,
) {
    val weekName = when (selectedDate.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
        null -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 $weekName",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (summaries != null && summaries.transactionCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (summaries.totalExpense > 0) {
                    Text(
                        text = "支 ¥${formatPlainAmount(summaries.totalExpense)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (summaries.totalIncome > 0) {
                    Text(
                        text = "收 ¥${formatPlainAmount(summaries.totalIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

