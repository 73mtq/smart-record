package com.nonen.Bookkeeping.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.ui.components.CalendarUiState
import com.nonen.Bookkeeping.ui.components.YimuCalendarNativeView
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun YimuCalendarScreen(vm: CalendarViewModel, onEdit: (Long) -> Unit, onAdd: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val month by vm.currentMonth.collectAsState()
    val selected by vm.selectedDate.collectAsState()
    val summaries by vm.monthSummaries.collectAsState()
    val income by vm.monthTotalIncome.collectAsState()
    val expense by vm.monthTotalExpense.collectAsState()
    val transactions by vm.selectedDayTransactions.collectAsState()
    val state = CalendarUiState(month, selected, summaries, income, expense, transactions)
    AndroidView(
        modifier = Modifier.fillMaxSize().statusBarsPadding().semantics { testTagsAsResourceId = true },
        factory = { ctx -> YimuCalendarNativeView(ctx) },
        update = { view ->
            view.render(state, object : YimuCalendarNativeView.Listener {
                override fun onSelectDate(date: LocalDate) = vm.selectDate(date)
                override fun onMonthChange(month: YearMonth) {
                    vm.selectDate(month.atDay(selected.dayOfMonth.coerceAtMost(month.lengthOfMonth())))
                }
                override fun onToday() = vm.goToToday()
                override fun onAdd(date: LocalDate) = onAdd(date)
                override fun onEdit(id: Long) = onEdit(id)
                override fun onMonthClick() {
                    Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show()
                }
                override fun onMore() {
                    Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show()
                }
            })
        },
    )
}
