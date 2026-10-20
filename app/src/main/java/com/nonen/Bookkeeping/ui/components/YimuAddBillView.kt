package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.util.TypedValue
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.ui.screens.AmountKeyboardState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class AddBillUiState(
    val isIncome: Boolean,
    val transactionType: String,
    val categories: List<CategoryEntity>,
    val parentCategoryId: Int?,
    val childCategoryId: Int?,
    val keyboard: AmountKeyboardState,
    val note: String,
    val tag: String,
    val timestamp: Long,
    val account: String,
    val assets: List<AssetEntity>,
    val assetId: Long,
    val transferToAssetId: Long?,
    val transferServiceCharge: Double,
    val saving: Boolean,
)

class YimuAddBillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onBack()
        fun onManageCategories()
        fun onManageAssets()
        fun onModeChange(mode: Int)
        fun onSelectParent(category: CategoryEntity)
        fun onSelectChild(parent: CategoryEntity, child: CategoryEntity)
        fun onKeyboardChange(state: AmountKeyboardState)
        fun onNoteChange(note: String)
        fun onShowDate()
        fun onShowAsset(from: Boolean)
        fun onShowTag()
        fun onShowFee()
        fun onSave(again: Boolean)
        fun onCancel()
        fun onInvalidDivision()
        fun onUnsupported()
    }

    private val iconCatalog = CategoryIconCatalog(context)
    private val iconBitmaps = LruCache<String, Bitmap>(64)
    private val fontIconBitmaps = LruCache<String, Bitmap>(16)
    private val density = resources.displayMetrics.density
    private val itemWidth = resources.displayMetrics.widthPixels / 5
    private val parentHeight = dp(67)
    private val parentMargin = dp(5)

    init {
        LayoutInflater.from(context).inflate(R.layout.yimu_add_bill, this, true)
    }

    private val btnBack = findViewById<View>(R.id.btn_back)
    private val tabExpense = findViewById<ViewGroup>(R.id.tab_expense)
    private val tabIncome = findViewById<ViewGroup>(R.id.tab_income)
    private val tabTransfer = findViewById<ViewGroup>(R.id.tab_transfer)
    private val tabDebt = findViewById<ViewGroup>(R.id.tab_debt)
    private val editButton = findViewById<TextView>(R.id.edit)
    private val expenseList = findViewById<RecyclerView>(R.id.bill_parent_type_list)
    private val incomeList = findViewById<RecyclerView>(R.id.bill_income_type_list)
    private val transferPanel = findViewById<View>(R.id.transfer_panel)
    private val debtPlaceholder = findViewById<TextView>(R.id.debt_placeholder)
    private val bottomLayout = findViewById<View>(R.id.bottom_Layout)
    private val categoryPager = findViewById<View>(R.id.category_pager)
    private val tagButton = findViewById<View>(R.id.tag_btn)
    private val tagName = findViewById<TextView>(R.id.tag_btn_name)
    private val remark = findViewById<EditText>(R.id.remark)
    private val cost = findViewById<TextView>(R.id.cost)
    private val dateLayout = findViewById<View>(R.id.date_layout)
    private val dateText = findViewById<TextView>(R.id.date)
    private val assetLayout = findViewById<View>(R.id.asset_layout)
    private val assetName = findViewById<TextView>(R.id.asset_name)
    private val assetIcon = findViewById<ImageView>(R.id.asset_icon)
    private val reimbursementLayout = findViewById<View>(R.id.reimbursement_layout)
    private val addFileLayout = findViewById<View>(R.id.add_file_layout)
    private val discountLayout = findViewById<View>(R.id.discount_layout)
    private val configLayout = findViewById<View>(R.id.config_layout)
    private val serviceChargeLayout = findViewById<View>(R.id.service_charge_layout)
    private val serviceChargeText = findViewById<TextView>(R.id.service_charge)
    private val keyboard = findViewById<YimuNumberKeyboardView>(R.id.number_key_board)
    private val fromAssetName = findViewById<TextView>(R.id.from_asset_name)
    private val toAssetName = findViewById<TextView>(R.id.to_asset_name)
    private val transferFrom = findViewById<View>(R.id.choice_from_asset)
    private val transferTo = findViewById<View>(R.id.choice_to_asset)

    private val expenseAdapter = CategoryAdapter(false)
    private val incomeAdapter = CategoryAdapter(true)
    private var listener: Listener? = null
    private var currentMode = 0
    private var lastVmMode = -1
    private var expandedParentId: Int? = null
    private var lastSelectedParentId: Int? = null
    private var lastState: AddBillUiState? = null
    private var categoryKey: Any? = null
    private var syncingNote = false

    init {
        val expenseLayout = GridLayoutManager(context, 5)
        expenseLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (expenseAdapter.getItemViewType(position) == 1) 5 else 1
        }
        expenseList.layoutManager = expenseLayout
        expenseList.adapter = expenseAdapter
        expenseList.itemAnimator = null
        incomeList.layoutManager = GridLayoutManager(context, 5)
        incomeList.adapter = incomeAdapter
        incomeList.itemAnimator = null
        btnBack.setOnClickListener { listener?.onBack() }
        editButton.setOnClickListener { listener?.onManageCategories() }
        tabExpense.setOnClickListener { selectMode(0) }
        tabIncome.setOnClickListener { selectMode(1) }
        tabTransfer.setOnClickListener { selectMode(2) }
        tabDebt.setOnClickListener { selectMode(3) }
        tagButton.setOnClickListener { listener?.onShowTag() }
        dateLayout.setOnClickListener { listener?.onShowDate() }
        assetLayout.setOnClickListener { listener?.onShowAsset(true) }
        transferFrom.setOnClickListener { listener?.onShowAsset(true) }
        transferTo.setOnClickListener { listener?.onShowAsset(false) }
        reimbursementLayout.setOnClickListener { listener?.onUnsupported() }
        addFileLayout.setOnClickListener { listener?.onUnsupported() }
        discountLayout.setOnClickListener { listener?.onUnsupported() }
        configLayout.setOnClickListener { listener?.onUnsupported() }
        serviceChargeLayout.setOnClickListener { listener?.onShowFee() }
        remark.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!syncingNote) listener?.onNoteChange(s?.toString().orEmpty())
            }
        })
    }

    fun render(state: AddBillUiState, listener: Listener) {
        this.listener = listener
        lastState = state
        val mode = when {
            state.transactionType == "TRANSFER" -> 2
            state.isIncome -> 1
            else -> 0
        }
        if (mode != lastVmMode) {
            lastVmMode = mode
            currentMode = mode
        }
        if (state.parentCategoryId != lastSelectedParentId) {
            lastSelectedParentId = state.parentCategoryId
            expandedParentId = state.parentCategoryId
        }
        applyModeViews()
        bindTabs()
        val key = listOf(state.isIncome, currentMode, state.categories, state.parentCategoryId, state.childCategoryId)
        if (key != categoryKey) {
            categoryKey = key
            bindCategories(state)
        }
        bindNoteRow(state)
        bindMenuRow(state)
        keyboard.bind(
            state = state.keyboard,
            repeatEnabled = true,
            onChange = { listener.onKeyboardChange(it) },
            onSave = { listener.onSave(it) },
            onCancel = { listener.onCancel() },
            onInvalidDivision = { listener.onInvalidDivision() },
        )
    }

    private fun selectMode(mode: Int) {
        currentMode = mode
        applyModeViews()
        bindTabs()
        lastState?.let { bindCategories(it) }
        if (mode < 3) listener?.onModeChange(mode)
    }

    private fun applyModeViews() {
        val expense = currentMode == 0
        val income = currentMode == 1
        val transfer = currentMode == 2
        val debt = currentMode == 3
        expenseList.visibility = if (expense) View.VISIBLE else View.GONE
        incomeList.visibility = if (income) View.VISIBLE else View.GONE
        transferPanel.visibility = if (transfer) View.VISIBLE else View.GONE
        debtPlaceholder.visibility = if (debt) View.VISIBLE else View.GONE
        bottomLayout.visibility = if (debt) View.GONE else View.VISIBLE
        categoryPager.visibility = View.VISIBLE
        dateLayout.visibility = View.VISIBLE
        assetLayout.visibility = if (expense || income) View.VISIBLE else View.GONE
        reimbursementLayout.visibility = if (expense) View.VISIBLE else View.GONE
        addFileLayout.visibility = View.VISIBLE
        discountLayout.visibility = if (expense || income) View.VISIBLE else View.GONE
        configLayout.visibility = if (income) View.VISIBLE else View.GONE
        serviceChargeLayout.visibility = if (transfer) View.VISIBLE else View.GONE
    }

    private fun bindTabs() {
        val tabs = listOf(tabExpense, tabIncome, tabTransfer, tabDebt)
        tabs.forEachIndexed { index, tab ->
            val selected = index == currentMode
            val text = tab.getChildAt(0) as TextView
            val indicator = tab.getChildAt(1)
            text.setTextColor(ContextCompat.getColor(context,
                if (selected) R.color.black else R.color.grey))
            indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun bindCategories(state: AddBillUiState) {
        val parents = state.categories.filter { it.parentId == 0 }
        val children = state.categories.filter { it.parentId != 0 }
        if (currentMode == 1) {
            val incomeParent = parents.firstOrNull { it.categoryId == 9 } ?: parents.firstOrNull()
            val items = incomeParent?.let { parent -> children.filter { it.parentId == parent.categoryId } }
                ?: parents
            val selectedChild = state.childCategoryId
                ?: items.firstOrNull { it.categoryName == "其他" }?.categoryId
            incomeAdapter.submit(
                items.map { category ->
                    CategoryRow.Node(category, false, incomeParent?.categoryName ?: "收入", category.categoryName)
                } + CategoryRow.Node(null, true, "", null),
                null,
                selectedChild,
            )
        } else {
            val rows = mutableListOf<CategoryRow>()
            parents.chunked(5).forEach { chunk ->
                chunk.forEach { rows += CategoryRow.Node(it, false, it.categoryName, null) }
                val open = chunk.firstOrNull { it.categoryId == expandedParentId }
                if (open != null) {
                    val list = children.filter { it.parentId == open.categoryId }
                    rows += CategoryRow.Panel(open, list)
                }
            }
            rows += CategoryRow.Node(null, true, "", null)
            expenseAdapter.submit(rows, state.parentCategoryId, state.childCategoryId)
        }
    }

    private fun bindNoteRow(state: AddBillUiState) {
        tagButton.visibility = if (currentMode == 2 || currentMode == 3) View.GONE else View.VISIBLE
        tagName.text = state.tag.ifBlank { context.getString(R.string.yimu_label) }
        cost.text = state.keyboard.display.ifEmpty { "0.00" }
        cost.scrollX = 0
        cost.setTextColor(ContextCompat.getColor(context, when (currentMode) {
            0 -> R.color.moneyPay
            1 -> R.color.moneyIncome
            else -> R.color.black
        }))
        if (remark.text.toString() != state.note) {
            syncingNote = true
            remark.setText(state.note)
            remark.setSelection(remark.text.length)
            syncingNote = false
        }
    }

    private fun bindMenuRow(state: AddBillUiState) {
        val date = Instant.ofEpochMilli(state.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        dateText.text = if (date == LocalDate.now()) {
            context.getString(R.string.yimu_today)
        } else {
            date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
        }
        assetName.text = state.account.ifBlank { context.getString(R.string.yimu_no_asset) }
        assetIcon.setImageResource(if (state.account.isBlank()) R.mipmap.ic_no_asset else R.mipmap.ic_main_account_book)
        val from = state.assets.firstOrNull { it.assetId == state.assetId }?.assetName
            ?: state.account.takeIf { it.isNotBlank() }
        val to = state.assets.firstOrNull { it.assetId == state.transferToAssetId }?.assetName
        fromAssetName.text = from.orEmpty()
        toAssetName.text = to.orEmpty()
        serviceChargeText.text = context.getString(R.string.yimu_service_charge)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private fun fontIconBitmap(glyph: String): Bitmap {
        val size = dp(30)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25f, resources.displayMetrics)
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val metrics = paint.fontMetrics
        canvas.drawText(
            glyph,
            size / 2f,
            (size / 2f - metrics.top / 2f - metrics.bottom / 2f).toInt().toFloat(),
            paint,
        )
        return bitmap
    }

    private sealed class CategoryRow {
        data class Node(
            val category: CategoryEntity?,
            val isNew: Boolean,
            val iconParent: String,
            val iconChild: String?,
        ) : CategoryRow()

        data class Panel(val parent: CategoryEntity, val children: List<CategoryEntity>) : CategoryRow()
    }

    private inner class CategoryAdapter(private val income: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var rows: List<CategoryRow> = emptyList()
        private var selectedParent: Int? = null
        private var selectedChild: Int? = null

        fun submit(rows: List<CategoryRow>, selectedParent: Int?, selectedChild: Int?) {
            this.rows = rows
            this.selectedParent = selectedParent
            this.selectedChild = selectedChild
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is CategoryRow.Panel) 1 else 0

        override fun getItemCount(): Int = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 1) {
                PanelHolder(inflater.inflate(R.layout.yimu_item_all_child_category, parent, false))
            } else {
                NodeHolder(inflater.inflate(R.layout.yimu_item_parent_category, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is CategoryRow.Node -> bindNode(holder as NodeHolder, row)
                is CategoryRow.Panel -> bindPanel(holder as PanelHolder, row)
            }
        }

        private fun bindNode(holder: NodeHolder, row: CategoryRow.Node) {
            val params = RecyclerView.LayoutParams(itemWidth, parentHeight)
            params.topMargin = parentMargin
            params.bottomMargin = parentMargin
            holder.itemView.layoutParams = params
            val category = row.category
            val selected = !row.isNew && (category?.categoryId == selectedParent || category?.categoryId == selectedChild)
            holder.icon.setBackgroundResource(if (selected) R.drawable.shape_type_select else R.drawable.shape_type_normal)
            holder.name.text = if (row.isNew) context.getString(R.string.yimu_new_category) else category?.categoryName.orEmpty()
            holder.name.setTextColor(ContextCompat.getColor(context,
                if (selected) R.color.colorPrimaryText else R.color.textColorBlack))
            holder.name.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            if (row.isNew) {
                holder.icon.visibility = View.VISIBLE
                holder.iconText.visibility = View.GONE
                holder.icon.setImageResource(R.mipmap.ic_add_asset)
                holder.icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textColorBlack))
                holder.more.visibility = View.GONE
            } else {
                applyCategoryIcon(holder.icon, holder.iconText, row.iconParent, row.iconChild, income, category?.iconUrl)
                val hasChildren = category != null &&
                    lastState?.categories?.any { it.parentId == category.categoryId } == true
                holder.more.visibility = if (hasChildren) View.VISIBLE else View.GONE
                holder.more.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context,
                    if (selected) R.color.colorPrimary else R.color.colorPrimaryLightNoAlpha))
            }
            holder.itemView.setOnClickListener {
                if (row.isNew) {
                    listener?.onManageCategories()
                } else if (category != null) {
                    if (income) {
                        listener?.onSelectChild(
                            lastState?.categories?.firstOrNull { it.parentId == 0 && category.parentId == it.categoryId }
                                ?: category,
                            category,
                        )
                    } else {
                        listener?.onSelectParent(category)
                        expandedParentId = if (expandedParentId == category.categoryId) null else category.categoryId
                        lastState?.let { bindCategories(it) }
                    }
                }
            }
        }

        private fun bindPanel(holder: PanelHolder, row: CategoryRow.Panel) {
            holder.itemView.setBackgroundResource(R.drawable.shape_bg_gray)
            val children = row.children.map { CategoryRow.Node(it, false, row.parent.categoryName, it.categoryName) }
            val rows = children + CategoryRow.Node(null, true, "", null)
            holder.list.layoutManager = GridLayoutManager(context, 5)
            holder.list.isNestedScrollingEnabled = false
            holder.list.adapter = ChildAdapter(row.parent, rows)
        }
    }

    private inner class ChildAdapter(
        private val parent: CategoryEntity,
        private val rows: List<CategoryRow.Node>,
    ) : RecyclerView.Adapter<ChildHolder>() {
        override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ChildHolder =
            ChildHolder(LayoutInflater.from(parentView.context).inflate(R.layout.yimu_item_bill_child_type, parentView, false))

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: ChildHolder, position: Int) {
            val row = rows[position]
            val category = row.category
            val selected = category != null && category.categoryId == lastState?.childCategoryId
            holder.icon.setBackgroundResource(if (selected) R.drawable.shape_type_select else R.drawable.shape_type_normal)
            holder.name.text = if (row.isNew) context.getString(R.string.yimu_new_category) else category?.categoryName.orEmpty()
            holder.name.setTextColor(ContextCompat.getColor(context,
                if (selected) R.color.colorPrimaryText else R.color.textColorBlack))
            holder.name.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            if (row.isNew) {
                holder.icon.visibility = View.VISIBLE
                holder.iconText.visibility = View.GONE
                holder.icon.setImageResource(R.mipmap.ic_add_asset)
                holder.icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textColorBlack))
            } else {
                applyCategoryIcon(holder.icon, holder.iconText, parent.categoryName, category?.categoryName, false, category?.iconUrl)
            }
            holder.itemView.setOnClickListener {
                if (row.isNew) {
                    listener?.onManageCategories()
                } else if (category != null) {
                    listener?.onSelectChild(parent, category)
                }
            }
        }
    }

    private fun applyCategoryIcon(
        image: ImageView,
        text: TextView,
        parentName: String,
        childName: String?,
        isIncome: Boolean,
        iconUrl: String?,
    ) {
        val name = runCatching { iconCatalog.get(parentName, childName, isIncome, iconUrl) }.getOrNull()
        if (name == null) {
            image.setImageDrawable(null)
            image.visibility = View.GONE
            text.visibility = View.GONE
            return
        }
        if (name.startsWith("font:")) {
            text.visibility = View.GONE
            image.visibility = View.VISIBLE
            val glyph = name.removePrefix("font:")
            val bitmap = fontIconBitmaps.get(name) ?: fontIconBitmap(glyph).also { fontIconBitmaps.put(name, it) }
            image.setImageBitmap(bitmap)
            image.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textColorBlack))
            return
        }
        text.visibility = View.GONE
        image.visibility = View.VISIBLE
        if (name.startsWith("lineIcon/")) {
            val bitmap = iconBitmaps.get(name) ?: context.assets.open("yimu/$name").use {
                requireNotNull(BitmapFactory.decodeStream(it)).also { value -> iconBitmaps.put(name, value) }
            }
            image.setImageBitmap(bitmap)
        } else {
            val resource = context.resources.getIdentifier(name, "mipmap", context.packageName)
            if (resource == 0) {
                image.setImageDrawable(null)
            } else {
                image.setImageResource(resource)
            }
        }
        image.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textColorBlack))
    }

    private class NodeHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val iconText: TextView = view.findViewById(R.id.icon_text)
        val name: TextView = view.findViewById(R.id.name)
        val more: ImageView = view.findViewById(R.id.icon_more)
    }

    private class PanelHolder(view: View) : RecyclerView.ViewHolder(view) {
        val list: RecyclerView = view.findViewById(R.id.data_list)
    }

    private class ChildHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val iconText: TextView = view.findViewById(R.id.icon_text)
        val name: TextView = view.findViewById(R.id.name)
    }
}
