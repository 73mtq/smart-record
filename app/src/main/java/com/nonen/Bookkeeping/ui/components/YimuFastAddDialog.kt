package com.nonen.Bookkeeping.ui.components

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.core.nlp.ClassificationModelService
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 一木 AI 记账面板复刻（原版 com.wangc.bill.dialog.AddBillDialog，布局 res/layout/dialog_add_bill）。
 * 首页/日历 FAB（add_btn）点击后弹出；保存走本地离线解析（ClassificationModelService.parseQuickRecord，
 * 与 AddEditViewModel.applyQuickText 同一模型），保持应用无网络权限的本地-first 红线。
 */
class YimuFastAddDialog(
    context: Context,
    private val onSaved: () -> Unit = {},
) : Dialog(context) {

    private lateinit var wordEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yimu_fast_add)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(android.view.Gravity.BOTTOM)
        window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        findViewById<TextView>(R.id.date_text).text = LocalDate.now().dayOfMonth.toString()
        // 原版 AddBillDialog 无自定义账本图标时不加载 bookIcon（AddBillDialog.java:684），参考实测 book_name 从 paddingStart 15dp 起
        findViewById<android.view.View>(R.id.book_icon).visibility = android.view.View.GONE
        wordEdit = findViewById(R.id.word_edit)
        wordEdit.requestFocus()
        findViewById<android.view.View>(R.id.add_btn).setOnClickListener { save() }
        findViewById<android.view.View>(R.id.parent_layout).setOnClickListener {
            if (wordEdit.text.isNullOrBlank()) dismiss()
        }
    }

    private fun save() {
        val text = wordEdit.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            dismiss()
            return
        }
        val result = ClassificationModelService.getInstance()?.parseQuickRecord(text)
        val amount = result?.amount
        if (amount == null || amount <= 0.0) {
            Toast.makeText(context, R.string.yimu_fast_add_no_amount, Toast.LENGTH_SHORT).show()
            return
        }
        val isIncome = result.isIncome
        val category = result.category
        val subCategory = result.subCategory?.takeIf { it.isNotBlank() }
        val resolved = CategoryMapping.resolve(category, subCategory, isIncome)
        val signed = if (isIncome) amount else -amount
        val timestamp = result.dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entity = TransactionEntity(
            amount = signed,
            category = category,
            subCategory = subCategory,
            parentCategoryId = resolved.parentId,
            childCategoryId = resolved.childId,
            assetId = 1L,
            note = result.remark.trim().ifEmpty { null },
            transactionType = if (isIncome) "INCOME" else "EXPENSE",
            timestamp = timestamp,
            source = "manual",
            hash = HashUtil.transactionHash(timestamp, signed, null, "manual"),
        )
        insert(entity)
    }

    private fun insert(entity: TransactionEntity) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val bookId = SettingsStore(appContext).selectedBookId.first()
            val ok = (appContext as BookkeepingApp).container.transactionRepository
                .insertIfNew(entity.copy(bookId = bookId))
            withContext(Dispatchers.Main) {
                if (ok) {
                    onSaved()
                    dismiss()
                } else {
                    Toast.makeText(appContext, R.string.error_duplicate_record, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
