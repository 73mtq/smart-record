package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.nonen.Bookkeeping.R

class YimuPeriodicNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onAdd()
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_cycle_manager, this, true)
        findViewById<View>(R.id.no_data_layout)?.visibility = View.VISIBLE
        findViewById<View>(R.id.cycle_list)?.visibility = View.GONE
        findViewById<TextView>(R.id.title)?.text = "周期记账"
    }

    fun bind(listener: Listener) {
        findViewById<View>(R.id.btn_back)?.setOnClickListener { listener.onBack() }
        findViewById<View>(R.id.add)?.setOnClickListener { listener.onAdd() }
        findViewById<View>(R.id.ic_more)?.setOnClickListener { }
    }
}
