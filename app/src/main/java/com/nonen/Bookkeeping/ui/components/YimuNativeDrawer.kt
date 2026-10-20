package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import java.util.Locale

fun bindNativeTemplateDrawer(
    root: View,
    templates: List<ModuleBillEntity>,
    categories: List<CategoryEntity>,
    onApply: (ModuleBillEntity) -> Unit,
) {
    val tip = root.findViewById<View>(R.id.tip_layout)
    val list = root.findViewById<LinearLayout>(R.id.module_bill_list)
    root.findViewById<ImageView>(R.id.drawer_tip_pen).imageTintList =
        ColorStateList.valueOf(root.context.getColor(R.color.colorPrimary))
    tip.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
    list.removeAllViews()
    if (templates.isEmpty()) return
    val categoryMap = categories.associateBy { it.categoryId }
    val icons = CategoryIconCatalog(root.context)
    val bitmaps = LruCache<String, Bitmap>(32)
    val inflater = LayoutInflater.from(root.context)
    templates.forEach { template ->
        val item = inflater.inflate(R.layout.item_module_bill_drawer, list, false)
        bindTemplateItem(item, template, categoryMap, icons, bitmaps)
        item.setOnClickListener { onApply(template) }
        list.addView(item)
    }
}

private fun bindTemplateItem(
    item: View,
    template: ModuleBillEntity,
    categories: Map<Int, CategoryEntity>,
    icons: CategoryIconCatalog,
    bitmaps: LruCache<String, Bitmap>,
) {
    val context = item.context
    val parent = categories[template.parentCategoryId]
    val child = template.childCategoryId?.let { categories[it] }
    val income = parent?.categoryType == 1 || parent?.categoryId == 9
    val names = if (parent == null) {
        template.templateName to null
    } else {
        icons.names(parent.categoryName, child?.categoryName, income)
    }
    item.findViewById<TextView>(R.id.name).apply {
        text = if (names.second.isNullOrBlank()) names.first else names.first + "-" + names.second
        setTextColor(context.getColor(R.color.black))
    }
    item.findViewById<TextView>(R.id.asset).apply {
        text = context.getString(if (template.assetId <= 0L) R.string.yimu_drawer_no_asset else R.string.yimu_drawer_asset)
        setTextColor(context.getColor(R.color.grey))
    }
    item.findViewById<TextView>(R.id.reimbursement).apply {
        text = context.getString(R.string.yimu_drawer_no_reimburse)
        setTextColor(context.getColor(R.color.grey))
    }
    item.findViewById<TextView>(R.id.remark).apply {
        val remark = template.remark.orEmpty()
        text = remark
        visibility = if (remark.isBlank()) View.GONE else View.VISIBLE
        setTextColor(context.getColor(R.color.darkGrey))
    }
    item.findViewById<View>(R.id.remark_layout).visibility =
        if (template.remark.isNullOrBlank()) View.GONE else View.VISIBLE
    item.findViewById<TextView>(R.id.cost).apply {
        text = String.format(Locale.US, "%,.2f", if (income) template.cost else -template.cost)
        setTextColor(context.getColor(if (income) R.color.moneyIncome else R.color.moneyPay))
    }
    bindTemplateIcon(item, template, parent, child, income, icons, bitmaps, context)
}

private fun bindTemplateIcon(
    item: View,
    template: ModuleBillEntity,
    parent: CategoryEntity?,
    child: CategoryEntity?,
    income: Boolean,
    icons: CategoryIconCatalog,
    bitmaps: LruCache<String, Bitmap>,
    context: Context,
) {
    val iconView = item.findViewById<ImageView>(R.id.icon)
    if (parent == null) {
        iconView.setImageResource(R.mipmap.ic_parent_other)
        return
    }
    val iconName = icons.get(parent.categoryName, child?.categoryName, income, parent.iconUrl)
    when {
        iconName.startsWith("font:") -> {
            iconView.setImageResource(R.mipmap.ic_parent_other)
            iconView.imageTintList = ColorStateList.valueOf(context.getColor(R.color.black))
        }
        iconName.startsWith("lineIcon/") -> {
            val bitmap = bitmaps.get(iconName) ?: context.assets.open("yimu/$iconName").use {
                requireNotNull(BitmapFactory.decodeStream(it)).also { loaded -> bitmaps.put(iconName, loaded) }
            }
            iconView.setImageBitmap(bitmap)
            iconView.imageTintList = ColorStateList.valueOf(context.getColor(R.color.black))
        }
        else -> {
            val resource = context.resources.getIdentifier(iconName, "mipmap", context.packageName)
            require(resource != 0) { "缺少分类图标：$iconName" }
            iconView.setImageResource(resource)
            iconView.imageTintList = ColorStateList.valueOf(context.getColor(R.color.black))
        }
    }
}
