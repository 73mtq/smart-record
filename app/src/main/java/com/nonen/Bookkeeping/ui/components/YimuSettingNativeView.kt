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

        findViewById<View>(R.id.bill_num_color).setOnClickListener { listener.onAction("bill_num_color") }
        buildCommonGroup(listener)
        buildPreferenceGroup(listener)
        buildApplicationGroup(listener)
    }

    /** 原版 activity_setting.xml：通用设置（图标设置 / 字体大小 / 导航栏文字） */
    private fun buildCommonGroup(listener: Listener) {
        val group = findViewById<LinearLayout>(R.id.group_common)
        arrowRow(group, "icon_setting", "图标设置", "为转账、还款、借入借出等设置自定义图标", null, listener)
        arrowRow(group, "font_size", "字体大小", "设置软件内显示的字体大小", "标准", listener)
        switchRowIn(group, "navigation_labels", "导航栏文字", "首页底部导航栏显示文字信息", listener)
    }

    /** 原版 activity_setting.xml：偏好设置（悬浮球、月起始日、默认账户、各类提示与模块入口） */
    private fun buildPreferenceGroup(listener: Listener) {
        val group = findViewById<LinearLayout>(R.id.group_pref)
        arrowRow(group, "quick_option", "快速操作", "悬浮球、快速记账、快速导入等设置", null, listener)
        arrowRow(group, "month_start", "账单月起始日", "月账单的起始日期，默认为每月1号", "1号", listener)
        arrowRow(group, "calendar_week_start", "日历周起始日", "设置日历视图中每周的起始日期", "周日", listener)
        arrowRow(group, "default_asset", "默认账户", "设置新增账单时默认选择的账户", "自动选择", listener)
        switchRowIn(group, "switch_home_budget", "首页预算显示", "在应用首页显示预算模块", listener)
        switchRowIn(group, "switch_vibrate", "振动反馈", "数字键盘、快捷操作振动反馈", listener)
        switchRowIn(group, "switch_hide_add_bill", "隐藏手动记账按钮", "打开后，“添加一条新记账”按钮将在右上角", listener)
        switchRowIn(group, "switch_smart_tip", "智能提示", "智能记账时显示历史识别记录", listener)
        switchRowIn(group, "switch_history_note", "历史备注提示", "记账时输入备注将实时匹配历史备注", listener)
        switchRowIn(group, "switch_asset_warning", "账户余额不足提醒", "当账户可用余额小于0时进行提醒", listener)
        switchRowIn(group, "switch_calendar_repay", "日历还款信息", "在日历视图中显示信贷账户的待还账单", listener)
        switchRowIn(group, "switch_calendar_lunar", "日历农历信息", "在日历视图中显示农历信息", listener)
        arrowRow(group, "home_pull_type", "首页下拉快捷方式", "打开云同步则同步数据，否则添加账单", null, listener)
        arrowRow(group, "function_module", "功能模块选择", "设置首页功能模块的显示/隐藏、排序", null, listener)
        arrowRow(group, "number_keyboard", "数字键盘排列", "设置记账时数字键盘排列的顺序", null, listener)
        arrowRow(group, "float_ball_position", "悬浮按钮设置", "设置首页智能记账悬浮按钮所处的位置", "右侧", listener)
        arrowRow(group, "float_ball_action", "悬浮按钮功能", "设置首页悬浮按钮点击功能", "智能记账", listener)
        arrowRow(group, "tile_type", "系统下拉快捷方式", "设置系统下拉快捷方式功能", "智能记账", listener)
    }

    /** 原版 activity_setting.xml：应用设置（退出提示、后台模式、权限管理） */
    private fun buildApplicationGroup(listener: Listener) {
        val group = findViewById<LinearLayout>(R.id.group_app)
        switchRowIn(group, "switch_exit_tip", "退出时提示", "点击返回键退出应用时，需要再次点击确认", listener)
        switchRowIn(group, "switch_background_pure", "后台纯净模式", "按返回退出首页后将在后台隐藏", listener)
        switchRowIn(group, "switch_background_blur", "后台应用模糊", "在后台管理页中，模糊处理应用卡片", listener)
        arrowRow(group, "system_permission", "系统权限管理", "更改应用的权限范围", null, listener)
    }

    private fun arrowRow(
        container: LinearLayout,
        key: String,
        title: String,
        subtitle: String,
        info: String?,
        listener: Listener,
    ) {
        val layout = if (info == null) R.layout.yimu_setting_row_arrow else R.layout.yimu_setting_row_info
        val row = LayoutInflater.from(context).inflate(layout, container, true)
        row.findViewById<TextView>(R.id.row_title).text = title
        row.findViewById<TextView>(R.id.row_subtitle).text = subtitle
        if (info != null) row.findViewById<TextView>(R.id.row_info).text = info
        row.setOnClickListener { listener.onAction(key) }
    }

    private fun switchRowIn(container: LinearLayout, key: String, title: String, subtitle: String, listener: Listener) {
        LayoutInflater.from(context).inflate(R.layout.yimu_setting_row_switch, container, true)
        container.getChildAt(container.childCount - 1).let { row ->
            row.findViewById<TextView>(R.id.row_title).text = title
            row.findViewById<TextView>(R.id.row_subtitle).text = subtitle
            val sw = RowSwitchView(context)
            sw.setChecked(listener.switchState(key))
            sw.setOnClickListener { v ->
                val toggle = v as RowSwitchView
                toggle.toggle()
                listener.onSwitchChanged(key, toggle.isChecked)
            }
            row.findViewById<android.widget.FrameLayout>(R.id.row_switch).addView(sw, android.widget.FrameLayout.LayoutParams(-2, -2))
            switches[key] = sw
        }
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
