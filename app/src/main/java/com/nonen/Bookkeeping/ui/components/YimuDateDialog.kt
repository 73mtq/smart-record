package com.nonen.Bookkeeping.ui.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.ui.calendar.YimuCalendarView
import com.zyyoona7.wheel.WheelView
import com.zyyoona7.wheel.adapter.ArrayWheelAdapter
import com.zyyoona7.wheel.listener.OnItemSelectedListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

class YimuDateDialog(
    context: Context,
    initialMillis: Long,
    private val onConfirm: (Long) -> Unit,
) : Dialog(context) {

    private val calendarView: YimuCalendarView
    private val hourPicker: WheelView
    private val minutePicker: WheelView
    private val monthDayText: TextView
    private val yearText: TextView
    private val lunarText: TextView
    private var selected: Calendar
    private var hour: Int
    private var minute: Int

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content = LayoutInflater.from(context).inflate(R.layout.yimu_date_dialog, null, false)
        calendarView = content.findViewById(R.id.calendarView)
        hourPicker = content.findViewById(R.id.hour_picker)
        minutePicker = content.findViewById(R.id.minute_picker)
        monthDayText = content.findViewById(R.id.tv_month_day)
        yearText = content.findViewById(R.id.tv_year)
        lunarText = content.findViewById(R.id.tv_lunar)

        val zoned = Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault())
        hour = zoned.hour
        minute = zoned.minute
        selected = Calendar().apply {
            year = zoned.year
            month = zoned.monthValue
            day = zoned.dayOfMonth
        }
        updateHeader(selected)

        calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
            override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                selected = calendar
                updateHeader(calendar)
            }
            override fun onCalendarOutOfRange(calendar: Calendar) = Unit
        })
        calendarView.post {
            calendarView.scrollToCalendar(selected.year, selected.month, selected.day, false)
        }

        hourPicker.setData((0..23).map { String.format(Locale.US, "%02d", it) })
        hourPicker.setSelectedPosition(hour)
        hourPicker.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(wheelView: WheelView, adapter: ArrayWheelAdapter<*>, position: Int) {
                hour = position
            }
        })
        minutePicker.setData((0..59).map { String.format(Locale.US, "%02d", it) })
        minutePicker.setSelectedPosition(minute)
        minutePicker.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(wheelView: WheelView, adapter: ArrayWheelAdapter<*>, position: Int) {
                minute = position
            }
        })

        content.findViewById<View>(R.id.cancel).setOnClickListener { dismiss() }
        content.findViewById<View>(R.id.confirm).setOnClickListener {
            val date = LocalDate.of(selected.year, selected.month, selected.day)
            onConfirm(date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            dismiss()
        }

        val density = context.resources.displayMetrics.density
        val shadowPadding = (6 * density).roundToInt()
        val container = FrameLayout(context).apply {
            setPadding(shadowPadding, shadowPadding, shadowPadding, shadowPadding)
            clipChildren = false
            clipToPadding = false
        }
        container.addView(content, FrameLayout.LayoutParams(
            dialogWidth(context), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER,
        ))
        setContentView(container)
        content.elevation = 4 * density
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.6f)
            setGravity(Gravity.CENTER)
            setLayout(dialogWidth(context) + shadowPadding * 2, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun show() {
        super.show()
        window?.let { window ->
            val shadowPadding = (6 * context.resources.displayMetrics.density).roundToInt()
            window.setLayout(dialogWidth(context) + shadowPadding * 2, WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply { y = -1 }
        }
    }

    private fun dialogWidth(context: Context): Int =
        context.resources.displayMetrics.widthPixels - (35.4 * context.resources.displayMetrics.density).roundToInt()

    private fun updateHeader(calendar: Calendar) {
        monthDayText.text = "${calendar.month}月"
        yearText.text = calendar.year.toString()
        lunarText.text = ""
    }
}
