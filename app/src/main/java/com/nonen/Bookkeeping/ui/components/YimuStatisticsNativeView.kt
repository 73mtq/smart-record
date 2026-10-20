package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.ui.chart.RoundedBarChartRenderer
import java.util.Locale

data class StatisticsBarUi(
    val labels: List<String>,
    val pay: List<Float>,
    val income: List<Float>,
    val expenseMode: Boolean,
    val totalPay: Double,
    val totalIncome: Double,
    val averagePay: Double,
    val averageIncome: Double,
)

data class StatisticsNormalUi(
    val bar: StatisticsBarUi,
    val assetEntries: List<Pair<String, Double>> = emptyList(),
    val budgetEntries: List<Pair<String, Double>> = emptyList(),
)

data class StatisticsMonthUi(
    val year: Int,
    val month: Int,
    val monthLabels: List<String>,
    val selectedIndex: Int = 0,
    val pay: Double,
    val income: Double,
    val balance: Double,
    val dailyPay: List<Float>,
    val dailyIncome: List<Float>,
    val assetTrend: List<Float>,
    val categoryEntries: List<Pair<String, Double>> = emptyList(),
)

data class StatisticsYearUi(
    val year: Int,
    val yearLabels: List<String>,
    val selectedIndex: Int = 1,
    val pay: Double,
    val income: Double,
    val balance: Double,
    val monthlyPay: List<Float>,
    val monthlyIncome: List<Float>,
    val dailyPay: List<List<Float>>,
    val dailyIncome: List<List<Float>>,
    val maxPay: Double,
    val maxIncome: Double,
)

data class StatisticsSelfUi(
    val startLabel: String,
    val endLabel: String,
    val pay: Double,
    val income: Double,
    val balance: Double,
    val dailyPay: List<Float>,
    val dailyIncome: List<Float>,
)

data class StatisticsUiState(
    val tab: Int = 0,
    val normal: StatisticsNormalUi? = null,
    val month: StatisticsMonthUi? = null,
    val year: StatisticsYearUi? = null,
    val self: StatisticsSelfUi? = null,
)

class YimuStatisticsNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onTabSelected(tab: Int)
        fun onBudget()
    }

    private val tabs = mutableListOf<TextView>()
    private val normalView: View = LayoutInflater.from(context).inflate(R.layout.yimu_statistics_normal, null, false)
    private val normalBar = normalView.findViewById<BarChart>(R.id.bar_chart)
    private val normalPayToggle = normalView.findViewById<TextView>(R.id.bar_pay)
    private val normalIncomeToggle = normalView.findViewById<TextView>(R.id.bar_income)
    private val normalTotalPay = normalView.findViewById<TextView>(R.id.total_pay)
    private val normalTotalIncome = normalView.findViewById<TextView>(R.id.total_income)
    private val normalDayPay = normalView.findViewById<TextView>(R.id.day_pay)
    private val normalDayIncome = normalView.findViewById<TextView>(R.id.day_income)
    private val normalAssetChart = normalView.findViewById<PieChart>(R.id.pie_asset_chart)
    private val normalBudgetChart = normalView.findViewById<PieChart>(R.id.pie_budget_chart)
    private val normalAssetCheck = normalView.findViewById<CheckBox>(R.id.filter_asset_check)
    private val normalAssetCheckLayout = normalView.findViewById<View>(R.id.asset_chart_check)
    private val normalBudgetInfo = normalView.findViewById<TextView>(R.id.budget_info)
    private val monthView: View = LayoutInflater.from(context).inflate(R.layout.yimu_statistics_month, null, false)
    private val monthYear = monthView.findViewById<TextView>(R.id.current_year)
    private val monthList = monthView.findViewById<LinearLayout>(R.id.month_list)
    private val monthPay = monthView.findViewById<TextView>(R.id.pay_num)
    private val monthIncome = monthView.findViewById<TextView>(R.id.income_num)
    private val monthBalance = monthView.findViewById<TextView>(R.id.balance_num)
    private val monthBar = monthView.findViewById<BarChart>(R.id.bar_chart)
    private val monthPayToggle = monthView.findViewById<TextView>(R.id.bar_pay)
    private val monthIncomeToggle = monthView.findViewById<TextView>(R.id.bar_income)
    private val monthPieParent = monthView.findViewById<TextView>(R.id.pie_parent)
    private val monthPieAll = monthView.findViewById<TextView>(R.id.pie_all)
    private val yearView: View = LayoutInflater.from(context).inflate(R.layout.yimu_statistics_year, null, false)
    private val yearList = yearView.findViewById<LinearLayout>(R.id.year_list)
    private val yearPay = yearView.findViewById<TextView>(R.id.pay_num)
    private val yearIncome = yearView.findViewById<TextView>(R.id.income_num)
    private val yearBalance = yearView.findViewById<TextView>(R.id.balance_num)
    private val yearBar = yearView.findViewById<BarChart>(R.id.bar_chart)
    private val yearHeatList = yearView.findViewById<RecyclerView>(R.id.heat_map_data)
    private val yearBarPay = yearView.findViewById<TextView>(R.id.bar_pay)
    private val yearBarIncome = yearView.findViewById<TextView>(R.id.bar_income)
    private val yearBarBalance = yearView.findViewById<TextView>(R.id.bar_balance)
    private val heatPay = yearView.findViewById<TextView>(R.id.heat_map_pay)
    private val heatIncome = yearView.findViewById<TextView>(R.id.heat_map_income)
    private val heatBalance = yearView.findViewById<TextView>(R.id.heat_map_balance)
    private val yearHeatAdapter = HeatAdapter()
    private val selfView: View = LayoutInflater.from(context).inflate(R.layout.yimu_statistics_self, null, false)
    private val selfStart = selfView.findViewById<TextView>(R.id.start_day)
    private val selfEnd = selfView.findViewById<TextView>(R.id.end_day)
    private val selfPay = selfView.findViewById<TextView>(R.id.pay_num)
    private val selfIncome = selfView.findViewById<TextView>(R.id.income_num)
    private val selfBalance = selfView.findViewById<TextView>(R.id.balance_num)
    private val selfLine = selfView.findViewById<LineChart>(R.id.bill_line_chart)
    private val selfAssetLine = selfView.findViewById<LineChart>(R.id.asset_line_chart)
    private val selfLineStart = selfView.findViewById<TextView>(R.id.bill_line_start_time)
    private val selfLineEnd = selfView.findViewById<TextView>(R.id.bill_line_end_time)
    private val monthLine = monthView.findViewById<LineChart>(R.id.line_chart)
    private val monthCategoryChart = monthView.findViewById<PieChart>(R.id.pie_chart)
    private var listener: Listener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_statistics_host, this, true)
        findViewById<FrameLayout>(R.id.statistics_content).addView(normalView)
        findViewById<FrameLayout>(R.id.statistics_content).addView(monthView)
        findViewById<FrameLayout>(R.id.statistics_content).addView(yearView)
        findViewById<FrameLayout>(R.id.statistics_content).addView(selfView)
        monthView.visibility = View.GONE
        yearView.visibility = View.GONE
        selfView.visibility = View.GONE
        tabs += listOf(
            findViewById(R.id.normal_bill_btn),
            findViewById(R.id.month_bill_btn),
            findViewById(R.id.year_bill_btn),
            findViewById(R.id.self_bill_btn),
        )
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener { listener?.onTabSelected(index) }
        }
        normalBudgetInfo.setOnClickListener { listener?.onBudget() }
        normalAssetCheck.isChecked = true
        configureBarChart(normalBar)
        normalBar.renderer = RoundedBarChartRenderer(normalBar, normalBar.animator, normalBar.viewPortHandler)
        configurePieChart(normalAssetChart)
        configurePieChart(normalBudgetChart)
        configureMonthBarChart(monthBar)
        configureMonthLineChart(monthLine)
        configurePieChart(monthCategoryChart)
        configureYearBarChart(yearBar)
        yearHeatList.layoutManager = GridLayoutManager(context, 4)
        yearHeatList.adapter = yearHeatAdapter
        yearView.findViewById<View>(R.id.member_layout).visibility = View.GONE
        selfView.findViewById<View>(R.id.member_layout).visibility = View.GONE
        configureBillLineChart(selfLine)
        configureMonthLineChart(selfAssetLine)
    }

    fun render(state: StatisticsUiState, listener: Listener) {
        this.listener = listener
        renderTabs(state.tab)
        normalView.visibility = if (state.tab == 0) View.VISIBLE else View.GONE
        monthView.visibility = if (state.tab == 1) View.VISIBLE else View.GONE
        yearView.visibility = if (state.tab == 2) View.VISIBLE else View.GONE
        selfView.visibility = if (state.tab == 3) View.VISIBLE else View.GONE
        state.normal?.let { renderNormal(it) }
        if (state.tab == 1) state.month?.let { renderMonth(it) }
        if (state.tab == 2) state.year?.let { renderYear(it) }
        if (state.tab == 3) state.self?.let { renderSelf(it) }
    }

    private fun renderTabs(selected: Int) {
        val host = findViewById<RelativeLayout>(R.id.top_layout)
        ViewCompat.setElevation(host, if (selected == 0) 3 * resources.displayMetrics.density else 0f)
        tabs.forEachIndexed { index, tab ->
            tab.setBackgroundResource(if (index == selected) R.drawable.bg_statistics_btn else 0)
            tab.setTextColor(ContextCompat.getColor(context,
                if (index == selected) R.color.textColorWhite else R.color.darkGrey))
        }
    }

    private fun renderNormal(state: StatisticsNormalUi) {
        val bar = state.bar
        normalTotalPay.text = "总支出：" + trim(bar.totalPay)
        normalTotalIncome.text = "总收入：" + trim(bar.totalIncome)
        normalDayPay.text = "日均支出：" + trim(bar.averagePay)
        normalDayIncome.text = "日均收入：" + trim(bar.averageIncome)
        renderToggle(normalPayToggle, normalIncomeToggle, bar.expenseMode)
        normalPayToggle.setOnClickListener {
            val next = state.copy(bar = bar.copy(expenseMode = false))
            renderNormal(next)
        }
        normalIncomeToggle.setOnClickListener {
            val next = state.copy(bar = bar.copy(expenseMode = true))
            renderNormal(next)
        }
        val values = if (bar.expenseMode) bar.pay else bar.income
        renderBars(values, bar.labels, if (bar.expenseMode) R.color.moneyPay else R.color.moneyIncome)
        normalAssetCheckLayout.visibility = if (state.assetEntries.isEmpty()) View.GONE else View.VISIBLE
        renderPie(normalAssetChart, state.assetEntries)
        renderPie(normalBudgetChart, state.budgetEntries)
    }

    private fun renderMonth(state: StatisticsMonthUi) {
        monthYear.text = state.year.toString()
        buildMonthStrip(state)
        monthPay.text = money2(state.pay)
        monthIncome.text = money2(state.income)
        monthBalance.text = money2(state.balance)
        renderToggle(monthPayToggle, monthIncomeToggle, true)
        monthView.findViewById<View>(R.id.member_layout).visibility = View.GONE
        monthView.findViewById<TextView>(R.id.pie_title).text = "支出占比"
        renderToggle(monthPieParent, monthPieAll, true)
        renderMonthBars(state.dailyPay)
        renderAssetLine(monthLine, state.assetTrend)
        renderMonthCategory(state.categoryEntries)
    }

    private fun renderMonthCategory(entries: List<Pair<String, Double>>) {
        val total = entries.sumOf { it.second }
        if (total <= 0.0) {
            renderPie(monthCategoryChart, emptyList())
            return
        }
        val slices = entries.map { (name, value) ->
            PieEntry(value.toFloat(), "$name ${String.format(Locale.US, "%.2f", value / total * 100)}%")
        }
        val colors = entries.indices.map { PIE_COLORS[it % PIE_COLORS.size] }
        val set = PieDataSet(slices, "").apply {
            sliceSpace = 3f
            selectionShift = 5f
            this.colors = colors
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = 0xFFCCCCCC.toInt()
        }
        val data = PieData(set)
        data.setDrawValues(false)
        monthCategoryChart.setUsePercentValues(true)
        monthCategoryChart.setDrawEntryLabels(true)
        monthCategoryChart.setEntryLabelColor(ContextCompat.getColor(context, R.color.darkGrey))
        monthCategoryChart.setEntryLabelTextSize(10f)
        monthCategoryChart.setDrawCenterText(false)
        monthCategoryChart.setHoleColor(0)
        monthCategoryChart.setTransparentCircleColor(Color.WHITE)
        monthCategoryChart.data = data
        monthCategoryChart.highlightValues(null)
        monthCategoryChart.invalidate()
    }

    private fun buildMonthStrip(state: StatisticsMonthUi) {
        monthList.removeAllViews()
        state.monthLabels.forEachIndexed { index, label ->
            val selected = index == state.selectedIndex
            val item = LayoutInflater.from(context).inflate(R.layout.item_month_or_year, monthList, false)
            val name = item.findViewById<TextView>(R.id.name)
            val icon = item.findViewById<ImageView>(R.id.choice_icon)
            name.text = label
            name.setTextColor(ContextCompat.getColor(context,
                if (selected) R.color.colorPrimary else R.color.darkGrey))
            name.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            icon.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            monthList.addView(item)
        }
    }

    private fun configureMonthBarChart(chart: BarChart) {
        chart.setViewPortOffsets(dp(40).toFloat(), dp(20).toFloat(), dp(10).toFloat(), dp(20).toFloat())
        chart.setDrawBarShadow(false)
        chart.description.isEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setTouchEnabled(false)
        chart.setDragEnabled(false)
        chart.setScaleEnabled(false)
        chart.setBackgroundColor(0)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.apply {
            axisMinimum = 0f
            setLabelCount(5, false)
            textSize = 7f
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            setDrawGridLines(false)
            axisLineColor = ContextCompat.getColor(context, R.color.colorPrimary)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    String.format(Locale.US, "%,.0f", value)
            }
        }
        chart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            textSize = 10f
            setLabelCount(15)
            granularity = 2f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
            }
        }
        chart.renderer = RoundedBarChartRenderer(chart, chart.animator, chart.viewPortHandler)
    }

    private fun renderMonthBars(values: List<Float>) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "收支").apply {
            color = ContextCompat.getColor(context, R.color.moneyPay)
            highLightColor = 0
            setDrawValues(false)
        }
        val data = BarData(dataSet)
        data.barWidth = 0.5f
        monthBar.xAxis.axisMinimum = -0.5f
        monthBar.xAxis.axisMaximum = values.size - 0.5f
        monthBar.data = data
        monthBar.invalidate()
    }

    private fun configureMonthLineChart(chart: LineChart) {
        chart.setViewPortOffsets(dp(40).toFloat(), dp(20).toFloat(), dp(15).toFloat(), dp(20).toFloat())
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDragEnabled(false)
        chart.setScaleEnabled(false)
        chart.setDrawBorders(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setBackgroundColor(0)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.apply {
            setLabelCount(5, false)
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            setDrawGridLines(false)
            axisLineColor = ContextCompat.getColor(context, R.color.colorPrimary)
            textSize = 7f
            axisMinimum = -1.1f
            axisMaximum = 1.1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    String.format(Locale.US, "%.1f", value)
            }
        }
        chart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            textSize = 10f
            setLabelCount(2)
            granularity = 30f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
            }
        }
    }

    private fun renderAssetLine(chart: LineChart, values: List<Float>) {
        val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "资产").apply {
            mode = LineDataSet.Mode.LINEAR
            color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            setFillDrawable(ContextCompat.getDrawable(context, R.drawable.fade_asset))
            highLightColor = 0
        }
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = (values.size - 1).toFloat()
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun money2(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

    private fun renderYear(state: StatisticsYearUi) {
        yearList.removeAllViews()
        state.yearLabels.forEachIndexed { index, label ->
            val selected = index == state.selectedIndex
            val item = LayoutInflater.from(context).inflate(R.layout.item_month_or_year, yearList, false)
            val name = item.findViewById<TextView>(R.id.name)
            val icon = item.findViewById<ImageView>(R.id.choice_icon)
            name.text = label
            name.setTextColor(ContextCompat.getColor(context,
                if (selected) R.color.colorPrimary else R.color.darkGrey))
            name.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            icon.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            yearList.addView(item)
        }
        renderThreeToggle(yearBarPay, yearBarIncome, yearBarBalance)
        renderThreeToggle(heatPay, heatIncome, heatBalance)
        yearPay.text = money2(state.pay)
        yearIncome.text = money2(state.income)
        yearBalance.text = money2(state.balance)
        renderYearBars(state.monthlyPay)
        yearHeatAdapter.submit(state)
    }

    private fun configureYearBarChart(chart: BarChart) {
        chart.setViewPortOffsets(dp(10).toFloat(), dp(20).toFloat(), dp(10).toFloat(), dp(20).toFloat())
        chart.setDrawBarShadow(false)
        chart.description.isEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setTouchEnabled(false)
        chart.setDragEnabled(false)
        chart.setScaleEnabled(false)
        chart.setBackgroundColor(0)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.apply {
            axisMinimum = 0f
            setDrawLabels(false)
            setDrawAxisLine(false)
            setDrawGridLines(false)
            setDrawZeroLine(false)
        }
        chart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            textSize = 10f
            setLabelCount(12)
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
            }
        }
        chart.renderer = RoundedBarChartRenderer(chart, chart.animator, chart.viewPortHandler)
    }

    private fun renderYearBars(values: List<Float>) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "收支").apply {
            color = ContextCompat.getColor(context, R.color.moneyPay)
            highLightColor = 0
            setDrawValues(true)
            valueTextSize = 8f
            valueTextColor = ContextCompat.getColor(context, R.color.black)
        }
        val data = BarData(dataSet)
        data.barWidth = 0.5f
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                if (value == 0f) "" else String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        })
        yearBar.xAxis.axisMinimum = -0.5f
        yearBar.xAxis.axisMaximum = values.size - 0.5f
        yearBar.data = data
        yearBar.invalidate()
    }

    private inner class HeatAdapter : RecyclerView.Adapter<HeatHolder>() {
        private var state: StatisticsYearUi? = null

        fun submit(state: StatisticsYearUi) {
            this.state = state
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HeatHolder =
            HeatHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_heat_map, parent, false))

        override fun getItemCount(): Int = 12

        override fun onBindViewHolder(holder: HeatHolder, position: Int) {
            val year = state ?: return
            holder.label.text = "${position + 1}月"
            holder.monthView.setData(
                java.time.YearMonth.of(year.year, position + 1),
                YimuHeatMapMonthView.PAY,
                year.dailyPay.getOrElse(position) { emptyList() },
                year.dailyIncome.getOrElse(position) { emptyList() },
                year.maxPay,
                year.maxIncome,
            )
        }
    }

    private fun renderSelf(state: StatisticsSelfUi) {
        selfStart.text = state.startLabel
        selfEnd.text = state.endLabel
        selfLineStart.text = state.startLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfLineEnd.text = state.endLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfPay.text = money2(state.pay)
        selfIncome.text = money2(state.income)
        selfBalance.text = money2(state.balance)
        selfView.findViewById<TextView>(R.id.asset_line_start_time).text =
            state.startLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfView.findViewById<TextView>(R.id.asset_line_end_time).text =
            state.endLabel.replace("年", ".").replace("月", ".").replace("日", "")
        renderBillLines(state.dailyPay, state.dailyIncome)
        renderAssetLine(selfAssetLine, state.dailyPay.map { 0f })
    }

    private fun configureBillLineChart(chart: LineChart) {
        chart.setViewPortOffsets(dp(40).toFloat(), dp(20).toFloat(), dp(15).toFloat(), dp(5).toFloat())
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDragEnabled(false)
        chart.setScaleEnabled(false)
        chart.setDrawBorders(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setBackgroundColor(0)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.isEnabled = false
    }

    private fun renderBillLines(pay: List<Float>, income: List<Float>) {
        val paySet = LineDataSet(pay.mapIndexed { index, value -> Entry(index.toFloat(), value) }, "支出").apply {
            color = ContextCompat.getColor(context, R.color.moneyPay)
            lineWidth = 2f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            cubicIntensity = 0.2f
            setDrawCircles(false)
            circleRadius = 2.5f
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(context, R.drawable.fade_pay)
            setCircleColor(ContextCompat.getColor(context, R.color.moneyPay))
            setDrawValues(false)
            highLightColor = 0
        }
        val incomeSet = LineDataSet(income.mapIndexed { index, value -> Entry(index.toFloat(), value) }, "收入").apply {
            color = ContextCompat.getColor(context, R.color.moneyIncome)
            lineWidth = 2f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            cubicIntensity = 0.2f
            setDrawCircles(false)
            circleRadius = 2.5f
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(context, R.drawable.fade_income)
            setCircleColor(ContextCompat.getColor(context, R.color.moneyIncome))
            setDrawValues(false)
            highLightColor = 0
        }
        val maxValue = (pay + income).maxOrNull() ?: 0f
        selfLine.axisLeft.axisMinimum = -maxValue * 0.1f
        selfLine.axisLeft.resetAxisMaximum()
        selfLine.data = LineData(listOf(paySet, incomeSet))
        selfLine.invalidate()
    }

    private fun renderThreeToggle(pay: TextView, income: TextView, balance: TextView) {
        val primary = ContextCompat.getColor(context, R.color.colorPrimary)
        pay.background = pill(primary)
        income.background = null
        balance.background = null
        pay.setTextColor(Color.WHITE)
        income.setTextColor(primary)
        balance.setTextColor(primary)
    }

    private fun renderToggle(pay: TextView, income: TextView, expenseMode: Boolean) {
        val primary = ContextCompat.getColor(context, R.color.colorPrimary)
        pay.background = if (expenseMode) pill(primary) else null
        income.background = if (!expenseMode) pill(primary) else null
        pay.setTextColor(if (expenseMode) Color.WHITE else primary)
        income.setTextColor(if (!expenseMode) Color.WHITE else primary)
    }

    private fun pill(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = 5 * resources.displayMetrics.density
    }

    private fun configureBarChart(chart: BarChart) {
        val density = resources.displayMetrics.density
        chart.setViewPortOffsets(10 * density, 20 * density, 10 * density, 20 * density)
        chart.setDrawBarShadow(false)
        chart.description.isEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setBackgroundColor(0)
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawZeroLine(false)
        chart.legend.isEnabled = false
        chart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            setDrawAxisLine(true)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            textSize = 10f
            granularity = 1f
            labelCount = 7
        }
    }

    private fun renderBars(values: List<Float>, labels: List<String>, colorRes: Int) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "收支").apply {
            color = ContextCompat.getColor(context, colorRes)
            highLightColor = 0
            setDrawValues(true)
            valueTextSize = 8f
            valueTextColor = ContextCompat.getColor(context, R.color.black)
        }
        val data = BarData(dataSet)
        data.barWidth = 0.5f
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                if (value == 0f) "" else trim(value.toDouble())
        })
        normalBar.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                labels.getOrNull(value.toInt().coerceIn(0, labels.lastIndex)).orEmpty()
        }
        normalBar.xAxis.axisMinimum = -0.5f
        normalBar.xAxis.axisMaximum = values.size - 0.5f
        normalBar.data = data
        normalBar.invalidate()
    }

    private fun configurePieChart(chart: PieChart) {
        chart.setExtraOffsets(0f, 10f, 0f, 10f)
        chart.setDrawEntryLabels(false)
        chart.description.isEnabled = false
        chart.dragDecelerationFrictionCoef = 0.95f
        chart.isDrawHoleEnabled = true
        chart.setHoleColor(0)
        chart.setTransparentCircleAlpha(110)
        chart.holeRadius = 62f
        chart.transparentCircleRadius = 70f
        chart.setDrawCenterText(false)
        chart.rotationAngle = 0f
        chart.isHighlightPerTapEnabled = false
        chart.setUsePercentValues(true)
        chart.legend.apply {
            isEnabled = false
            setDrawInside(false)
        }
    }

    private fun renderPie(chart: PieChart, entries: List<Pair<String, Double>>) {
        val slices = if (entries.isEmpty()) listOf(PieEntry(1f, "")) else entries.map { PieEntry(it.second.toFloat(), it.first) }
        val colors = if (entries.isEmpty()) listOf(ContextCompat.getColor(context, R.color.pieChartNone))
        else entries.indices.map { PIE_COLORS[it % PIE_COLORS.size] }
        val set = PieDataSet(slices, "").apply {
            sliceSpace = 3f
            selectionShift = 5f
            this.colors = colors
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }
        val data = PieData(set)
        data.setDrawValues(false)
        chart.setDrawEntryLabels(false)
        chart.data = data
        chart.setTransparentCircleColor(Color.WHITE)
        chart.highlightValues(null)
        chart.invalidate()
    }

    private fun trim(value: Double): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    private companion object {
        val PIE_COLORS = intArrayOf(
            0xFF00ADB5.toInt(), 0xFFF08A5D.toInt(), 0xFFF9ED69.toInt(), 0xFF95E1D3.toInt(),
            0xFFEAFFD0.toInt(), 0xFFFCE38A.toInt(), 0xFFF38181.toInt(), 0xFFFC5185.toInt(),
            0xFFFCBAD3.toInt(), 0xFFAA96DA.toInt(), 0xFFA8D8EA.toInt(), 0xFF8785A2.toInt(),
            0xFF3F72AF.toInt(), 0xFFFFB6B9.toInt(), 0xFFDEFCF9.toInt(), 0xFF5782BB.toInt(),
            0xFF64D7D6.toInt(), 0xFFFFFEEC.toInt(), 0xFFC4AFF0.toInt(), 0xFFFA7F7F.toInt(),
            0xFFF8FBA2.toInt(), 0xFFA0E4B0.toInt(), 0xFF1AA59A.toInt(),
        )
    }
}

private class HeatHolder(view: View) : RecyclerView.ViewHolder(view) {
    val label: TextView = view.findViewById(R.id.month_info)
    val monthView: YimuHeatMapMonthView = view.findViewById(R.id.month_info_view)
}
