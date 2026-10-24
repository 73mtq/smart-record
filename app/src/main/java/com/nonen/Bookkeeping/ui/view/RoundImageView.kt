package com.nonen.Bookkeeping.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/** 原版 RoundImage.RoundedImageView 的等效圆形裁剪（oval=true）。 */
class RoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        path.addCircle(w / 2f, h / 2f, minOf(w, h) / 2f, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        if (width > 0 && height > 0) {
            val save = canvas.save()
            canvas.clipPath(path)
            super.draw(canvas)
            canvas.restoreToCount(save)
        } else {
            super.draw(canvas)
        }
    }
}
