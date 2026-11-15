package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.nonen.Bookkeeping.stats.CategoryRow
import com.nonen.Bookkeeping.stats.DayFormRow
import com.nonen.Bookkeeping.stats.TagStat
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
    val categories: List<CategoryRow> = emptyList(),
    val incomeCategoryEntries: List<Pair<String, Double>> = emptyList(),
    val incomeCategories: List<CategoryRow> = emptyList(),
    val dayForms: List<DayFormRow> = emptyList(),
    val tags: List<TagStat> = emptyList(),
    val tagExpenseMode: Boolean = true,
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
    val categoryEntries: List<Pair<String, Double>> = emptyList(),
    val categories: List<CategoryRow> = emptyList(),
    val dayForms: List<DayFormRow> = emptyList(),
    val tags: List<TagStat> = emptyList(),
    val tagExpenseMode: Boolean = true,
    val assetTrend: List<Float> = emptyList(),
)

data class StatisticsSelfUi(
    val startLabel: String,
    val endLabel: String,
    val pay: Double,
    val income: Double,
    val balance: Double,
    val dailyPay: List<Float>,
    val dailyIncome: List<Float>,
    val categoryEntries: List<Pair<String, Double>> = emptyList(),
    val categories: List<CategoryRow> = emptyList(),
    val dayForms: List<DayFormRow> = emptyList(),
    val tags: List<TagStat> = emptyList(),
    val tagExpenseMode: Boolean = true,
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

        /** 月统计页顶部的月份条：0 为本月，1 为上月，依次向前。 */
        fun onMonthSelected(index: Int) {}

        /** 年统计页顶部的年份条：0 为所有，1 为今年，2 为去年，依次向前。 */
        fun onYearSelected(index: Int) {}

        /** 自定义统计页的起止日期，点击后由外层弹出日期选择。 */
        fun onSelfStartClick() {}

        fun onSelfEndClick() {}
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
    private val selfPieTitle = selfView.findViewById<TextView>(R.id.pie_title)
    private val selfPieParent = selfView.findViewById<TextView>(R.id.pie_parent)
    private val selfPieAll = selfView.findViewById<TextView>(R.id.pie_all)
    private val monthLine = monthView.findViewById<LineChart>(R.id.line_chart)
    private val monthCategoryChart = monthView.findViewById<PieChart>(R.id.pie_chart)
    private val monthCategoryList = monthView.findViewById<RecyclerView>(R.id.bill_data)
    private val monthDayFormList = monthView.findViewById<RecyclerView>(R.id.day_form_data)
    private val monthTagChart = monthView.findViewById<PieChart>(R.id.tag_pie_chart)
    private val monthTagCheck = monthView.findViewById<View>(R.id.tag_chart_check)
    private val monthTagCheckText = monthView.findViewById<TextView>(R.id.tag_chart_check_text)
    private val monthTagCheckIcon = monthView.findViewById<ImageView>(R.id.tag_chart_check_icon)
    private val monthTagCheckTip = monthView.findViewById<TextView>(R.id.tag_chart_check_tip_text)
    private val monthTagNumCheck = monthView.findViewById<CheckBox>(R.id.tag_num_check)
    private val monthTagList = monthView.findViewById<RecyclerView>(R.id.tag_data)
    private val yearLine = yearView.findViewById<LineChart>(R.id.line_chart)
    private val yearCategoryChart = yearView.findViewById<PieChart>(R.id.pie_chart)
    private val yearCategoryList = yearView.findViewById<RecyclerView>(R.id.bill_data)
    private val yearDayFormList = yearView.findViewById<RecyclerView>(R.id.month_form_data)
    private val yearTagChart = yearView.findViewById<PieChart>(R.id.tag_pie_chart)
    private val yearTagCheck = yearView.findViewById<View>(R.id.tag_chart_check)
    private val yearTagCheckText = yearView.findViewById<TextView>(R.id.tag_chart_check_text)
    private val yearTagCheckIcon = yearView.findViewById<ImageView>(R.id.tag_chart_check_icon)
    private val yearTagCheckTip = yearView.findViewById<TextView>(R.id.tag_chart_check_tip_text)
    private val yearTagNumCheck = yearView.findViewById<CheckBox>(R.id.tag_num_check)
    private val yearTagList = yearView.findViewById<RecyclerView>(R.id.tag_data)
    private val selfCategoryChart = selfView.findViewById<PieChart>(R.id.pie_chart)
    private val selfCategoryList = selfView.findViewById<RecyclerView>(R.id.bill_data)
    private val selfTagChart = selfView.findViewById<PieChart>(R.id.tag_pie_chart)
    private val selfTagCheck = selfView.findViewById<View>(R.id.tag_chart_check)
    private val selfTagCheckText = selfView.findViewById<TextView>(R.id.tag_chart_check_text)
    private val selfTagCheckIcon = selfView.findViewById<ImageView>(R.id.tag_chart_check_icon)
    private val selfTagCheckTip = selfView.findViewById<TextView>(R.id.tag_chart_check_tip_text)
    private val selfTagNumCheck = selfView.findViewById<CheckBox>(R.id.tag_num_check)
    private val selfTagList = selfView.findViewById<RecyclerView>(R.id.tag_data)
    private var listener: Listener? = null
    private var lastState: StatisticsUiState? = null
    private var monthTagExpenseMode = true
    private var yearTagExpenseMode = true
    private var selfTagExpenseMode = true
    private var monthCategoryExpenseMode = true
    private var monthDayFormSort = 0
    private var monthTagSort = 0
    private var yearDayFormSort = 0
    private var yearTagSort = 0
    private var selfTagSort = 0
    private val tagAdapter = TagAdapter()
    private val yearTagAdapter = TagAdapter()
    private val selfTagAdapter = TagAdapter()
    private val categoryAdapter = CategoryAdapter()
    private val yearCategoryAdapter = CategoryAdapter()
    private val selfCategoryAdapter = CategoryAdapter()
    private val dayFormAdapter = DayFormAdapter()
    private val yearDayFormAdapter = DayFormAdapter()

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
        configurePieChart(monthTagChart)
        configureYearBarChart(yearBar)
        configureMonthLineChart(yearLine)
        configurePieChart(yearCategoryChart)
        configurePieChart(yearTagChart)
        configurePieChart(selfCategoryChart)
        configurePieChart(selfTagChart)
        monthCategoryList.layoutManager = LinearLayoutManager(context)
        monthCategoryList.adapter = categoryAdapter
        monthDayFormList.layoutManager = LinearLayoutManager(context)
        monthDayFormList.adapter = dayFormAdapter
        monthTagList.layoutManager = LinearLayoutManager(context)
        monthTagList.adapter = tagAdapter
        yearCategoryList.layoutManager = LinearLayoutManager(context)
        yearCategoryList.adapter = yearCategoryAdapter
        yearDayFormList.layoutManager = LinearLayoutManager(context)
        yearDayFormList.adapter = yearDayFormAdapter
        yearTagList.layoutManager = LinearLayoutManager(context)
        yearTagList.adapter = yearTagAdapter
        selfCategoryList.layoutManager = LinearLayoutManager(context)
        selfCategoryList.adapter = selfCategoryAdapter
        selfTagList.layoutManager = LinearLayoutManager(context)
        selfTagList.adapter = selfTagAdapter
        yearHeatList.layoutManager = GridLayoutManager(context, 4)
        yearHeatList.adapter = yearHeatAdapter
        yearView.findViewById<View>(R.id.member_layout).visibility = View.GONE
        selfView.findViewById<View>(R.id.member_layout).visibility = View.GONE
        configureSelfLineChart(selfLine, commaFormatter = true)
        configureSelfLineChart(selfAssetLine, commaFormatter = false)
        // 参考资产走势（空数据平线 0.0）轴刻度 ±1.0 步 0.5，等效轴范围 ±1.2（实测标签间距 84.8px/407px plot）
        selfAssetLine.axisLeft.apply {
            axisMinimum = -1.2f
            axisMaximum = 1.2f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    String.format(Locale.US, "%.1f", value)
            }
        }
        monthTagNumCheck.setOnCheckedChangeListener { _, _ -> lastState?.month?.let { renderMonthTags(it) } }
        yearTagNumCheck.setOnCheckedChangeListener { _, _ -> lastState?.year?.let { renderYearTags(it) } }
        selfTagNumCheck.setOnCheckedChangeListener { _, _ -> lastState?.self?.let { renderSelfTags(it) } }
    }

    fun render(state: StatisticsUiState, listener: Listener) {
        this.listener = listener
        lastState = state
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
        renderToggle(monthPieParent, monthPieAll, true)
        renderMonthBars(state.dailyPay)
        renderAssetLine(monthLine, state.assetTrend)
        renderMonthCategoryBlock(state)
        dayFormAdapter.submit(sortDayForms(state.dayForms, monthDayFormSort))
        val sortBtn = monthView.findViewById<View>(R.id.form_sort_btn)
        sortBtn.setOnClickListener { showDayFormSort(sortBtn, state) }
        renderMonthTags(state)
    }

    private fun showDayFormSort(anchor: View, state: StatisticsMonthUi) {
        val popup = PopupMenu(context, anchor)
        DAY_FORM_SORT_LABELS.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
        popup.setOnMenuItemClickListener { item ->
            monthDayFormSort = item.itemId
            renderMonth(state)
            true
        }
        popup.show()
    }

    /** 报表排序：原版 StatisticsSort 的日期/收入/支出/结余 与升序降序组合。 */
    private fun sortDayForms(rows: List<DayFormRow>, mode: Int): List<DayFormRow> {
        val field = mode / 2
        val comparator = when (field) {
            0 -> compareBy<DayFormRow> { it.label }
            1 -> compareBy { it.income }
            2 -> compareBy { it.pay }
            else -> compareBy { it.balance }
        }
        return if (mode % 2 == 1) rows.sortedWith(comparator.reversed()) else rows.sortedWith(comparator)
    }

    /** 分类占比区块：原版 i4.U1/T1，中心提示按支出与收入数据是否都非空决定是否可点击切换。 */
    private fun renderMonthCategoryBlock(state: StatisticsMonthUi) {
        val check = monthView.findViewById<View>(R.id.pie_chart_check)
        val checkText = monthView.findViewById<TextView>(R.id.pie_chart_check_text)
        val icon = monthView.findViewById<ImageView>(R.id.pie_chart_check_icon)
        val tip = monthView.findViewById<TextView>(R.id.pie_chart_check_tip_text)
        val title = monthView.findViewById<TextView>(R.id.pie_title)
        val dataTitle = monthView.findViewById<TextView>(R.id.data_title)
        val hasExpense = state.categoryEntries.isNotEmpty()
        val hasIncome = state.incomeCategoryEntries.isNotEmpty()
        check.visibility = View.VISIBLE
        when {
            !hasExpense && !hasIncome -> {
                check.visibility = View.GONE
                check.setOnClickListener(null)
            }
            !hasExpense || !hasIncome -> {
                icon.visibility = View.GONE
                tip.visibility = View.GONE
                check.setOnClickListener(null)
            }
            else -> {
                icon.visibility = View.VISIBLE
                tip.visibility = View.VISIBLE
                check.setOnClickListener {
                    monthCategoryExpenseMode = !monthCategoryExpenseMode
                    renderMonthCategoryBlock(state)
                }
            }
        }
        val useExpense = monthCategoryExpenseMode && hasExpense
        title.text = if (useExpense) "支出占比" else "收入占比"
        dataTitle.text = if (useExpense) "分类数据" else "收入数据"
        checkText.text = if (useExpense) "支出" else "收入"
        tip.text = if (useExpense) "切换到收入" else "切换到支出"
        renderCategoryPie(monthCategoryChart, if (useExpense) state.categoryEntries else state.incomeCategoryEntries)
        categoryAdapter.submit(if (useExpense) state.categories else state.incomeCategories)
    }

    private fun renderMonthTags(state: StatisticsMonthUi) {
        renderTagBlock(
            chart = monthTagChart,
            check = monthTagCheck,
            checkText = monthTagCheckText,
            checkIcon = monthTagCheckIcon,
            checkTip = monthTagCheckTip,
            list = monthTagList,
            adapter = tagAdapter,
            rows = sortTags(state.tags, monthTagSort),
            expenseMode = monthTagExpenseMode,
            showAmount = monthTagNumCheck.isChecked,
            onExpenseModeChange = { monthTagExpenseMode = it; renderMonthTags(state) },
        )
        val sortBtn = monthView.findViewById<View>(R.id.tag_sort_btn)
        sortBtn.setOnClickListener { showTagSort(sortBtn, state) }
    }

    private fun showTagSort(anchor: View, state: StatisticsMonthUi) {
        val popup = PopupMenu(context, anchor)
        TAG_SORT_LABELS.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
        popup.setOnMenuItemClickListener { item ->
            monthTagSort = item.itemId
            renderMonthTags(state)
            true
        }
        popup.show()
    }

    /** 标签排序：原版按笔数、支出、收入三个字段的正序与降序。 */
    private fun sortTags(rows: List<TagStat>, mode: Int): List<TagStat> {
        val field = mode / 2
        val comparator = when (field) {
            0 -> compareBy<TagStat> { it.billCount }
            1 -> compareBy { it.pay }
            else -> compareBy { it.income }
        }
        return if (mode % 2 == 1) rows.sortedWith(comparator.reversed()) else rows.sortedWith(comparator)
    }

    private fun renderMonthCategory(entries: List<Pair<String, Double>>) {
        renderCategoryPie(monthCategoryChart, entries)
    }

    /** 分类占比饼图：原版 i4.V0/U0，小于总额 2.5% 的切片钳到 2.5% 参与角度计算，标签百分比用原始值。 */
    private fun renderCategoryPie(chart: PieChart, entries: List<Pair<String, Double>>) {
        val total = entries.sumOf { it.second }
        if (total <= 0.0) {
            renderPie(chart, emptyList())
            return
        }
        val threshold = total * 0.025
        val clampedSum = entries.sumOf { maxOf(it.second, threshold) }
        val slices = entries.map { (name, value) ->
            PieEntry(
                (maxOf(value, threshold) / clampedSum * 100.0).toFloat(),
                "$name ${String.format(Locale.US, "%.2f", value / total * 100)}%",
            )
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
            valueLineColor = ContextCompat.getColor(context, R.color.divider)
        }
        val data = PieData(set)
        data.setDrawValues(false)
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(true)
        chart.setEntryLabelColor(ContextCompat.getColor(context, R.color.darkGrey))
        chart.setEntryLabelTextSize(8f)
        chart.setDrawCenterText(false)
        chart.setHoleColor(0)
        chart.setTransparentCircleColor(Color.WHITE)
        chart.data = data
        chart.highlightValues(null)
        chart.invalidate()
    }

    private fun buildMonthStrip(state: StatisticsMonthUi) {
        monthList.removeAllViews()
        state.monthLabels.forEachIndexed { index, label ->
            val selected = index == state.selectedIndex
            val item = LayoutInflater.from(context).inflate(R.layout.item_month_or_year, monthList, false)
            val name = item.findViewById<TextView>(R.id.name)
            val icon = item.findViewById<ImageView>(R.id.choice_icon)
            name.text = label
            renderChoiceItem(name, icon, selected)
            item.setOnClickListener { listener?.onMonthSelected(index) }
            monthList.addView(item)
        }
    }

    private fun configureMonthBarChart(chart: BarChart) {
        chart.setNoDataText("")
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
        chart.setNoDataText("")
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
            // 参考刻度步长 0.5（标签间距 84.8px）→ 轴范围 ±1.2；±1.1 时 MPChart 算出 0.4 步长（实测）
            axisMinimum = -1.2f
            axisMaximum = 1.2f
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
        // 原版 H1（i4.java:1333）：xAxis labelCount=当月天数 + formatter(index+1)，参考实测 1,3,5…31
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = (values.size - 1).toFloat()
        // 图模板 configureLineChart 为资产页单月视图设过 granularity=30；此处必须回到 1，
        // 否则 MPChart 只生成 0 与 30 两个刻度，日期标签只剩一个（2026-11-12 实测）。
        chart.xAxis.granularity = 1f
        // 参考实测（2026-11 月，30 天）：日期标签为 1,3,5…29 共 15 个，间距约 63.5px。
        // 用 15 个刻度让 MPChart 取间隔 2；用当月天数会得到逐日标签，与参考不符。
        chart.xAxis.setLabelCount(15, false)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun money2(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

    /** 原版 je 适配器：未选中 @color/grey(#999999)、选中 colorPrimaryText 并显示箭头，不加粗（条目 XML 恒为 bold）。 */
    private fun renderChoiceItem(name: TextView, icon: ImageView, selected: Boolean) {
        name.setTextColor(ContextCompat.getColor(context,
            if (selected) R.color.colorPrimaryText else R.color.grey))
        icon.visibility = if (selected) View.VISIBLE else View.GONE
    }

    private fun renderYear(state: StatisticsYearUi) {
        yearList.removeAllViews()
        state.yearLabels.forEachIndexed { index, label ->
            val selected = index == state.selectedIndex
            val item = LayoutInflater.from(context).inflate(R.layout.item_month_or_year, yearList, false)
            val name = item.findViewById<TextView>(R.id.name)
            val icon = item.findViewById<ImageView>(R.id.choice_icon)
            name.text = label
            renderChoiceItem(name, icon, selected)
            item.setOnClickListener { listener?.onYearSelected(index) }
            yearList.addView(item)
        }
        // 年页三联参考弧实测 16px(≈5dp，与容器同心)；normal 页参考弧为 bg_statistics_btn 的 8dp
        renderThreeToggle(yearBarPay, yearBarIncome, yearBarBalance, containerRound = true)
        renderThreeToggle(heatPay, heatIncome, heatBalance, containerRound = true)
        yearPay.text = money2(state.pay)
        yearIncome.text = money2(state.income)
        yearBalance.text = money2(state.balance)
        renderYearBars(state.monthlyPay)
        yearHeatAdapter.submit(state)
        renderAssetLine(yearLine, state.assetTrend)
        renderCategoryPie(yearCategoryChart, state.categoryEntries)
        yearCategoryAdapter.submit(state.categories)
        yearDayFormAdapter.submit(sortDayForms(state.dayForms, yearDayFormSort))
        val sortBtn = yearView.findViewById<View>(R.id.form_sort_btn)
        sortBtn.setOnClickListener { showYearDayFormSort(sortBtn, state) }
        renderYearTags(state)
    }

    private fun showYearDayFormSort(anchor: View, state: StatisticsYearUi) {
        val popup = PopupMenu(context, anchor)
        DAY_FORM_SORT_LABELS.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
        popup.setOnMenuItemClickListener { item ->
            yearDayFormSort = item.itemId
            renderYear(state)
            true
        }
        popup.show()
    }

    private fun renderYearTags(state: StatisticsYearUi) {
        renderTagBlock(
            chart = yearTagChart,
            check = yearTagCheck,
            checkText = yearTagCheckText,
            checkIcon = yearTagCheckIcon,
            checkTip = yearTagCheckTip,
            list = yearTagList,
            adapter = yearTagAdapter,
            rows = sortTags(state.tags, yearTagSort),
            expenseMode = yearTagExpenseMode,
            showAmount = yearTagNumCheck.isChecked,
            onExpenseModeChange = { yearTagExpenseMode = it; renderYearTags(state) },
        )
        val sortBtn = yearView.findViewById<View>(R.id.tag_sort_btn)
        sortBtn.setOnClickListener {
            val popup = PopupMenu(context, sortBtn)
            TAG_SORT_LABELS.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
            popup.setOnMenuItemClickListener { item ->
                yearTagSort = item.itemId
                renderYearTags(state)
                true
            }
            popup.show()
        }
    }

    private fun configureYearBarChart(chart: BarChart) {
        chart.setNoDataText("")
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
            // 原版年图分支 setValueTextSize(6.0f)+textColorBlack（i4.java 1896 行）；8sp 时 cap 高 18px vs 参考 14px
            valueTextSize = 6f
            valueTextColor = ContextCompat.getColor(context, R.color.textColorBlack)
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
        selfStart.setOnClickListener { listener?.onSelfStartClick() }
        selfEnd.text = state.endLabel
        selfEnd.setOnClickListener { listener?.onSelfEndClick() }
        selfLineStart.text = state.startLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfLineEnd.text = state.endLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfPay.text = money2(state.pay)
        selfIncome.text = money2(state.income)
        selfBalance.text = money2(state.balance)
        // 原版 T1/s0 默认支出模式：pie_title 运行时置为"支出占比"，分类切换条默认选中"一级分类"
        selfPieTitle.text = "支出占比"
        renderToggle(selfPieParent, selfPieAll, true)
        selfView.findViewById<TextView>(R.id.asset_line_start_time).text =
            state.startLabel.replace("年", ".").replace("月", ".").replace("日", "")
        selfView.findViewById<TextView>(R.id.asset_line_end_time).text =
            state.endLabel.replace("年", ".").replace("月", ".").replace("日", "")
        renderBillLines(state.dailyPay, state.dailyIncome)
        renderAssetLine(selfAssetLine, state.dailyPay.map { 0f })
        renderCategoryPie(selfCategoryChart, state.categoryEntries)
        selfCategoryAdapter.submit(state.categories)
        renderSelfTags(state)
    }

    private fun renderSelfTags(state: StatisticsSelfUi) {
        renderTagBlock(
            chart = selfTagChart,
            check = selfTagCheck,
            checkText = selfTagCheckText,
            checkIcon = selfTagCheckIcon,
            checkTip = selfTagCheckTip,
            list = selfTagList,
            adapter = selfTagAdapter,
            rows = sortTags(state.tags, selfTagSort),
            expenseMode = selfTagExpenseMode,
            showAmount = selfTagNumCheck.isChecked,
            onExpenseModeChange = { selfTagExpenseMode = it; renderSelfTags(state) },
        )
        val sortBtn = selfView.findViewById<View>(R.id.tag_sort_btn)
        sortBtn.setOnClickListener {
            val popup = PopupMenu(context, sortBtn)
            TAG_SORT_LABELS.forEachIndexed { index, label -> popup.menu.add(0, index, index, label) }
            popup.setOnMenuItemClickListener { item ->
                selfTagSort = item.itemId
                renderSelfTags(state)
                true
            }
            popup.show()
        }
    }

    /** 原版 i4.Q0：轴开启、labelCount(5,false)、左轴 colorPrimary 线、7sp darkGrey、offsets(40,20,15,5)。 */
    private fun configureSelfLineChart(chart: LineChart, commaFormatter: Boolean) {
        chart.setNoDataText("")
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
        chart.xAxis.apply {
            isEnabled = true
            setDrawGridLines(false)
            setPosition(XAxis.XAxisPosition.BOTTOM)
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            setDrawAxisLine(true)
            setDrawLabels(false)
        }
        chart.axisLeft.apply {
            setLabelCount(5, false)
            textColor = ContextCompat.getColor(context, R.color.darkGrey)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            setDrawGridLines(false)
            axisLineColor = ContextCompat.getColor(context, R.color.colorPrimary)
            textSize = 7f
        }
        if (commaFormatter) {
            // 参考 bill 图轴刻度 0 / 1,000 / 2,000 / 3,000（千分位整数）
            val format = java.text.DecimalFormat("#,###")
            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = format.format(value.toDouble())
            }
        }
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
        // 原版 a2 对 bill 图无轴钳制，min/max 由数据加默认 spaceTop/Bottom 决定（参考刻度 0/1,000/2,000/3,000）
        selfLine.axisLeft.resetAxisMinimum()
        selfLine.axisLeft.resetAxisMaximum()
        selfLine.data = LineData(listOf(paySet, incomeSet))
        selfLine.invalidate()
    }

    private fun renderThreeToggle(pay: TextView, income: TextView, balance: TextView, containerRound: Boolean = false) {
        val primary = ContextCompat.getColor(context, R.color.colorPrimary)
        pay.background = if (containerRound) pill(primary)
        else ContextCompat.getDrawable(context, R.drawable.bg_statistics_btn)
        income.background = null
        balance.background = null
        pay.setTextColor(Color.WHITE)
        income.setTextColor(primary)
        balance.setTextColor(primary)
    }

    private fun renderToggle(pay: TextView, income: TextView, expenseMode: Boolean) {
        val primary = ContextCompat.getColor(context, R.color.colorPrimary)
        val selected = ContextCompat.getDrawable(context, R.drawable.bg_statistics_btn)
        pay.background = if (expenseMode) selected else null
        income.background = if (!expenseMode) selected else null
        pay.setTextColor(if (expenseMode) Color.WHITE else primary)
        income.setTextColor(if (!expenseMode) Color.WHITE else primary)
    }

    private fun pill(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = 5 * resources.displayMetrics.density
    }

    private fun configureBarChart(chart: BarChart) {
        chart.setNoDataText("")
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

    /**
     * 原版普通柱图配置核对（i4.java:982-1032 Q1，H0:2594-2620）：
     * 柱顶数值 8f + textColorBlack、LargeValueFormatter.setDigits(2)、viewPortOffsets(10,20,10,20)；
     * barWidth 在数据侧写 0.6f，但 fork BarChartRenderer.java:98 固定 buffer.setBarWidth(0.5f)，本地渲染器同样固定 0.5f。
     * 剩余差异仅是首个柱顶数值右缘 1px 抗锯齿，以上参数均无对应差异，保持现状不做盲调。
     */
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
        // 空数据由 renderPie 的灰色空环承接，禁止图表库绘制默认的无数据文案
        chart.setNoDataText("")
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

    /**
     * 标签占比区块：原版 i4.h2 与 V0。
     * 支出与收入都有标签数据时中心提示可点击切换；只有一侧有数据时固定显示该侧并隐藏切换图标；
     * 两侧都没有时隐藏中心提示，饼图绘制灰色空环（原版 V1），不会出现图表库的无数据文案。
     */
    private fun renderTagBlock(
        chart: PieChart,
        check: View,
        checkText: TextView,
        checkIcon: ImageView,
        checkTip: TextView,
        list: RecyclerView,
        adapter: TagAdapter,
        rows: List<TagStat>,
        expenseMode: Boolean,
        showAmount: Boolean,
        onExpenseModeChange: (Boolean) -> Unit,
    ) {
        val payRows = rows.filter { it.pay > 0.0 }
        val incomeRows = rows.filter { it.income > 0.0 }
        check.visibility = View.VISIBLE
        when {
            payRows.isEmpty() && incomeRows.isEmpty() -> {
                check.visibility = View.GONE
                check.setOnClickListener(null)
            }
            payRows.isEmpty() || incomeRows.isEmpty() -> {
                checkIcon.visibility = View.GONE
                checkTip.visibility = View.GONE
                check.setOnClickListener(null)
            }
            else -> {
                checkIcon.visibility = View.VISIBLE
                checkTip.visibility = View.VISIBLE
                check.setOnClickListener { onExpenseModeChange(!expenseMode) }
            }
        }
        val useExpense = expenseMode && payRows.isNotEmpty()
        val activeRows = if (useExpense) payRows else incomeRows
        checkText.text = if (useExpense) "支出" else "收入"
        adapter.submit(rows)
        val total = activeRows.sumOf { if (useExpense) it.pay else it.income }
        if (total <= 0.0) {
            renderPie(chart, emptyList())
            return
        }
        val threshold = total * 0.025
        val clampedSum = activeRows.sumOf { maxOf(if (useExpense) it.pay else it.income, threshold) }
        val slices = activeRows.map { row ->
            val amount = if (useExpense) row.pay else row.income
            val label = if (showAmount) {
                row.tagName + " " + String.format(Locale.US, "%,.2f", amount)
            } else {
                row.tagName + " " + String.format(Locale.US, "%.2f", amount / total * 100) + "%"
            }
            PieEntry((maxOf(amount, threshold) / clampedSum * 100.0).toFloat(), label)
        }
        val colors = activeRows.indices.map { PIE_COLORS[it % PIE_COLORS.size] }
        val set = PieDataSet(slices, "").apply {
            sliceSpace = 3f
            selectionShift = 5f
            this.colors = colors
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = ContextCompat.getColor(context, R.color.divider)
        }
        val data = PieData(set)
        data.setDrawValues(false)
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(true)
        chart.setEntryLabelColor(ContextCompat.getColor(context, R.color.darkGrey))
        chart.setEntryLabelTextSize(8f)
        chart.setDrawCenterText(false)
        chart.setHoleColor(0)
        chart.setTransparentCircleColor(Color.WHITE)
        chart.data = data
        chart.highlightValues(null)
        chart.invalidate()
    }
    private fun trim(value: Double): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

private val DAY_FORM_SORT_LABELS = listOf("日期↑", "日期↓", "收入↑", "收入↓", "支出↑", "支出↓", "结余↑", "结余↓")
private val TAG_SORT_LABELS = listOf("笔数↑", "笔数↓", "支出↑", "支出↓", "收入↑", "收入↓")

private val PIE_COLORS = intArrayOf(
    0xFF00ADB5.toInt(), 0xFFF08A5D.toInt(), 0xFFF9ED69.toInt(), 0xFF95E1D3.toInt(),
    0xFFEAFFD0.toInt(), 0xFFFCE38A.toInt(), 0xFFF38181.toInt(), 0xFFFC5185.toInt(),
    0xFFFCBAD3.toInt(), 0xFFAA96DA.toInt(), 0xFFA8D8EA.toInt(), 0xFF8785A2.toInt(),
    0xFF3F72AF.toInt(), 0xFFFFB6B9.toInt(), 0xFFDEFCF9.toInt(), 0xFF5782BB.toInt(),
    0xFF64D7D6.toInt(), 0xFFFFFEEC.toInt(), 0xFFC4AFF0.toInt(), 0xFFFA7F7F.toInt(),
    0xFFF8FBA2.toInt(), 0xFFA0E4B0.toInt(), 0xFF1AA59A.toInt(),
)

private class HeatHolder(view: View) : RecyclerView.ViewHolder(view) {
    val label: TextView = view.findViewById(R.id.month_info)
    val monthView: YimuHeatMapMonthView = view.findViewById(R.id.month_info_view)
}

private class TagHolder(view: View) : RecyclerView.ViewHolder(view) {
    val color: View = view.findViewById(R.id.tag_color)
    val name: TextView = view.findViewById(R.id.tag_name)
    val num: TextView = view.findViewById(R.id.tag_num)
    val pay: TextView = view.findViewById(R.id.pay_num)
    val income: TextView = view.findViewById(R.id.income_num)
    val line: View = view.findViewById(R.id.line)
}

/** 标签数据列表：原版 adapter/rg.java 的绑定内容。 */
private class TagAdapter : RecyclerView.Adapter<TagHolder>() {
    private var rows: List<TagStat> = emptyList()

    fun submit(values: List<TagStat>) {
        rows = values
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TagHolder =
        TagHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_statistics_tag_data, parent, false))

    override fun onBindViewHolder(holder: TagHolder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context
        holder.name.text = row.tagName
        holder.num.text = context.getString(R.string.bill_num, row.billCount)
        holder.pay.text = "支：-" + money(row.pay)
        holder.income.text = "收：+" + money(row.income)
        holder.line.visibility = if (position != rows.lastIndex) View.VISIBLE else View.GONE
        holder.color.background = GradientDrawable().apply {
            cornerRadius = 250f * context.resources.displayMetrics.density
            setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        }
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)
}

private class CategoryHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.icon)
    val category: TextView = view.findViewById(R.id.category)
    val billNum: TextView = view.findViewById(R.id.bill_num)
    val percent: TextView = view.findViewById(R.id.percent)
    val progress: YimuCircleLineView = view.findViewById(R.id.progress_view)
    val cost: TextView = view.findViewById(R.id.cost)
    val rate: TextView = view.findViewById(R.id.rate_text)
    val line: View = view.findViewById(R.id.line)
}

/** 分类数据列表：原版 adapter/qg.java 的绑定内容。 */
private class CategoryAdapter : RecyclerView.Adapter<CategoryHolder>() {
    private var rows: List<CategoryRow> = emptyList()
    private val bitmaps = HashMap<String, Bitmap>()

    fun submit(values: List<CategoryRow>) {
        rows = values
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CategoryHolder =
        CategoryHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_statistics_bill_data, parent, false))

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context
        holder.category.text = row.name
        holder.billNum.text = context.getString(R.string.bill_num, row.count)
        holder.percent.text = String.format(Locale.US, "%.2f", row.percent) + "%"
        holder.progress.setLineColor(PIE_COLORS[position % PIE_COLORS.size])
        holder.progress.setProgress(row.percent.toInt())
        holder.cost.text = (if (row.isIncome) "+" else "-") + String.format(Locale.US, "%,.2f", row.amount)
        holder.cost.setTextColor(
            ContextCompat.getColor(context, if (row.isIncome) R.color.moneyIncome else R.color.moneyPay),
        )
        holder.rate.visibility = View.GONE
        holder.line.visibility = if (position != rows.lastIndex) View.VISIBLE else View.GONE
        require(row.iconName.isNotEmpty()) { "分类图标缺失: " + row.name }
        when {
            row.iconName.startsWith("font:") -> holder.icon.setImageDrawable(null)
            row.iconName.startsWith("lineIcon/") -> {
                val bitmap = bitmaps.getOrPut(row.iconName) {
                    context.assets.open("yimu/" + row.iconName).use { stream ->
                        requireNotNull(BitmapFactory.decodeStream(stream))
                    }
                }
                holder.icon.setImageBitmap(bitmap)
            }
            else -> {
                val resource = context.resources.getIdentifier(row.iconName, "mipmap", context.packageName)
                require(resource != 0) { "分类图标资源缺失: " + row.iconName }
                holder.icon.setImageResource(resource)
            }
        }
    }
}

private class DayFormHolder(view: View) : RecyclerView.ViewHolder(view) {
    val date: TextView = view.findViewById(R.id.date)
    val income: TextView = view.findViewById(R.id.income)
    val pay: TextView = view.findViewById(R.id.pay)
    val balance: TextView = view.findViewById(R.id.balance)
}

/** 报表统计列表：原版 adapter/x7.java 的绑定内容。 */
private class DayFormAdapter : RecyclerView.Adapter<DayFormHolder>() {
    private var rows: List<DayFormRow> = emptyList()

    fun submit(values: List<DayFormRow>) {
        rows = values
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DayFormHolder =
        DayFormHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_day_form, parent, false))

    override fun onBindViewHolder(holder: DayFormHolder, position: Int) {
        val row = rows[position]
        holder.date.text = row.label
        holder.income.text = money(row.income)
        holder.pay.text = money(row.pay)
        holder.balance.text = money(row.balance)
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)
}

