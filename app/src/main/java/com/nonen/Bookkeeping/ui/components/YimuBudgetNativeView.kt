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
        tipLayout = findViewById(R.id.tip_layout)
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
        dayPay.text = money(if (hasBudget) totalExpense / 31.0 else 0.0)
        budgetDay.text = money(totalBudget / 31.0)
        tipLayout.visibility = if (categoryCount == 0) View.VISIBLE else View.GONE
        renderChart()
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
            axisMinimum = -1.1f
            axisMaximum = 1.1f
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
            setLabelCount(15)
            granularity = 2f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
            }
        }
    }

    private fun renderChart() {
        val entries = (0..30).map { Entry(it.toFloat(), 0f) }
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
        lineChart.xAxis.axisMinimum = -0.5f
        lineChart.xAxis.axisMaximum = 30.5f
        lineChart.data = LineData(listOf(pay, remaining))
        lineChart.invalidate()
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)
}
