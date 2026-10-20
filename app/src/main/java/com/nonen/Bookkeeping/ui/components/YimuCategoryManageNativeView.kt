package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.CategoryEntity

/** 分类管理页：原版 activity_type_edit.xml 的两级分类树，展开态内嵌子分类网格。 */
class YimuCategoryManageNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onTypeChanged(income: Boolean)
        fun onAdd(parentId: Int)
        fun onToggleExpand(categoryId: Int)
        fun onEdit(category: CategoryEntity)
        fun onDelete(category: CategoryEntity)
        fun onUnsupported()
    }

    private val back: ImageView
    private val payBtn: TextView
    private val incomeBtn: TextView
    private val shareCenter: TextView
    private val rightIcon: ImageView
    private val addButton: LinearLayout
    private val payList: RecyclerView
    private val incomeList: RecyclerView
    private var listener: Listener? = null
    private var selectedType = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_category_manage, this, true)
        back = findViewById(R.id.btn_back)
        payBtn = findViewById(R.id.pay_btn)
        incomeBtn = findViewById(R.id.income_btn)
        shareCenter = findViewById(R.id.share_center)
        rightIcon = findViewById(R.id.right_icon)
        addButton = findViewById(R.id.add_btn)
        payList = findViewById(R.id.pay_type_list)
        incomeList = findViewById(R.id.income_type_list)
        listOf(payList, incomeList).forEach {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = CategoryAdapter(context)
        }
        back.setOnClickListener { listener?.onBack() }
        payBtn.setOnClickListener { listener?.onTypeChanged(false) }
        incomeBtn.setOnClickListener { listener?.onTypeChanged(true) }
        shareCenter.setOnClickListener { listener?.onUnsupported() }
        rightIcon.setOnClickListener { listener?.onUnsupported() }
        addButton.setOnClickListener { listener?.onAdd(0) }
    }

    fun bind(
        listener: Listener,
        type: Int,
        categories: List<CategoryEntity>,
        expandedParentIds: Set<Int>,
    ) {
        this.listener = listener
        findViewById<View>(R.id.text_tip_layout)?.visibility = View.VISIBLE
        findViewById<View>(R.id.category_type_tip)?.visibility = View.VISIBLE
        if (selectedType != type) {
            selectedType = type
            payBtn.setTextColor(ContextCompat.getColor(context, if (type == 0) R.color.black else R.color.grey))
            payBtn.textSize = if (type == 0) 16f else 13f
            incomeBtn.setTextColor(ContextCompat.getColor(context, if (type == 1) R.color.black else R.color.grey))
            incomeBtn.textSize = if (type == 1) 16f else 13f
            payList.visibility = if (type == 0) View.VISIBLE else View.GONE
            incomeList.visibility = if (type == 1) View.VISIBLE else View.GONE
        }
        val target = if (type == 0) payList else incomeList
        (target.adapter as CategoryAdapter).submit(listener, type == 1, categories, expandedParentIds)
    }

    private class CategoryAdapter(private val context: Context) : RecyclerView.Adapter<CategoryAdapter.Holder>() {

        private var listener: Listener? = null
        private var isIncome = false
        private var rows: List<Row> = emptyList()

        class Row(val parent: CategoryEntity, val children: List<CategoryEntity>, val expanded: Boolean)

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val container: View = view.findViewById(R.id.total_layout)
            val parentLayout: View = view.findViewById(R.id.parent_layout)
            val arrow: ImageView = view.findViewById(R.id.arrow)
            val icon: ImageView = view.findViewById(R.id.icon)
            val name: TextView = view.findViewById(R.id.type_name)
            val edit: ImageView = view.findViewById(R.id.edit)
            val childList: RecyclerView = view.findViewById(R.id.child_type)
        }

        fun submit(
            listener: Listener,
            isIncome: Boolean,
            categories: List<CategoryEntity>,
            expandedParentIds: Set<Int>,
        ) {
            this.listener = listener
            this.isIncome = isIncome
            val parents = categories.filter { it.parentId == 0 }
            rows = parents.map { parent ->
                Row(parent, categories.filter { it.parentId == parent.categoryId }, parent.categoryId in expandedParentIds)
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(context).inflate(R.layout.item_yimu_edit_type_parent, parent, false))

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = rows[position]
            val current = listener ?: return
            holder.arrow.rotation = if (row.expanded) 90f else 0f
            holder.arrow.setImageResource(R.mipmap.ic_right)
            holder.name.text = row.parent.categoryName
            applyIcon(holder.icon, row.parent)
            holder.parentLayout.setOnClickListener { current.onToggleExpand(row.parent.categoryId) }
            holder.edit.setOnClickListener { current.onEdit(row.parent) }
            holder.childList.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 4)
            holder.childList.adapter = ChildAdapter(context, current, isIncome, row.children)
            holder.childList.visibility = if (row.expanded) View.VISIBLE else View.GONE
        }

        private fun applyIcon(target: ImageView, category: CategoryEntity) {
            val catalog = CategoryIconCatalog(context)
            val name = catalog.get(category.categoryName, null, isIncome, category.iconUrl)
            val tint = ContextCompat.getColor(context, R.color.black)
            when {
                name.startsWith("lineIcon/") -> {
                    val bitmap = context.assets.open("yimu/$name").use { BitmapFactory.decodeStream(it) }
                    target.setImageBitmap(bitmap)
                    target.imageTintList = android.content.res.ColorStateList.valueOf(tint)
                }
                name.startsWith("font:") -> target.setImageDrawable(null)
                else -> {
                    val id = context.resources.getIdentifier(name, "mipmap", context.packageName)
                    if (id != 0) {
                        target.setImageResource(id)
                        target.imageTintList = android.content.res.ColorStateList.valueOf(tint)
                    }
                }
            }
        }
    }

    private class ChildAdapter(
        private val context: Context,
        private val listener: Listener,
        private val isIncome: Boolean,
        private val children: List<CategoryEntity>,
    ) : RecyclerView.Adapter<ChildAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.icon)
            val name: TextView = view.findViewById(R.id.type_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(context).inflate(R.layout.item_yimu_edit_type_child, parent, false))

        override fun getItemCount(): Int = children.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val child = children[position]
            holder.name.text = child.categoryName
            val catalog = CategoryIconCatalog(context)
            val name = catalog.get(child.categoryName, null, isIncome, child.iconUrl)
            when {
                name.startsWith("lineIcon/") -> {
                    holder.icon.setImageBitmap(context.assets.open("yimu/$name").use { BitmapFactory.decodeStream(it) })
                    holder.icon.imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.black),
                    )
                }
                name.startsWith("font:") -> holder.icon.setImageDrawable(null)
                else -> {
                    val id = context.resources.getIdentifier(name, "mipmap", context.packageName)
                    if (id != 0) holder.icon.setImageResource(id)
                }
            }
            holder.itemView.setOnClickListener { listener.onEdit(child) }
        }
    }
}
