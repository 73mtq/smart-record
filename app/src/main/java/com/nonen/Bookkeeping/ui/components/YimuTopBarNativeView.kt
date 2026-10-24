package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.nonen.Bookkeeping.R

/** 二级页通用原生工具栏：title 用 XML TextView（与原版栅格化一致，Compose Text 有字形差）。 */
class YimuTopBarNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val toolBar: View
    private val titleView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_top_bar, this, true)
        toolBar = findViewById(R.id.tool_bar)
        titleView = findViewById(R.id.title)
        findViewById<View>(R.id.btn_back).setOnClickListener { onBack?.invoke() }
    }

    var onBack: (() -> Unit)? = null

    /** 原版 tool_bar 实测 [0,0][1200,303]：50dp + 状态栏 inset（同 rules/settings 页） */
    fun applyToolbarInset(statusBarPx: Int) {
        val contentHeight = Math.round(50 * resources.displayMetrics.density)
        if (statusBarPx > 0 && toolBar.layoutParams.height != contentHeight + statusBarPx) {
            toolBar.layoutParams.height = contentHeight + statusBarPx
            toolBar.requestLayout()
        }
    }

    fun setTitle(text: String) {
        titleView.text = text
    }

    /** 把页面内容挂到工具栏之下（同一原生容器，保证 elevation 阴影不被独立 View 边界裁掉） */
    fun setContent(view: View) {
        findViewById<FrameLayout>(R.id.content_layout).addView(view)
    }
}
