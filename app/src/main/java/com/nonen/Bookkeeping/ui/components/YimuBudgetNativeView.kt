package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nonen.Bookkeeping.R
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class YimuBudgetNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onEditTotal()
        fun onAddCategory()
    }

    private val monthText: TextView
    private val monthBudget: TextView
    private val budgetSurplus: TextView
    private val monthPay: TextView
    private val dayPay: TextView
    private val budgetDay: TextView
    private val lineChart: LineChart
    private val progressTrack: YimuCircleLineView
    private val progressFill: YimuCircleLineView
    private val tipLayout: View
    private var listener: Listener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_budget_manager, this, true)
        monthText = findViewById(R.id.month)
        monthBudget = findViewById(R.id.month_budget)
        budgetSurplus = findViewById(R.id.budget_surplus)
        monthPay = findViewById(R.id.month_pay)
        dayPay = findViewById(R.id.day_pay)
        budgetDay = findViewById(R.id.budget_day_surplus)
        lineChart = findViewById(R.id.budget_line_chart)
        progressTrack = findViewById(R.id.progress_view_one)
        progressFill = findViewById(R.id.progress_view_two)
        tipLayout = findViewById(R.id.tip_layout)
        progressTrack.setLineColor(ContextCompat.getColor(context, R.color.colorPrimaryAlpha10))
        progressTrack.setProgress(100)
        progressFill.setLineColor(ContextCompat.getColor(context, R.color.textColorPrimary))
        findViewById<View>(R.id.btn_back).setOnClickListener { listener?.onBack() }
        findViewById<View>(R.id.month_budget_layout).setOnClickListener { listener?.onEditTotal() }
        findViewById<View>(R.id.add_category_budget).setOnClickListener { listener?.onAddCategory() }
        configureChart()
    }

    fun render(year: Int, month: Int, totalBudget: Double, totalExpense: Double, remaining: Double,
               usageRatio: Float, categoryCount: Int, listener: Listener) {
        this.listener = listener
        monthText.text = "${year}年${month}月"
        val hasBudget = totalBudget > 0.0
        monthBudget.text = money(totalBudget)
        budgetSurplus.text = money(if (hasBudget) remaining else 0.0)
        monthPay.text = money(if (hasBudget) totalExpense else 0.0)
        // 原版 s0：日均 = 支出 / 已过天数；日预算 = 剩余 / 剩余天数（含当天）
        val measure = budgetDays(year, month, LocalDate.now())
        dayPay.text = money(if (hasBudget && measure.elapsedDays > 0) totalExpense / measure.elapsedDays else 0.0)
        budgetDay.text = money(if (hasBudget && measure.remainingDays > 0) remaining / measure.remainingDays else 0.0)
        progressFill.setProgress(budgetRemainingPercent(totalBudget, totalExpense))
        tipLayout.visibility = if (categoryCount == 0) View.VISIBLE else View.GONE
        renderChart(year, month)
    }

    private fun configureChart() {
        lineChart.setViewPortOffsets(dp(40).toFloat(), dp(10).toFloat(), dp(15).toFloat(), dp(15).toFloat())
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.setDragEnabled(false)
        lineChart.setScaleEnabled(false)
        lineChart.setDrawBorders(false)
        lineChart.setPinchZoom(false)
        lineChart.setDrawGridBackground(false)
        lineChart.setBackgroundColor(0)
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisLeft.apply {
            setLabelCount(5, false)
            textSize = 7f
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            setDrawGridLines(false)
            axisLineColor = ContextCompat.getColor(context, R.color.colorPrimary)
            // 参考刻度 0.0/-0.5/-1.0 步 0.5 → 轴范围 ±1.2；±1.1 时 MPChart 算出 0.4 步长（实测）
            axisMinimum = -1.2f
            axisMaximum = 1.2f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = String.format(Locale.US, "%.1f", value)
            }
        }
        lineChart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            textSize = 7f
        }
    }

    private fun renderChart(year: Int, month: Int) {
        // 原版 BudgetManagerActivity.B0():382-390 用当月第一天/最后一天逐日累加，数据点与 labelCount 均为当月天数
        val days = budgetAxisDays(year, month)
        val entries = (0 until days).map { Entry(it.toFloat(), 0f) }
        val pay = LineDataSet(entries, "支出").apply {
            color = ContextCompat.getColor(context, R.color.moneyPay)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            setFillDrawable(ContextCompat.getDrawable(context, R.drawable.fade_pay))
            highLightColor = 0
        }
        val remaining = LineDataSet(entries, "剩余").apply {
            color = ContextCompat.getColor(context, R.color.moneyIncome)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            setFillDrawable(ContextCompat.getDrawable(context, R.drawable.fade_income))
            highLightColor = 0
        }
        // 原版 budget 图 xAxis 无 min/max 钳制，首点贴 plot 左缘（-0.5 会使线缩进约 16px）
        lineChart.xAxis.resetAxisMinimum()
        lineChart.xAxis.resetAxisMaximum()
        lineChart.xAxis.setLabelCount(days)
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = budgetDayAxisLabel(value, 1, days)
        }
        lineChart.data = LineData(listOf(pay, remaining))
        lineChart.invalidate()
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)
}

internal fun budgetAxisDays(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

/** 原版 s0 的已过天数与剩余天数：剩余天数从当天算到月末，含当天。 */
internal data class BudgetDays(val elapsedDays: Int, val remainingDays: Int)

internal fun budgetDays(year: Int, month: Int, today: LocalDate): BudgetDays {
    val days = budgetAxisDays(year, month)
    val first = LocalDate.of(year, month, 1)
    val last = LocalDate.of(year, month, days)
    return when {
        today.isAfter(last) -> BudgetDays(days, 0)
        today.isBefore(first) -> BudgetDays(0, days)
        else -> BudgetDays(today.dayOfMonth, days - today.dayOfMonth + 1)
    }
}

/**
 * 原版 BudgetManagerActivity.g.getFormattedValue(float)：
 * (1 + value) % 2 == 0 返回空串；否则 (int) value + 起始日，超过当月天数则减去当月天数。
 */
internal fun budgetDayAxisLabel(value: Float, startDay: Int, daysInMonth: Int): String {
    if ((1.0f + value) % 2.0f == 0.0f) return ""
    var day = value.toInt() + startDay
    if (day > daysInMonth) day -= daysInMonth
    return day.toString()
}

/**
 * 原版 BudgetManagerActivity.s0：进度条按剩余比例绘制，
 * 预算减支出小于等于 0 时不绘制；百分比取整后由 CircleLineView 钳到 0..100。
 */
internal fun budgetRemainingPercent(totalBudget: Double, totalExpense: Double): Int {
    if (totalBudget <= 0.0) return 0
    val remaining = totalBudget - totalExpense
    if (remaining <= 0.0) return 0
    return ((remaining * 100.0) / totalBudget).toInt().coerceIn(0, 100)
}
