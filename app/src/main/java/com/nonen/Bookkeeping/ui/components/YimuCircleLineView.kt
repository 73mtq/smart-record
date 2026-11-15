package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 原版 com.wangc.bill.view.CircleLineView：
 * 5dp 圆头线段，从左侧 5dp 处开始，按百分比绘制到右侧对应位置。
 */
class YimuCircleLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val lineInset = 5f * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5f * density
        color = 0xFFFFFFFF.toInt()
    }
    private var progress = 0

    fun setProgress(value: Int) {
        val clamped = value.coerceIn(0, 100)
        if (progress == clamped) return
        progress = clamped
        invalidate()
    }

    fun setLineColor(color: Int) {
        if (paint.color == color) return
        paint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress == 0 || width <= lineInset * 2) return
        val endX = (width - lineInset * 2) * progress / 100f + lineInset
        canvas.drawLine(lineInset, height / 2f, endX, height / 2f, paint)
    }
}
