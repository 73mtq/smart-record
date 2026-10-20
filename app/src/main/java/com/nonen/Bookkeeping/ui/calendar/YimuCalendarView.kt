package com.nonen.Bookkeeping.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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

class YimuCalendarView(context: Context, attrs: AttributeSet?) : CalendarView(context, attrs) {
    var summaries: Map<LocalDate, CalendarDaySummary> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            setSchemeDate(value.keys.associate { day ->
                val calendar = Calendar().apply { year = day.year; month = day.monthValue; this.day = day.dayOfMonth; scheme = "record" }
                calendar.toString() to calendar
            })
        }
}

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
    private val money = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 7 * density }
    private val futureColor = ContextCompat.getColor(view.context, if (week) R.color.grey else R.color.darkGrey)

    fun text(canvas: Canvas, calendar: Calendar, x: Int, y: Int, width: Int, height: Int, baseline: Float,
        selected: Boolean, day: Paint, lunar: Paint) {
        val entry = view.calendarHost().summaries[LocalDate.of(calendar.year, calendar.month, calendar.day)]
        val center = x + width / 2f
        val top = y - 2f
        val lunarSize = 9f * density
        lunar.textSize = lunarSize
        if (!selected && calendar.isCurrentMonth) {
            day.color = when {
                calendar.isCurrentDay -> ContextCompat.getColor(view.context, R.color.colorPrimary)
                calendar.timeInMillis > System.currentTimeMillis() -> futureColor
                calendar.isWeekend -> ContextCompat.getColor(view.context, R.color.colorPrimary)
                else -> Color.BLACK
            }
        }
        canvas.drawText(if (calendar.isCurrentDay && !selected) "今" else calendar.day.toString(), center,
            top + baseline - height / 4, day)
        if (entry == null || entry.transactionCount == 0) {
            if (!selected) {
                lunar.color = ContextCompat.getColor(view.context, R.color.grey)
                lunar.isFakeBoldText = false
            }
            val lunarY = top + baseline + height / 10 - if (week) 1f else 0f
            canvas.drawText(calendar.lunar, center, lunarY, lunar)
        } else {
            money.textSize = lunarSize
            money.typeface = lunar.typeface
            money.isFakeBoldText = true
            money.color = if (selected) Color.WHITE else ContextCompat.getColor(view.context, R.color.moneyPay)
            canvas.drawText("-${amount(entry.totalExpense)}", center, top + baseline, money)
            money.color = if (selected) Color.WHITE else ContextCompat.getColor(view.context, R.color.moneyIncome)
            canvas.drawText("+${amount(entry.totalIncome)}", center, top + baseline + height / 5, money)
        }
    }

    private fun amount(value: Double) = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

class ActivityMonthView(context: Context) : MonthView(context) {
    private val renderer = DayRenderer(this)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mItemHeight -= (0.9f * resources.displayMetrics.density).roundToInt()
    }
    override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean): Boolean {
        val padding = 3 * resources.displayMetrics.density
        val top = y - 2f
        canvas.drawRoundRect(x + padding, top + padding, x + mItemWidth - padding, top + mItemHeight - padding, padding, padding, mSelectedPaint)
        return true
    }
    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) = Unit
    override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean, isSelected: Boolean) {
        val paint = when {
            isSelected -> mSelectTextPaint
            calendar.isCurrentDay -> mCurDayTextPaint
            !calendar.isCurrentMonth -> mOtherMonthTextPaint
            else -> mCurMonthTextPaint
        }
        renderer.text(canvas, calendar, x, y, mItemWidth, mItemHeight, mTextBaseLine, isSelected,
            paint, if (isSelected) mSelectedLunarTextPaint else mCurMonthLunarTextPaint)
    }
}

class CustomWeekView(context: Context) : WeekView(context) {
    private val renderer = DayRenderer(this, week = true)
    override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, hasScheme: Boolean): Boolean {
        val padding = 3 * resources.displayMetrics.density
        canvas.drawRoundRect(x + padding, padding, x + mItemWidth - padding, mItemHeight - padding, padding, padding, mSelectedPaint)
        return true
    }
    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int) = Unit
    override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, hasScheme: Boolean, isSelected: Boolean) {
        renderer.text(canvas, calendar, x, 0, mItemWidth, mItemHeight, mTextBaseLine, isSelected,
            if (isSelected) mSelectTextPaint else if (calendar.isCurrentDay) mCurDayTextPaint else mCurMonthTextPaint,
            if (isSelected) mSelectedLunarTextPaint else mCurMonthLunarTextPaint)
    }
}
