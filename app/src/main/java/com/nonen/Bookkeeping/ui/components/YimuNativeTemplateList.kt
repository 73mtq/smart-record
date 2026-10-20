package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import java.util.Locale

/**
 * 模板列表绑定状态：复用原版 item_yimu_template_bill 的控件 ID，由 view_yimu_template_manager 提供容器。
 */
data class NativeTemplateState(
    val templates: List<ModuleBillEntity>,
    val categories: List<CategoryEntity>,
    val assets: List<AssetEntity>,
    val text: Int,
    val secondary: Int,
)

/**
 * 把模板列表绑定到模块管理页的 RecyclerView（id 为 module_bill_list）。
 * 每次状态变化都重新绑定；适配器相同且状态相同时直接返回，避免无意义刷新。
 */
fun bindNativeTemplateList(
    list: RecyclerView,
    state: NativeTemplateState,
    onApply: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onLockedApply: (Long) -> Unit,
) {
    val adapter = list.adapter as? NativeTemplateAdapter ?: NativeTemplateAdapter(list.context).also {
        list.adapter = it
    }
    adapter.update(state, onApply, onEdit, onLockedApply)
}

class NativeTemplateAdapter(private val context: Context) :
    RecyclerView.Adapter<NativeTemplateAdapter.Holder>() {
    private val inflater = LayoutInflater.from(context)
    private val icons = CategoryIconCatalog(context)
    private val bitmaps = LruCache<String, Bitmap>(32)
    private lateinit var state: NativeTemplateState
    private var categories: Map<Int, CategoryEntity> = emptyMap()
    private var assets: Map<Long, String> = emptyMap()
    private var onApply: (Long) -> Unit = {}
    private var onEdit: (Long) -> Unit = {}
    private var onLockedApply: (Long) -> Unit = {}

    fun update(
        next: NativeTemplateState,
        apply: (Long) -> Unit,
        edit: (Long) -> Unit,
        lockedApply: (Long) -> Unit,
    ) {
        onApply = apply
        onEdit = edit
        onLockedApply = lockedApply
        if (::state.isInitialized && state == next) return
        state = next
        categories = next.categories.associateBy { it.categoryId }
        assets = next.assets.associate { it.assetId to it.assetName }
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (::state.isInitialized) state.templates.size else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(inflater.inflate(R.layout.item_yimu_template_bill, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val template = state.templates[position]
        val view = holder.itemView
        val income = categories[template.parentCategoryId]?.categoryType == 1
        val transfer = template.moduleType == ModuleBillEntity.MODULE_TYPE_TRANSFER
        view.text(R.id.category).apply {
            text = if (transfer) transferTitle(template) else categoryTitle(template, income)
            setTextColor(state.text)
        }
        view.text(R.id.remark).apply {
            val remark = template.remark.orEmpty()
            visibility = if (remark.isBlank()) View.GONE else View.VISIBLE
            text = remark
            setTextColor(state.secondary)
        }
        view.text(R.id.cost).apply {
            text = String.format(Locale.US, "%,.2f", kotlin.math.abs(template.cost))
            setTextColor(state.text)
        }
        view.text(R.id.asset).apply {
            val account = if (transfer) {
                "${assets[template.fromAssetId] ?: DELETED}→${assets[template.toAssetId] ?: DELETED}"
            } else {
                assets[template.assetId].orEmpty()
            }
            visibility = if (account.isBlank()) View.GONE else View.VISIBLE
            text = account
            setTextColor(state.secondary)
        }
        view.text(R.id.lock_status).apply {
            val locked = template.moduleLock && template.cost > 0.0
            text = if (locked) LOCKED_TEXT else UNLOCKED_TEXT
            setTextColor(state.secondary)
        }
        bindIcon(view, template, transfer, income)
        view.findViewById<View>(R.id.bill_layout).apply {
            setOnClickListener {
                if (template.moduleLock && template.cost > 0.0) onLockedApply(template.moduleBillId)
                else onApply(template.moduleBillId)
            }
        }
        view.findViewById<View>(R.id.edit).setOnClickListener { onEdit(template.moduleBillId) }
    }

    private fun bindIcon(view: View, template: ModuleBillEntity, transfer: Boolean, income: Boolean) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val iconText = view.text(R.id.yimu_template_icon_text)
        val iconName = if (transfer) {
            TRANSFER_ICON
        } else {
            val parent = requireNotNull(categories[template.parentCategoryId]) {
                "模板缺少一级分类：${template.moduleBillId}"
            }
            icons.get(parent.categoryName, template.childCategoryId?.let { categories[it]?.categoryName },
                income, parent.iconUrl)
        }
        when {
            iconName.startsWith("font:") -> {
                icon.visibility = View.GONE
                iconText.visibility = View.VISIBLE
                iconText.text = iconName.removePrefix("font:")
                iconText.setTextColor(state.text)
            }
            iconName.startsWith("lineIcon/") -> {
                icon.visibility = View.VISIBLE
                iconText.visibility = View.GONE
                val bitmap = bitmaps.get(iconName) ?: context.assets.open("yimu/$iconName").use {
                    requireNotNull(BitmapFactory.decodeStream(it)).also { loaded -> bitmaps.put(iconName, loaded) }
                }
                icon.setImageBitmap(bitmap)
            }
            else -> {
                icon.visibility = View.VISIBLE
                iconText.visibility = View.GONE
                val resource = context.resources.getIdentifier(iconName, "mipmap", context.packageName)
                require(resource != 0) { "缺少分类图标：$iconName" }
                icon.setImageResource(resource)
            }
        }
        icon.imageTintList = ColorStateList.valueOf(state.text)
    }

    private fun categoryTitle(template: ModuleBillEntity, income: Boolean): String {
        val parent = categories[template.parentCategoryId] ?: return OTHER_TEXT
        val child = template.childCategoryId?.let { categories[it] }
        val names = icons.names(parent.categoryName, child?.categoryName, income)
        return if (names.second.isNullOrBlank()) names.first else names.first + "-" + names.second
    }

    private fun transferTitle(template: ModuleBillEntity): String {
        val from = assets[template.fromAssetId] ?: DELETED
        val to = assets[template.toAssetId] ?: DELETED
        return from + "→" + to
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)

    private companion object {
        const val DELETED = "已删账户"
        const val LOCKED_TEXT = "已锁定，可直接添加"
        const val UNLOCKED_TEXT = "未锁定"
        const val OTHER_TEXT = "其他"
        const val TRANSFER_ICON = "ic_transfer_accounts"
    }
}

private fun View.text(id: Int): TextView = requireNotNull(findViewById(id))
