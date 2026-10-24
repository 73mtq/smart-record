package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.kyleduo.switchbutton.SwitchButton
import com.nonen.Bookkeeping.R

/**
 * 原版 activity_setting.xml 的原生宿主（ROOT 页）。
 * 行模板 yimu_setting_row_switch.xml 经 LayoutInflater 解析（与原版同为 AppCompat 转换路径，
 * 保证 12/15sp 文字栅格化与原版一致——程序化 TextView 有亚像素差，见 groupE-known-differences）。
 */
class YimuSettingNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {

    interface Listener {
        fun onBack()
        fun onSwitchChanged(key: String, checked: Boolean)
        fun onAction(key: String)
        fun switchState(key: String): Boolean
    }

    private val switches = mutableMapOf<String, RowSwitchView>()
    private var bound = false

    private val toolBar: View

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_settings, this, true)
        toolBar = findViewById(R.id.tool_bar)
    }

    /** 原版设置页 tool_bar 实测 [0,0][1200,303]：50dp + 状态栏 inset（同 rules 页） */
    fun applyToolbarInset(statusBarPx: Int) {
        val contentHeight = Math.round(50 * resources.displayMetrics.density)
        if (statusBarPx > 0 && toolBar.layoutParams.height != contentHeight + statusBarPx) {
            toolBar.layoutParams.height = contentHeight + statusBarPx
            toolBar.requestLayout()
        }
    }

    fun bind(listener: Listener) {
        if (bound) {
            switches.forEach { (key, sw) -> sw.setChecked(listener.switchState(key)) }
            return
        }
        bound = true
        findViewById<View>(R.id.btn_back).setOnClickListener { listener.onBack() }
        holderRow("row_switch_lock", "switch_lock", listener)
        holderRow("row_switch_gesture", "switch_gesture", listener)
        switchRow("row_currency", "switch_currency", "多币种", "为账单、账户设置币种", listener)
        switchRow("row_remark_main", "switch_remark_main", "备注优先显示", "将备注作为标题，分类作为描述", listener)
        switchRow("row_category_simple", "switch_category_simple", "分类精简", "拥有二级分类的账单将不显示一级分类", listener)
        switchRow("row_show_time", "switch_show_time", "显示账单时间", "在账单流水中显示账单的具体时间", listener)
        switchRow("row_show_old_cost", "switch_show_old_cost", "显示原金额", "退款、优惠账单显示原金额", listener)
        switchRow("row_show_asset_icon", "switch_show_asset_icon", "显示账户图标", "在流水中显示账户图标", listener)
        switchRow("row_location", "switch_location", "自动定位", "在记账时，会自动记录记账所在地点", listener)
        switchRow("row_navigation_labels", "navigation_labels", "导航文字", "显示底部导航文字", listener)

        findViewById<View>(R.id.bill_num_color).setOnClickListener { listener.onAction("bill_num_color") }
        findViewById<View>(R.id.appearance).setOnClickListener { listener.onAction("appearance") }
        findViewById<View>(R.id.backup).setOnClickListener { listener.onAction("backup") }
        findViewById<View>(R.id.import_export).setOnClickListener { listener.onAction("import_export") }
    }

    /** 返回后同步外部状态（ViewModel 驱动） */
    fun updateSwitch(key: String, checked: Boolean) {
        switches[key]?.setChecked(checked)
    }

    /** 安全锁组两行为静态 XML 占位（无副标题结构），只注入开关 */
    private fun holderRow(holderId: String, key: String, listener: Listener) {
        val holder = findViewById<android.widget.FrameLayout>(resources.getIdentifier(holderId, "id", context.packageName))
        val sw = RowSwitchView(context)
        sw.setChecked(listener.switchState(key))
        sw.setOnClickListener { v ->
            val row = v as RowSwitchView
            row.toggle()
            listener.onSwitchChanged(key, row.isChecked)
        }
        holder.addView(sw, android.widget.FrameLayout.LayoutParams(-2, -2))
        switches[key] = sw
    }

    private fun switchRow(containerId: String, key: String, title: String, subtitle: String, listener: Listener) {
        val container = findViewById<LinearLayout>(resources.getIdentifier(containerId, "id", context.packageName))
        LayoutInflater.from(context).inflate(R.layout.yimu_setting_row_switch, container, true)
        container.findViewById<TextView>(R.id.row_title).text = title
        container.findViewById<TextView>(R.id.row_subtitle).text = subtitle
        val sw = RowSwitchView(context)
        sw.setChecked(listener.switchState(key))
        sw.setOnClickListener { v ->
            val row = v as RowSwitchView
            row.toggle()
            listener.onSwitchChanged(switches.entries.firstOrNull { it.value === row }?.key ?: "", row.isChecked)
        }
        val holder = container.findViewById<android.widget.FrameLayout>(R.id.row_switch)
        holder.addView(sw, android.widget.FrameLayout.LayoutParams(-2, -2))
        switches[key] = sw
    }


}

/** 参考实测几何（库默认）：back 40×24dp 圆角 12dp，thumb 20dp 圆 + 2dp margin；thumb 未选中 #CAC2C2/选中 #48AB93 */
class RowSwitchView(context: Context) : View(context) {
    private var state = false

    private val backPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    val isChecked: Boolean get() = state

    fun setChecked(value: Boolean) {
        state = value
        contentDescription = if (value) "开" else "关"
        invalidate()
    }

    fun toggle() {
        state = !state
        contentDescription = if (state) "开" else "关"
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(Math.round(40 * density), Math.round(24 * density))
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        backPaint.color = if (state) 0xFFDDF0EB.toInt() else 0xFFDFDFDF.toInt()
        canvas.drawRoundRect(0f, 0f, width.toFloat(), h, r, r, backPaint)
        val d = Math.round(2 * density)
        val t = Math.round(20 * density).toFloat()
        thumbPaint.color = if (state) 0xFF48AB93.toInt() else 0xFFCAC2C2.toInt()
        val left = if (state) width - d - t else d.toFloat()
        canvas.drawCircle(left + t / 2f, h / 2f, t / 2f, thumbPaint)
    }
}
