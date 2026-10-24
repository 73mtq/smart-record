package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.nonen.Bookkeeping.R

/** 分类关键词页：原版 activity_self_ai_type.xml 的空态与列表两种状态。 */
class YimuCategoryKeyNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onAdd()
    }

    private val back: View
    private val addButton: View
    private val toolBar: View

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_category_key, this, true)
        back = findViewById(R.id.btn_back)
        addButton = findViewById(R.id.add_ai_self_type)
        toolBar = findViewById(R.id.tool_bar)
    }

    /**
     * 原版该页是 edge-to-edge 容器内的 Fragment，tool_bar 实测 bounds [0,0][1200,303]：
     * 高度含状态栏 inset，内容 bottom 锚定（activity_self_ai_type.xml 无工具栏，来自外层容器）。
     * AndroidView 内收不到 window insets 派发，由 Compose 侧传入状态栏像素高度。
     */
    fun applyToolbarInset(statusBarPx: Int) {
        // 原版 XML 50.0dp 在 3.25 密度下为 162.5px，框架取整为 163；用截断会得 162 使内容整体上移 1px
        val contentHeight = Math.round(50 * resources.displayMetrics.density)
        if (statusBarPx > 0 && toolBar.layoutParams.height != contentHeight + statusBarPx) {
            toolBar.layoutParams.height = contentHeight + statusBarPx
            toolBar.requestLayout()
        }
    }

    fun bind(listener: Listener, hasRules: Boolean) {
        back.setOnClickListener { listener.onBack() }
        addButton.setOnClickListener { listener.onAdd() }
        findViewById<View>(R.id.tip_layout).visibility = if (hasRules) View.GONE else View.VISIBLE
        findViewById<View>(R.id.ai_type_list).visibility = if (hasRules) View.VISIBLE else View.GONE
    }
}
