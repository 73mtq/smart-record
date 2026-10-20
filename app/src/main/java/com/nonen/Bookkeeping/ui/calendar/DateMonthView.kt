package com.nonen.Bookkeeping.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView
import com.nonen.Bookkeeping.R
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min
import kotlin.math.roundToInt

class DateMonthView(context: Context) : MonthView(context) {
    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.backgroundLight)
        style = Paint.Style.FILL
    }
    private val solarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        textAlign = Paint.Align.CENTER
    }
    private val primary = ContextCompat.getColor(context, R.color.colorPrimary)
    private val black = ContextCompat.getColor(context, R.color.black)
    private val grey = ContextCompat.getColor(context, R.color.grey)
    private val dayOffset = -(0.3f * resources.displayMetrics.density)
    private val lunarOffset = -(0.6f * resources.displayMetrics.density)
    private val tomorrowStart = LocalDate.now().plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mItemHeight -= (0.9f * resources.displayMetrics.density).roundToInt()
    }

    override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean): Boolean {
        canvas.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f + dayOffset, radius(), mSelectedPaint)
        return true
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) = Unit

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean,
    ) {
        val cx = x + mItemWidth / 2f
        val future = calendar.timeInMillis >= tomorrowStart
        if (calendar.isCurrentDay && !isSelected) {
            canvas.drawCircle(cx, y + mItemHeight / 2f + dayOffset, radius(), todayPaint)
        }
        val dayColor = when {
            isSelected -> Color.WHITE
            calendar.isCurrentDay -> primary
            !calendar.isCurrentMonth -> grey
            future -> grey
            else -> black
        }
        val dayPaint = if (isSelected) mSelectTextPaint else mCurMonthTextPaint
        dayPaint.color = dayColor
        dayPaint.textScaleX = 1.04f
        canvas.drawText(calendar.day.toString(), cx, y + mTextBaseLine - mItemHeight / 6f + dayOffset, dayPaint)
        val solarTerm = calendar.solarTerm
        val lunarText = if (!solarTerm.isNullOrEmpty()) solarTerm else calendar.lunar
        val lunarColor = when {
            isSelected -> Color.WHITE
            !solarTerm.isNullOrEmpty() -> primary
            calendar.isCurrentDay -> primary
            else -> grey
        }
        val lunarPaint = if (isSelected) mSelectedLunarTextPaint else mCurMonthLunarTextPaint
        lunarPaint.color = lunarColor
        solarPaint.textSize = lunarPaint.textSize
        canvas.drawText(lunarText, cx - if (lunarText.length >= 3) 0f else 1f, y + mTextBaseLine + mItemHeight / 10f + lunarOffset,
            if (!solarTerm.isNullOrEmpty() && !isSelected) solarPaint else lunarPaint)
    }

    private fun radius(): Float = (min(mItemWidth, mItemHeight) / 11) * 5f
}
