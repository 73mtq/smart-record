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

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_category_key, this, true)
        back = findViewById(R.id.btn_back)
        addButton = findViewById(R.id.add_ai_self_type)
    }

    fun bind(listener: Listener, hasRules: Boolean) {
        back.setOnClickListener { listener.onBack() }
        addButton.setOnClickListener { listener.onAdd() }
        findViewById<View>(R.id.tip_layout).visibility = if (hasRules) View.GONE else View.VISIBLE
        findViewById<View>(R.id.ai_type_list).visibility = if (hasRules) View.VISIBLE else View.GONE
    }
}
