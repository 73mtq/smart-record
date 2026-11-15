package com.nonen.Bookkeeping.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.haibin.calendarview.MonthView
import com.haibin.calendarview.WeekView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.stats.CalendarDaySummary
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/*
 * 原版 fragment_calendar.xml 的 CalendarView 为 wrap_content：星期头约 40dp + 行数 × 55dp。
 * 参考机 2026-11（周日起算 5 行）实测 1028px = 316.3dp，以此作为 5 行基线换算其余行数。
 */
internal fun calendarRows(year: Int, month: Int): Int {
    val first = LocalDate.of(year, month, 1)
    val offset = first.dayOfWeek.value % 7
    return (offset + first.lengthOfMonth() + 6) / 7
}

internal fun calendarHeightPx(year: Int, month: Int, density: Float): Int {
    val rows = calendarRows(year, month)
    return Math.round((316.3f + (rows - 5) * 55f) * density)
}

class YimuCalendarView(context: Context, attrs: AttributeSet?) : CalendarView(context, attrs) {
    var summaries: Map<LocalDate, CalendarDaySummary> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            // 原版 CalendarFragment 未启用 scheme 日期（ActivityMonthView.onDrawScheme 为空），
            // 这里用空 scheme 调用 setSchemeDate，只为触发月/周视图 invalidate 重绘新数据。
            setSchemeDate(emptyMap<String, Calendar>())
        }
}

/*
 * 原版 ActivityMonthView.y()/v() 与 CustomWeekView.y()/v() 的绘制公式。
 * 月视图的 y 为单元格顶部坐标，周视图固定为 0；均为纯计算，便于 JVM 单测。
 */
internal fun dayTextBaseline(textBaseline: Float, y: Int, itemHeight: Int): Float =
    textBaseline + (y - itemHeight / 4)

internal fun lunarTextBaseline(textBaseline: Float, y: Int, itemHeight: Int): Float =
    textBaseline + y + itemHeight / 10

internal fun payTextBaseline(textBaseline: Float, y: Int): Float = textBaseline + y

internal fun incomeTextBaseline(textBaseline: Float, y: Int, itemHeight: Int): Float =
    textBaseline + y + itemHeight / 5

internal data class CalendarRoundRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal fun selectedRoundRect(x: Int, y: Int, itemWidth: Int, itemHeight: Int, padding: Float): CalendarRoundRect =
    CalendarRoundRect(
        left = x + padding,
        top = y + padding,
        right = x + itemWidth - padding,
        bottom = y + itemHeight - padding,
    )

/* 原版用 blankj SizeUtils.dp2px(3.0f)，结果为四舍五入后的整数像素 */
private fun View.selectionInset(): Float = ((3f * resources.displayMetrics.density) + 0.5f).toInt().toFloat()

private fun View.calendarHost(): YimuCalendarView {
    var current = parent
    while (current != null) {
        if (current is YimuCalendarView) return current
        current = current.parent
    }
    error("CalendarView parent is required")
}

private class DayRenderer(private val view: View, private val week: Boolean = false) {
    private val density = view.resources.displayMetrics.density
    private val weekendColor = ContextCompat.getColor(view.context, R.color.colorPrimary)
    private val futureColor = ContextCompat.getColor(view.context, if (week) R.color.grey else R.color.darkGrey)

    fun text(canvas: Canvas, calendar: Calendar, x: Int, y: Int, width: Int, height: Int, baseline: Float,
        selected: Boolean, day: Paint, lunar: Paint) {
        val entry = view.calendarHost().summaries[LocalDate.of(calendar.year, calendar.month, calendar.day)]
        val center = x + width / 2f
        // 本项目 CalendarView 的单元格实测比原版多约 3px，绘制统一上移 2px 对齐参考机
        val top = y - 2f
        val lunarSize = 9f * density
        lunar.textSize = lunarSize
        if (!selected && !calendar.isCurrentDay) {
            val future = calendar.timeInMillis > System.currentTimeMillis()
            day.color = when {
                calendar.isWeekend && calendar.isCurrentMonth -> if (future) futureColor else weekendColor
                future -> futureColor
                else -> ContextCompat.getColor(view.context, R.color.black)
            }
        }
        canvas.drawText(if (calendar.isCurrentDay && !selected) "今" else calendar.day.toString(), center,
            dayTextBaseline(baseline, y, height) - 2f, day)
        if (entry == null || entry.transactionCount == 0) {
            lunar.isFakeBoldText = false
            if (!selected) lunar.color = ContextCompat.getColor(view.context, R.color.grey)
            canvas.drawText(calendar.lunar, center,
                lunarTextBaseline(baseline, y, height) - 2f - if (week) 1f else 0f, lunar)
        } else {
            // 原版直接用 mCurMonthLunarTextPaint / mSelectedLunarTextPaint 绘制金额，复用同一支画笔保证字形与字重一致
            lunar.textSize = lunarSize
            lunar.isFakeBoldText = true
            lunar.color = if (selected) ContextCompat.getColor(view.context, R.color.white)
            else ContextCompat.getColor(view.context, R.color.moneyPay)
            canvas.drawText("-${amount(entry.totalExpense)}", center, payTextBaseline(baseline, y) - 2f, lunar)
            lunar.color = if (selected) ContextCompat.getColor(view.context, R.color.white)
            else ContextCompat.getColor(view.context, R.color.moneyIncome)
            canvas.drawText("+${amount(entry.totalIncome)}", center, incomeTextBaseline(baseline, y, height) - 2f, lunar)
        }
    }

    private fun amount(value: Double) = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

class ActivityMonthView(context: Context) : MonthView(context) {
    private val renderer = DayRenderer(this)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 参考机实测：本视图行高比原版多约 3px，居中补偿后回到原版单元格高度
        mItemHeight -= (0.9f * resources.displayMetrics.density).roundToInt()
    }

    override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean): Boolean {
        val padding = selectionInset()
        val rect = selectedRoundRect(x, y - 2, mItemWidth, mItemHeight, padding)
        canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, padding, padding, mSelectedPaint)
        return true
    }
    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) = Unit
    override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean,
        isSelected: Boolean) {
        val paint = when {
            isSelected -> mSelectTextPaint
            calendar.isCurrentDay -> mCurDayTextPaint
            else -> mCurMonthTextPaint
        }
        renderer.text(canvas, calendar, x, y, mItemWidth, mItemHeight, mTextBaseLine, isSelected,
            paint, if (isSelected) mSelectedLunarTextPaint else mCurMonthLunarTextPaint)
    }
}

class CustomWeekView(context: Context) : WeekView(context) {
    private val renderer = DayRenderer(this, week = true)
    override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, hasScheme: Boolean): Boolean {
        val padding = selectionInset()
        val rect = selectedRoundRect(x, 0, mItemWidth, mItemHeight, padding)
        canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, padding, padding, mSelectedPaint)
        return true
    }
    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int) = Unit
    override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, hasScheme: Boolean, isSelected: Boolean) {
        val paint = when {
            isSelected -> mSelectTextPaint
            calendar.isCurrentDay -> mCurDayTextPaint
            else -> mCurMonthTextPaint
        }
        renderer.text(canvas, calendar, x, 0, mItemWidth, mItemHeight, mTextBaseLine, isSelected,
            paint, if (isSelected) mSelectedLunarTextPaint else mCurMonthLunarTextPaint)
    }
}
