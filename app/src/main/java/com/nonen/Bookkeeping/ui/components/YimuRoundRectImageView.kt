package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/** 原版 com.wangc.bill.view.RoundRectCornerImageView：图片按 5dp 圆角裁剪。 */
class YimuRoundRectImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val clipPath = Path()
    private val clipRect = RectF()
    private val radiusPx = 5f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(clipRect, radiusPx, radiusPx, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        canvas.restore()
    }
}
