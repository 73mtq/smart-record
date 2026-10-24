package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.nonen.Bookkeeping.R

/**
 * 原版 activity_bill_search.xml 的原生宿主（历史态）。
 * 原版历史存 LitePal search_history 表（order id desc），此处等价存 SharedPreferences，
 * 采集前由测试注入与参考一致的条目顺序。
 */
class YimuSearchNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onFilter()
    }

    val searchInput: AppCompatEditText
    private val historyList: LinearLayout
    private val clearHistory: TextView
    private val prefs: SharedPreferences

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_search, this, true)
        orientation = VERTICAL
        searchInput = findViewById(R.id.search_input)
        historyList = findViewById(R.id.search_history_list)
        clearHistory = findViewById(R.id.clear_history_btn)
        prefs = context.getSharedPreferences("yimu_search_history", Context.MODE_PRIVATE)
    }

    fun bind(listener: Listener) {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { listener.onBack() }
        findViewById<TextView>(R.id.btn_filter).setOnClickListener { listener.onFilter() }
        clearHistory.setOnClickListener {
            prefs.edit().clear().apply()
            renderHistory(emptyList())
        }
        renderHistory(loadHistory())
        searchInput.requestFocus()
    }

    private fun loadHistory(): List<String> =
        prefs.getString("items", null)?.split("\u0001")?.filter { it.isNotEmpty() } ?: emptyList()

    fun storeHistory(items: List<String>) {
        prefs.edit().putString("items", items.joinToString("\u0001")).apply()
        renderHistory(items)
    }

    private fun renderHistory(items: List<String>) {
        historyList.removeAllViews()
        items.forEach { key ->
            // 与原版同路径：LayoutInflater 解析 item_search_history.xml（AppCompat 转换 + 主题属性）
            val row = LayoutInflater.from(context).inflate(R.layout.item_search_history, historyList, false)
            val text = row.findViewById<TextView>(R.id.text)
            text.text = key
            text.setOnClickListener {
                val items2 = loadHistory().toMutableList()
                items2.remove(key)
                storeHistory(items2)
            }
            row.findViewById<ImageView>(R.id.clear).setOnClickListener {
                val items2 = loadHistory().toMutableList()
                items2.remove(key)
                storeHistory(items2)
            }
            historyList.addView(row)
        }
    }
}
