package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity

/** 规则行：原版 item_ai_type.xml 的类别行 + 关键词标签。 */
data class AiTypeRow(
    val category: String,
    val isIncome: Boolean,
    val keywords: List<String>,
)

/**
 * 按类别聚合自定义关键词规则。
 * 原版 CategoryKeyManagerActivity.a0() 读取 AiType 表（仅自定义智能分类），
 * content 字段以空格分隔多个关键词，行内依次展示类别与关键词。
 */
internal fun aiTypeRows(rules: List<CategoryRuleEntity>): List<AiTypeRow> =
    rules.asSequence()
        .filter { it.isCustom }
        .groupBy { it.category }
        .map { (category, entries) ->
            AiTypeRow(
                category = category,
                isIncome = category in Categories.incomeCategories,
                keywords = entries.map { it.keyword },
            )
        }

/** 自定义智能分类页：原版 CategoryKeyManagerActivity，布局 activity_self_ai_type.xml。 */
class YimuAiTypeNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onAdd()
    }

    private val toolBar: View
    private val rightText: TextView
    private val addButton: View
    private val tipLayout: View
    private val aiTypeList: RecyclerView
    private val adapter = AiTypeAdapter(context)
    private var listener: Listener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_ai_type_edit, this, true)
        toolBar = findViewById(R.id.tool_bar)
        rightText = findViewById(R.id.right_text)
        addButton = findViewById(R.id.add_ai_self_type)
        tipLayout = findViewById(R.id.tip_layout)
        aiTypeList = findViewById(R.id.ai_type_list)
        aiTypeList.layoutManager = LinearLayoutManager(context)
        aiTypeList.adapter = adapter
        findViewById<View>(R.id.btn_back).setOnClickListener { listener?.onBack() }
        rightText.setOnClickListener { listener?.onAdd() }
        addButton.setOnClickListener { listener?.onAdd() }
    }

    /** 原版该页由 BaseToolBarActivity 承载，tool_bar 高度含状态栏 inset，内容 bottom 锚定。 */
    fun applyToolbarInset(statusBarPx: Int) {
        val contentHeight = Math.round(50 * resources.displayMetrics.density)
        if (statusBarPx > 0 && toolBar.layoutParams.height != contentHeight + statusBarPx) {
            toolBar.layoutParams.height = contentHeight + statusBarPx
            toolBar.requestLayout()
        }
    }

    fun bind(listener: Listener, rules: List<CategoryRuleEntity>) {
        this.listener = listener
        val rows = aiTypeRows(rules)
        adapter.submit(rows)
        val empty = rows.isEmpty()
        tipLayout.visibility = if (empty) View.VISIBLE else View.GONE
        aiTypeList.visibility = if (empty) View.GONE else View.VISIBLE
        // 原版 CategoryKeyManagerActivity.Z():62-72 列表非空时显示工具栏右侧“新增”，空态使用“开始添加”
        rightText.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private class AiTypeAdapter(private val context: Context) : RecyclerView.Adapter<AiTypeAdapter.Holder>() {

        private val catalog = CategoryIconCatalog(context)
        private var rows: List<AiTypeRow> = emptyList()

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.type_icon)
            val type: TextView = view.findViewById(R.id.type)
            val keywords: YimuKeywordFlowLayout = view.findViewById(R.id.text_list)
        }

        fun submit(rows: List<AiTypeRow>) {
            this.rows = rows
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_ai_type, parent, false))

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = rows[position]
            holder.type.text = row.category
            applyIcon(holder.icon, row)
            bindKeywords(holder, row.keywords)
        }

        private fun applyIcon(target: ImageView, row: AiTypeRow) {
            // 规则行保存“一级-二级”组合名，图标按一级/二级拆分查询（原版取 ParentCategory/ChildCategory.iconUrl）
            val parent = row.category.substringBefore("-").trim().ifBlank { row.category }
            val child = row.category.substringAfter("-", "").trim().ifBlank { null }
            val fallback = CategoryMapping.resolve(parent, child, row.isIncome).iconUrl
            val name = catalog.get(parent, child, row.isIncome, fallback)
            val tint = ContextCompat.getColor(context, R.color.black)
            when {
                name.startsWith("lineIcon/") -> {
                    val bitmap = target.context.assets.open("yimu/$name").use { BitmapFactory.decodeStream(it) }
                    target.setImageBitmap(bitmap)
                    target.imageTintList = ColorStateList.valueOf(tint)
                }
                name.startsWith("font:") -> target.setImageDrawable(null)
                else -> {
                    val id = target.context.resources.getIdentifier(name, "mipmap", target.context.packageName)
                    if (id != 0) {
                        target.setImageResource(id)
                        target.imageTintList = ColorStateList.valueOf(tint)
                    }
                }
            }
        }

        private fun bindKeywords(holder: Holder, keywords: List<String>) {
            holder.keywords.removeAllViews()
            val density = holder.itemView.resources.displayMetrics.density
            fun dp(value: Int): Int = Math.round(value * density)
            keywords.forEach { keyword ->
                val chip = TextView(holder.itemView.context).apply {
                    text = keyword
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                    setTextColor(ContextCompat.getColor(context, R.color.textColorBlack))
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    maxWidth = dp(100)
                    setPadding(dp(6), 0, dp(6), 0)
                }
                holder.keywords.addView(
                    chip,
                    ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)).apply {
                        leftMargin = dp(4)
                        rightMargin = dp(4)
                        topMargin = dp(5)
                    },
                )
            }
        }
    }
}

/** 关键词横排换行容器，对应原版 item_ai_type 用 FlexboxLayoutManager 承载的 text_list。 */
class YimuKeywordFlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            if (lineWidth > 0 && lineWidth + childWidth > available) {
                totalHeight += lineHeight
                lineWidth = 0
                lineHeight = 0
            }
            lineWidth += childWidth
            lineHeight = maxOf(lineHeight, child.measuredHeight + lp.topMargin + lp.bottomMargin)
        }
        totalHeight += lineHeight
        setMeasuredDimension(
            resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec),
            resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val available = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        var lineLeft = paddingLeft
        var lineTop = paddingTop
        var lineHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            if (lineLeft > paddingLeft && lineLeft - paddingLeft + childWidth > available) {
                lineLeft = paddingLeft
                lineTop += lineHeight
                lineHeight = 0
            }
            val childLeft = lineLeft + lp.leftMargin
            val childTop = lineTop + lp.topMargin
            child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
            lineLeft += childWidth
            lineHeight = maxOf(lineHeight, child.measuredHeight + lp.topMargin + lp.bottomMargin)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(p: LayoutParams): LayoutParams = MarginLayoutParams(p)

    override fun checkLayoutParams(p: LayoutParams): Boolean = p is MarginLayoutParams
}
