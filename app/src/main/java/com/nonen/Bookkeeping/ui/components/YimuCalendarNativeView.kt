package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarLayout
import com.haibin.calendarview.CalendarView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.stats.CalendarDaySummary
import com.nonen.Bookkeeping.ui.calendar.YimuCalendarView
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarUiState(
    val month: YearMonth,
    val selected: LocalDate,
    val summaries: List<CalendarDaySummary>,
    val monthIncome: Double,
    val monthExpense: Double,
    val transactions: List<TransactionEntity>,
)

class YimuCalendarNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {

    interface Listener {
        fun onSelectDate(date: LocalDate)
        fun onMonthChange(month: YearMonth)
        fun onToday()
        fun onAdd(date: LocalDate)
        fun onFastAdd()
        fun onEdit(id: Long)
        fun onMonthClick()
        fun onMore()
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_yimu_calendar_native, this, true)
    }

    private val calendarView = findViewById<YimuCalendarView>(R.id.calendarView)
    private val calendarLayout = findViewById<CalendarLayout>(R.id.calendarLayout)
    private val collapseSpacer = findViewById<View>(R.id.collapse_spacer)
    private val monthText = findViewById<TextView>(R.id.month)
    private val monthIncomeText = findViewById<TextView>(R.id.month_income)
    private val monthPayText = findViewById<TextView>(R.id.month_pay)
    private val monthBalanceText = findViewById<TextView>(R.id.month_balance)
    private val dateText = findViewById<TextView>(R.id.date_text)
    private val dateLunarText = findViewById<TextView>(R.id.date_lunar_text)
    private val dayIncomeText = findViewById<TextView>(R.id.income)
    private val dayPayText = findViewById<TextView>(R.id.pay)
    private val dayBalanceText = findViewById<TextView>(R.id.balance)
    private val tipLayout = findViewById<View>(R.id.tip_layout)
    private val dataLayout = findViewById<View>(R.id.data_layout)
    private val list = findViewById<RecyclerView>(R.id.data_list)
    private val fab = findViewById<View>(R.id.add_btn)
    private val todayButton = findViewById<View>(R.id.back_today)
    private val addButton = findViewById<View>(R.id.add_bill)
    private val moreButton = findViewById<View>(R.id.btn_more)
    private val adapter = BillAdapter()
    private val iconCatalog = CategoryIconCatalog(context)
    private val icons = LruCache<String, Bitmap>(64)
    private var listener: Listener? = null
    private var state: CalendarUiState? = null
    private var dayTransactions: List<TransactionEntity> = emptyList()

    init {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
            override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                val date = LocalDate.of(calendar.year, calendar.month, calendar.day)
                bindHeaderDate(date, calendar.lunar)
                if (date != state?.selected) listener?.onSelectDate(date)
            }
            override fun onCalendarOutOfRange(calendar: Calendar) = Unit
        })
        calendarView.setOnMonthChangeListener { year, month ->
            val next = YearMonth.of(year, month)
            if (next != state?.month) listener?.onMonthChange(next)
        }
        monthText.setOnClickListener { listener?.onMonthClick() }
        todayButton.setOnClickListener { listener?.onToday() }
        addButton.setOnClickListener { state?.let { listener?.onAdd(it.selected) } }
        fab.setOnClickListener { listener?.onFastAdd() }
        moreButton.setOnClickListener { listener?.onMore() }
        calendarLayout.viewTreeObserver.addOnPreDrawListener {
            val expanded = calendarLayout.isExpand
            fab.visibility = if (expanded) View.VISIBLE else View.GONE
            collapseSpacer.visibility = if (expanded) View.GONE else View.VISIBLE
            true
        }
    }

    fun render(state: CalendarUiState, listener: Listener) {
        this.state = state
        this.listener = listener
        monthText.text = "${state.month.year}年${state.month.monthValue}月"
        monthIncomeText.text = money(state.monthIncome)
        monthPayText.text = money(state.monthExpense)
        monthBalanceText.text = money(state.monthIncome - state.monthExpense)
        calendarView.summaries = state.summaries.associateBy { it.date }
        val current = calendarView.selectedCalendar
        if (current == null || current.year != state.selected.year ||
            current.month != state.selected.monthValue || current.day != state.selected.dayOfMonth) {
            calendarView.scrollToCalendar(state.selected.year, state.selected.monthValue, state.selected.dayOfMonth, false)
        }
        dayTransactions = state.transactions
        adapter.submit(state.transactions, onEdit = { listener.onEdit(it) }, icons = { transaction ->
            iconName(transaction)
        }, loader = { name -> loadIcon(name) })
        val empty = state.transactions.isEmpty()
        tipLayout.visibility = if (empty) View.VISIBLE else View.GONE
        dataLayout.visibility = if (empty) View.GONE else View.VISIBLE
        val income = state.transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val pay = -state.transactions.filter { it.amount < 0 }.sumOf { it.amount }
        dayIncomeText.text = money(income)
        dayPayText.text = money(pay)
        dayBalanceText.text = money(income - pay)
        bindHeaderDate(state.selected, calendarView.selectedCalendar?.lunar.orEmpty())
        post { bindHeaderDate(state.selected, calendarView.selectedCalendar?.lunar.orEmpty()) }
    }

    private fun bindHeaderDate(date: LocalDate, lunar: String) {
        dateText.text = if (date == LocalDate.now()) "今天" else date.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))
        dateLunarText.text = lunar
    }

    private fun iconName(transaction: TransactionEntity): String =
        runCatching {
            iconCatalog.get(transaction.category, transaction.subCategory, transaction.amount > 0, null)
        }.getOrNull().orEmpty()

    private fun loadIcon(name: String): Bitmap? {
        if (name.isEmpty() || name.startsWith("font:")) return null
        if (name.startsWith("lineIcon/")) {
            return icons.get(name) ?: context.assets.open("yimu/$name").use {
                requireNotNull(BitmapFactory.decodeStream(it)).also { bitmap -> icons.put(name, bitmap) }
            }
        }
        return null
    }

    private fun money(value: Double): String = String.format(Locale.US, "%,.2f", value)

    private inner class BillAdapter : RecyclerView.Adapter<BillHolder>() {
        private var items: List<TransactionEntity> = emptyList()
        private var onEdit: (Long) -> Unit = {}
        private var iconName: (TransactionEntity) -> String = { "" }
        private var loader: (String) -> Bitmap? = { null }

        fun submit(items: List<TransactionEntity>, onEdit: (Long) -> Unit,
                   icons: (TransactionEntity) -> String, loader: (String) -> Bitmap?) {
            this.items = items
            this.onEdit = onEdit
            this.iconName = icons
            this.loader = loader
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillHolder =
            BillHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_yimu_calendar_bill, parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: BillHolder, position: Int) {
            val transaction = items[position]
            val sub = transaction.subCategory
            val (parentName, childName) = iconCatalog.names(transaction.category, sub, transaction.amount > 0)
            holder.category.text = if (childName.isNullOrBlank()) parentName else "$parentName-$childName"
            holder.remark.text = transaction.note.orEmpty()
            holder.remark.visibility = if (transaction.note.isNullOrBlank()) View.GONE else View.VISIBLE
            holder.cost.text = String.format(Locale.US, "%.2f", transaction.amount)
            holder.cost.setTextColor(ContextCompat.getColor(holder.itemView.context,
                if (transaction.amount < 0) R.color.moneyPay else R.color.moneyIncome))
            val name = iconName(transaction)
            val bitmap = loader(name)
            if (bitmap != null) {
                holder.icon.setImageBitmap(bitmap)
            } else {
                val resource = holder.itemView.context.resources.getIdentifier(name, "mipmap", holder.itemView.context.packageName)
                if (resource != 0) holder.icon.setImageResource(resource) else holder.icon.setImageDrawable(null)
            }
            holder.icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, R.color.black))
            holder.itemView.setOnClickListener { onEdit(transaction.id) }
        }

    }
}

private class BillHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.icon)
    val category: TextView = view.findViewById(R.id.category)
    val remark: TextView = view.findViewById(R.id.remark)
    val cost: TextView = view.findViewById(R.id.cost)
}
