package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nonen.Bookkeeping.R
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.sqrt

class YimuHeatMapMonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val dot = 8f * density
    // 原版网格步长为整型像素 dp(8)+dp(3)=26+10=36px；浮点 9.75f 会累积漂移（第 7 颗圆心偏差 1.5px）
    private val gap = Math.round(3f * density).toFloat()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var month = YearMonth.now()
    private var mode = PAY
    private var daily: List<Double> = emptyList()
    private var max = 0.0

    init {
        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.moneyPay)
    }

    fun setData(month: YearMonth, mode: Int, dailyPay: List<Float>, dailyIncome: List<Float>, maxPay: Double, maxIncome: Double) {
        this.month = month
        this.mode = mode
        daily = when (mode) {
            INCOME -> dailyIncome.map { it.toDouble() }
            BALANCE -> dailyIncome.zip(dailyPay) { income, pay -> income - pay }.map { it.toDouble() }
            else -> dailyPay.map { it.toDouble() }
        }
        max = when (mode) {
            INCOME -> maxIncome
            BALANCE -> maxOf(maxPay, maxIncome)
            else -> maxPay
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val offset = month.atDay(1).dayOfWeek.value % 7
        val days = month.lengthOfMonth()
        val cells = if (offset + days <= 35) 35 else 42
        val maxSqrt = sqrt(max)
        for (index in 0 until cells) {
            val day = index - offset
            if (day < 0 || day >= days) continue
            val value = daily.getOrElse(day) { 0.0 }
            val normalized = when (mode) {
                BALANCE -> if (maxSqrt == 0.0) 0.0 else (if (value >= 0) sqrt(value) else -sqrt(-value)) / maxSqrt
                else -> if (maxSqrt == 0.0) 0.0 else sqrt(abs(value)) / maxSqrt
            }
            paint.color = when {
                value == 0.0 -> ContextCompat.getColor(context, R.color.darkGrey)
                mode == BALANCE && value < 0 || mode == PAY -> ContextCompat.getColor(context, R.color.moneyPay)
                else -> ContextCompat.getColor(context, R.color.moneyIncome)
            }
            paint.alpha = (((abs(normalized) * 0.9) + 0.1) * 255).toInt().coerceIn(0, 255)
            val col = index % 7
            val row = index / 7
            val cx = col * (dot + gap) + dot / 2
            val cy = row * (dot + gap) + dot / 2
            canvas.drawCircle(cx, cy, dot / 2, paint)
        }
    }

    companion object {
        const val PAY = 1
        const val INCOME = 2
        const val BALANCE = 3
    }
}
