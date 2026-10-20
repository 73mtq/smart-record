package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.nonen.Bookkeeping.R

class YimuDreamNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onAdd()
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_dream_list, this, true)
        findViewById<View>(R.id.text_tip_layout)?.visibility = View.GONE
        findViewById<View>(R.id.btn_setting)?.visibility = View.GONE
        findViewById<View>(R.id.btn_add)?.visibility = View.GONE
        findViewById<View>(R.id.tip_layout)?.visibility = View.VISIBLE
    }

    fun bind(listener: Listener) {
        findViewById<View>(R.id.btn_back)?.setOnClickListener { listener.onBack() }
        findViewById<View>(R.id.btn_add)?.setOnClickListener { listener.onAdd() }
        findViewById<View>(R.id.add_dream)?.setOnClickListener { listener.onAdd() }
        findViewById<View>(R.id.btn_setting)?.setOnClickListener { }
    }
}
