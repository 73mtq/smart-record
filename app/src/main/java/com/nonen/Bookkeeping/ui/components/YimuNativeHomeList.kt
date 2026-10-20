package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.LruCache
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.ui.theme.YimuTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun YimuNativeHomeList(
    transactions: List<TransactionEntity>,
    today: LocalDate,
    hideAmount: Boolean,
    noteFirst: Boolean,
    simpleCategory: Boolean,
    showTime: Boolean,
    showAssetIcon: Boolean,
    selected: Set<Long>,
    onEdit: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onLongSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    onBudget: () -> Unit,
    onChooseMonth: () -> Unit,
    onOpenTemplates: () -> Unit,
    onScrolled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val palette = NativeHomePalette(
        text = colors.onSurface.toArgb(),
        secondary = colors.onSurfaceVariant.toArgb(),
        summaryTitle = if (dark) YimuTokens.NightText.toArgb() else YimuTokens.TextGray.toArgb(),
        monthPay = YimuTokens.Primary.toArgb(),
        surface = colors.surface.toArgb(),
        selection = colors.primaryContainer.toArgb(),
        primary = colors.primary.toArgb(),
        onPrimary = colors.onPrimary.toArgb(),
    )
    val state = NativeHomeState(transactions, today, hideAmount, noteFirst, simpleCategory, showTime, showAssetIcon,
        selected.toSet(), palette)
    val callbacks = NativeHomeCallbacks(onEdit, onToggleSelection, onLongSelect, onAdd, onBudget, onChooseMonth,
        onOpenTemplates, onScrolled)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val themed = ContextThemeWrapper(context, R.style.YimuReferenceTextTheme)
            FrameLayout(themed).apply {
                val adapter = NativeHomeAdapter(themed)
                addView(RecyclerView(themed).apply {
                    id = R.id.data_list
                    layoutManager = LinearLayoutManager(themed)
                    this.adapter = adapter
                    itemAnimator = null
                    clipToPadding = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    setPadding(0, 0, 0, themed.dp(15))
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            adapter.callbacks?.onScrolled?.invoke(recyclerView.canScrollVertically(-1))
                        }
                    })
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    .apply {
                        marginStart = themed.dp(15)
                        marginEnd = themed.dp(15)
                    })
            }
        },
        update = { root ->
            val list = root.findViewById<RecyclerView>(R.id.data_list)
            (list.adapter as NativeHomeAdapter).update(state, callbacks)
        },
    )
}

class YimuNativeSummaryBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 与 ConstraintLayout 的 dimensionRatio=980:376 一致：按宽度精确约束高度，向下取整
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = width * 376 / 980
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    }
}

private data class NativeHomePalette(
    val text: Int,
    val secondary: Int,
    val summaryTitle: Int,
    val monthPay: Int,
    val surface: Int,
    val selection: Int,
    val primary: Int,
    val onPrimary: Int,
)

private data class NativeHomeState(
    val transactions: List<TransactionEntity>,
    val today: LocalDate,
    val hideAmount: Boolean,
    val noteFirst: Boolean,
    val simpleCategory: Boolean,
    val showTime: Boolean,
    val showAssetIcon: Boolean,
    val selected: Set<Long>,
    val palette: NativeHomePalette,
)

private data class NativeHomeCallbacks(
    val onEdit: (Long) -> Unit,
    val onToggleSelection: (Long) -> Unit,
    val onLongSelect: (Long) -> Unit,
    val onAdd: () -> Unit,
    val onBudget: () -> Unit,
    val onChooseMonth: () -> Unit,
    val onOpenTemplates: () -> Unit,
    val onScrolled: (Boolean) -> Unit,
)

private sealed interface NativeHomeRow {
    data object Summary : NativeHomeRow
    data class Day(val date: LocalDate, val transactions: List<TransactionEntity>) : NativeHomeRow
    data class Bill(val transaction: TransactionEntity, val first: Boolean, val last: Boolean) : NativeHomeRow
    data object Empty : NativeHomeRow
}

private class NativeHomeAdapter(private val context: Context) : RecyclerView.Adapter<NativeHomeHolder>() {
    private val inflater = LayoutInflater.from(context).cloneInContext(context).apply {
        factory2 = object : LayoutInflater.Factory2 {
            override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
                when (name) {
                    "TextView" -> AppCompatTextView(context, attrs)
                    "ImageView" -> AppCompatImageView(context, attrs)
                    else -> null
                }

            override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
                onCreateView(null, name, context, attrs)
        }
    }
    private val icons = CategoryIconCatalog(context)
    private val bitmaps = LruCache<String, Bitmap>(64)
    private lateinit var state: NativeHomeState
    private var rows: List<NativeHomeRow> = emptyList()
    var callbacks: NativeHomeCallbacks? = null

    fun update(next: NativeHomeState, nextCallbacks: NativeHomeCallbacks) {
        callbacks = nextCallbacks
        if (::state.isInitialized && state == next) return
        state = next
        rows = buildList {
            add(NativeHomeRow.Summary)
            next.transactions.groupBy { localDateOf(it.timestamp) }.toSortedMap(compareByDescending { it })
                .forEach { (date, transactions) ->
                    add(NativeHomeRow.Day(date, transactions))
                    transactions.forEachIndexed { index, transaction ->
                        add(NativeHomeRow.Bill(transaction, index == 0, index == transactions.lastIndex))
                    }
                }
            if (next.transactions.isEmpty()) add(NativeHomeRow.Empty)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) = when (rows[position]) {
        NativeHomeRow.Summary -> R.layout.yimu_home_summary
        is NativeHomeRow.Day -> R.layout.yimu_home_day
        is NativeHomeRow.Bill -> R.layout.yimu_home_bill
        NativeHomeRow.Empty -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NativeHomeHolder {
        val view = if (viewType == 0) TextView(context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(280))
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setText(R.string.yimu_home_empty)
        } else inflater.inflate(viewType, parent, false)
        return NativeHomeHolder(view)
    }

    override fun onBindViewHolder(holder: NativeHomeHolder, position: Int) {
        when (val row = rows[position]) {
            NativeHomeRow.Summary -> bindSummary(holder.itemView)
            is NativeHomeRow.Day -> bindDay(holder.itemView, row)
            is NativeHomeRow.Bill -> bindBill(holder.itemView, row)
            NativeHomeRow.Empty -> (holder.itemView as TextView).setTextColor(state.palette.secondary)
        }
    }

    private fun money(amount: Double) = if (state.hideAmount) "***" else String.format(Locale.US, "%,.2f", amount)

    private fun bindSummary(view: View) {
        val income = state.transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = -state.transactions.filter { it.amount < 0 }.sumOf { it.amount }
        view.text(R.id.month_income).text = money(income)
        view.text(R.id.month_balance).text = money(income - expense)
        view.text(R.id.month_pay).text = money(expense)
        intArrayOf(R.id.month_income_title, R.id.month_balance_title, R.id.month_budget_title,
            R.id.month_pay_title).forEach { view.text(it).setTextColor(state.palette.summaryTitle) }
        intArrayOf(R.id.month_income, R.id.month_balance, R.id.month_budget)
            .forEach { view.text(it).setTextColor(state.palette.text) }
        view.text(R.id.month_budget).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        }
        view.text(R.id.month_pay).setTextColor(state.palette.monthPay)
        val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
        intArrayOf(R.id.month_income, R.id.month_balance).forEach { id ->
            view.text(id).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = boldTypeface
            }
        }
        view.findViewById<View>(R.id.home_background).setOnClickListener { callbacks?.onChooseMonth?.invoke() }
        view.findViewById<View>(R.id.budget_setting).setOnClickListener { callbacks?.onBudget?.invoke() }
        view.findViewById<View>(R.id.add_bill_btn).apply {
            background = background(state.palette.primary, allCorners())
            setOnClickListener { callbacks?.onAdd?.invoke() }
            isLongClickable = true
            setOnLongClickListener { callbacks?.onOpenTemplates?.invoke(); true }
        }
        view.findViewById<ImageView>(R.id.add_bill_icon).imageTintList = ColorStateList.valueOf(state.palette.onPrimary)
        view.text(R.id.add_bill_label).setTextColor(state.palette.onPrimary)
    }

    private fun bindDay(view: View, row: NativeHomeRow.Day) {
        view.text(R.id.date_info).apply {
            text = when (row.date) {
                state.today -> "今天"
                state.today.minusDays(1) -> "昨天"
                else -> row.date.format(DateTimeFormatter.ofPattern("M月d日"))
            }
            setTextColor(state.palette.text)
        }
        view.text(R.id.week_info).text = row.date.format(DateTimeFormatter.ofPattern("EEE", Locale.CHINA))
        intArrayOf(R.id.week_info, R.id.income_label, R.id.pay_label)
            .forEach { view.text(it).setTextColor(state.palette.secondary) }
        view.text(R.id.income).apply {
            text = money(row.transactions.filter { it.amount > 0 }.sumOf { it.amount })
            setTextColor(YimuTokens.Income.toArgb())
        }
        view.text(R.id.pay).apply {
            text = money(-row.transactions.filter { it.amount < 0 }.sumOf { it.amount })
            setTextColor(YimuTokens.Pay.toArgb())
        }
    }

    private fun bindBill(view: View, row: NativeHomeRow.Bill) {
        val transaction = row.transaction
        val names = icons.names(transaction.category, transaction.subCategory, transaction.amount > 0)
        val categoryTitle = if (state.simpleCategory && !names.second.isNullOrBlank()) requireNotNull(names.second)
            else listOfNotNull(names.first, names.second?.takeIf(String::isNotBlank)).joinToString("-")
        val noteFirst = state.noteFirst && !transaction.note.isNullOrBlank()
        view.text(R.id.category).apply {
            text = if (noteFirst) transaction.note else categoryTitle
            setTextColor(state.palette.text)
        }
        view.text(R.id.remark).apply {
            text = if (noteFirst) categoryTitle else transaction.note.orEmpty()
            setTextColor(state.palette.secondary)
        }
        view.text(R.id.time).apply {
            visibility = if (state.showTime) View.VISIBLE else View.GONE
            text = Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
            setTextColor(state.palette.secondary)
        }
        view.findViewById<View>(R.id.second_layout).visibility =
            if (state.showTime || !transaction.note.isNullOrBlank()) View.VISIBLE else View.GONE
        view.text(R.id.cost).apply {
            text = money(transaction.amount)
            setTextColor(if (transaction.amount < 0) YimuTokens.Pay.toArgb() else YimuTokens.Income.toArgb())
        }
        val hasAsset = state.showAssetIcon && !transaction.account.isNullOrBlank()
        view.text(R.id.asset).apply {
            visibility = if (hasAsset) View.VISIBLE else View.GONE
            text = transaction.account.orEmpty()
            setTextColor(state.palette.secondary)
        }
        view.findViewById<ImageView>(R.id.asset_icon).apply {
            visibility = if (hasAsset) View.VISIBLE else View.GONE
            if (hasAsset) setImageResource(R.mipmap.ic_asset_not_check) else setImageDrawable(null)
            imageTintList = ColorStateList.valueOf(state.palette.secondary)
        }
        val selecting = state.selected.isNotEmpty()
        val icon = view.findViewById<ImageView>(R.id.icon)
        val iconText = view.text(R.id.icon_text)
        val iconName = icons.get(transaction.category, transaction.subCategory, transaction.amount > 0, null)
        icon.visibility = if (selecting || iconName.startsWith("font:")) View.GONE else View.VISIBLE
        iconText.visibility = if (!selecting && iconName.startsWith("font:")) View.VISIBLE else View.GONE
        iconText.text = iconName.removePrefix("font:")
        iconText.setTextColor(state.palette.text)
        if (iconName.startsWith("lineIcon/")) {
            val bitmap = bitmaps.get(iconName) ?: context.assets.open("yimu/$iconName").use {
                requireNotNull(BitmapFactory.decodeStream(it)).also { bitmap -> bitmaps.put(iconName, bitmap) }
            }
            icon.setImageBitmap(bitmap)
        } else if (!iconName.startsWith("font:")) {
            val resource = context.resources.getIdentifier(iconName, "mipmap", context.packageName)
            require(resource != 0) { "Missing category icon: $iconName" }
            icon.setImageResource(resource)
        }
        icon.imageTintList = ColorStateList.valueOf(state.palette.text)
        view.findViewById<RadioButton>(R.id.radio_button).apply {
            visibility = if (selecting) View.VISIBLE else View.GONE
            isChecked = transaction.id in state.selected
            buttonTintList = ColorStateList.valueOf(state.palette.primary)
        }
        val radius = context.dp(8).toFloat()
        val corners = floatArrayOf(
            if (row.first) radius else 0f, if (row.first) radius else 0f,
            if (row.first) radius else 0f, if (row.first) radius else 0f,
            if (row.last) radius else 0f, if (row.last) radius else 0f,
            if (row.last) radius else 0f, if (row.last) radius else 0f,
        )
        view.background = background(
            if (transaction.id in state.selected) state.palette.selection else state.palette.surface,
            corners,
        )
        view.setOnClickListener {
            if (state.selected.isEmpty()) callbacks?.onEdit?.invoke(transaction.id)
            else callbacks?.onToggleSelection?.invoke(transaction.id)
        }
        view.setOnLongClickListener {
            callbacks?.onLongSelect?.invoke(transaction.id)
            true
        }
    }

    private fun allCorners() = FloatArray(8) { context.dp(8).toFloat() }

    private fun background(color: Int, corners: FloatArray) = RippleDrawable(
        ColorStateList.valueOf(YimuTokens.PrimaryAlpha10.toArgb()),
        GradientDrawable().apply { setColor(color); cornerRadii = corners },
        null,
    )
}

private class NativeHomeHolder(view: View) : RecyclerView.ViewHolder(view)
private fun View.text(id: Int): TextView = requireNotNull(findViewById(id))
private fun Context.dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
