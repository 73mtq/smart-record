package com.nonen.Bookkeeping.ui.components

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.view.ContextThemeWrapper
import com.nonen.Bookkeeping.R
import kotlin.math.roundToInt
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YimuTextView(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    bold: Boolean = false,
    viewId: Int = View.NO_ID,
    endPadding: Int = 0,
    includeFontPadding: Boolean = true,
    sizeUnit: Int = TypedValue.COMPLEX_UNIT_SP,
) {
    AndroidView(modifier = modifier, factory = { context -> TextView(ContextThemeWrapper(context, R.style.YimuReferenceTextTheme)).apply {
        setSingleLine(true)
        ellipsize = android.text.TextUtils.TruncateAt.END
    } },
        update = { view ->
            view.id = viewId
            view.text = text
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                TypedValue.applyDimension(sizeUnit, size, view.resources.displayMetrics).roundToInt().toFloat())
            view.setTextColor(color.toArgb())
            view.setTypeface(view.typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
            view.includeFontPadding = includeFontPadding
            view.setPaddingRelative(0, 0, (endPadding * view.resources.displayMetrics.density + 0.5f).toInt(), 0)
        })
}
