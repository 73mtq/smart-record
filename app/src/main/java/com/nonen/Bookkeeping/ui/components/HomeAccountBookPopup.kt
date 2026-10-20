package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.BookEntity
import kotlin.math.roundToInt

class HomeAccountBookPopup(
    context: Context,
    private val anchor: View,
    books: List<BookEntity>,
    onSelect: (BookEntity) -> Unit,
    onCreate: () -> Unit,
    onManage: () -> Unit,
    onDismissed: () -> Unit,
) : PopupWindow() {
    init {
        val content = LayoutInflater.from(context).inflate(R.layout.yimu_home_account_book_popup, null, false)
        content.findViewById<ImageView>(R.id.add_icon).imageTintList =
            ColorStateList.valueOf(context.getColor(R.color.textColorPrimary))
        content.findViewById<ImageView>(R.id.account_book_manager).imageTintList =
            ColorStateList.valueOf(context.getColor(R.color.iconTint))
        val list = content.findViewById<MaxHeightRecyclerView>(R.id.data_list)
        list.maxHeight = (500 * context.resources.displayMetrics.density).toInt()
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = BookAdapter(books) { book ->
            dismiss()
            onSelect(book)
        }
        content.findViewById<View>(R.id.add_account_book).setOnClickListener {
            dismiss()
            onCreate()
        }
        content.findViewById<View>(R.id.account_book_manager).setOnClickListener {
            dismiss()
            onManage()
        }
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        setContentView(content)
        width = content.measuredWidth
        height = content.measuredHeight
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setOnDismissListener { onDismissed() }
    }

    fun show() {
        val density = anchor.resources.displayMetrics.density
        showAsDropDown(anchor, (6 * density).roundToInt(), -(10 * density).roundToInt())
    }

    private class BookAdapter(
        private val books: List<BookEntity>,
        private val onClick: (BookEntity) -> Unit,
    ) : RecyclerView.Adapter<BookAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_choice_account_book, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val book = books[position]
            holder.itemView.findViewById<TextView>(R.id.name).text = book.bookName
            holder.itemView.findViewById<ImageView>(R.id.book_icon).apply {
                setImageResource(R.mipmap.ic_main_account_book)
                imageTintList = ColorStateList.valueOf(context.getColor(R.color.textColorPrimary))
            }
            holder.itemView.findViewById<View>(R.id.tick).visibility = View.GONE
            holder.itemView.setOnClickListener { onClick(book) }
        }

        override fun getItemCount(): Int = books.size
    }
}
