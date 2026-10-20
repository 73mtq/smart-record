package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart as NativeBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.nonen.Bookkeeping.ui.theme.ChartColors
import com.nonen.Bookkeeping.R
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.ui.components.localDateOf
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun YimuBarChart(values: List<Pair<String, Double>>, color: Color) {
    val text = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(modifier = Modifier.fillMaxWidth().height(150.dp), factory = { context ->
        NativeBarChart(context).apply {
            id = R.id.bar_chart
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDoubleTapToZoomEnabled(false)
            setNoDataText("")
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textSize = 10f
        }
    }, update = { chart ->
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value.second.toFloat()) }
        chart.data = BarData(BarDataSet(entries, "").apply { this.color = color.toArgb(); setDrawValues(false) }).apply { barWidth = 0.5f }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(values.map { it.first })
        chart.xAxis.textColor = text
        chart.xAxis.axisLineColor = color.toArgb()
        chart.notifyDataSetChanged()
        chart.invalidate()
    })
}

@Composable
fun YimuLineChart(values: List<Pair<String, Double>>, color: Color, viewId: Int = R.id.bill_line_chart) {
    val text = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(modifier = Modifier.fillMaxWidth().height(150.dp), factory = { context ->
        LineChart(context).apply {
            id = viewId
            description.isEnabled = false; legend.isEnabled = false
            axisLeft.isEnabled = false; axisRight.isEnabled = false
            setScaleEnabled(false); setPinchZoom(false); setDoubleTapToZoomEnabled(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textSize = 7f
        }
    }, update = { chart ->
        chart.data = LineData(LineDataSet(values.mapIndexed { index, value -> Entry(index.toFloat(), value.second.toFloat()) }, "").apply {
            this.color = color.toArgb(); lineWidth = 1.5f; setDrawCircles(false); setDrawValues(false)
        })
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(values.map { it.first })
        chart.xAxis.textColor = text
        chart.notifyDataSetChanged(); chart.invalidate()
    })
}

@Composable
fun YimuYearHeatMap(year: Int, transactions: List<TransactionEntity>, mode: Int) {
    val amounts = transactions.groupBy { localDateOf(it.timestamp) }.mapValues { (_, records) ->
        when (mode) {
            0 -> -records.filter { it.amount < 0 }.sumOf { it.amount }
            1 -> records.filter { it.amount > 0 }.sumOf { it.amount }
            else -> records.sumOf { it.amount }
        }
    }
    val positiveMax = amounts.values.filter { it > 0 }.maxOrNull() ?: 0.0
    val negativeMax = amounts.values.filter { it < 0 }.maxOfOrNull { abs(it) } ?: 0.0
    val empty = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        (1..12).chunked(4).forEach { months ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                months.forEach { month ->
                    Column(Modifier.weight(1f)) {
                        Text("${month}月", fontSize = 12.sp, color = empty)
                        Canvas(Modifier.padding(top = 5.dp).size(80.dp, 75.dp)) {
                            val period = YearMonth.of(year, month)
                            val offset = period.atDay(1).dayOfWeek.value % 7
                            val diameter = 8.dp.toPx()
                            val step = diameter + 3.dp.toPx()
                            for (day in 1..period.lengthOfMonth()) {
                                val amount = amounts[LocalDate.of(year, month, day)] ?: 0.0
                                val maximum = if (amount < 0) negativeMax else positiveMax
                                val intensity = if (maximum == 0.0) 0f else sqrt(abs(amount) / maximum).toFloat()
                                val color = if (amount == 0.0) empty else if (mode == 0 || amount < 0)
                                    com.nonen.Bookkeeping.ui.theme.YimuTokens.Pay else com.nonen.Bookkeeping.ui.theme.YimuTokens.Income
                                val cell = offset + day - 1
                                drawCircle(color.copy(alpha = 0.1f + intensity * 0.9f), diameter / 2,
                                    Offset((cell % 7) * step + diameter / 2, (cell / 7) * step + diameter / 2))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YimuPieChart(values: List<Pair<String, Double>>) {
    val surface = MaterialTheme.colorScheme.surface.toArgb()
    AndroidView(modifier = Modifier.fillMaxWidth().height(200.dp), factory = { context ->
        PieChart(context).apply {
            description.isEnabled = false; legend.isEnabled = false
            setDrawEntryLabels(false); holeRadius = 62f; transparentCircleRadius = 70f
            setTransparentCircleAlpha(110); setExtraOffsets(0f, 10f, 0f, 10f)
            setNoDataText("")
        }
    }, update = { chart ->
        chart.setHoleColor(surface)
        chart.data = PieData(PieDataSet(values.filter { it.second > 0 }.map { PieEntry(it.second.toFloat(), it.first) }, "").apply {
            colors = ChartColors.map { it.toArgb() }; setDrawValues(false)
        })
        chart.notifyDataSetChanged(); chart.invalidate()
    })
}
