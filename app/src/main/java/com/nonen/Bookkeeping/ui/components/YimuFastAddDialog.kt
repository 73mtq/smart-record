package com.nonen.Bookkeeping.ui.components

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.core.nlp.ClassificationModelService
import com.nonen.Bookkeeping.core.nlp.QuickRecordResult
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 一木 AI 记账面板复刻（原版 com.wangc.bill.dialog.AddBillDialog，布局 res/layout/dialog_add_bill）。
 * 首页/日历 FAB（add_btn）点击后弹出；保存走本地离线解析（ClassificationModelService.parseQuickRecord，
 * 与 AddEditViewModel.applyQuickText 同一模型），保持应用无网络权限的本地-first 红线。
 *
 * 输入过程中按原版 AddBillDialog.E0 的行为实时分析：解析到金额才显示「分类 / 金额」并点亮添加按钮，
 * 未识别出金额时隐藏分析行、按钮保持禁用。
 */
class YimuFastAddDialog(
    context: Context,
    private val onSaved: () -> Unit = {},
) : Dialog(context, R.style.YimuEditDialogStyle) {

    private lateinit var wordEdit: EditText
    private lateinit var analysisLayout: View
    private lateinit var categoryLayout: View
    private lateinit var categoryInfo: TextView
    private lateinit var numInfo: TextView
    private lateinit var dateText: TextView
    private lateinit var addBtn: View
    private lateinit var addBackground: ImageView
    private var pending: QuickRecordResult? = null
    private var parseJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
        dateText = findViewById(R.id.date_text)
        dateText.text = LocalDate.now().dayOfMonth.toString()
        // 原版 AddBillDialog 无自定义账本图标时不加载 bookIcon（AddBillDialog.java:684），参考实测 book_name 从 paddingStart 15dp 起
        findViewById<View>(R.id.book_icon).visibility = View.GONE
        analysisLayout = findViewById(R.id.analysis_layout)
        categoryLayout = findViewById(R.id.category_layout)
        categoryInfo = findViewById(R.id.category_info)
        numInfo = findViewById(R.id.num_info)
        addBtn = findViewById(R.id.add_btn)
        addBackground = findViewById(R.id.add_background)
        wordEdit = findViewById(R.id.word_edit)
        wordEdit.requestFocus()
        setAddEnabled(false)
        wordEdit.doAfterTextChanged { text -> scheduleAnalysis(text?.toString().orEmpty()) }
        addBtn.setOnClickListener { save() }
        findViewById<View>(R.id.parent_layout).setOnClickListener {
            if (wordEdit.text.isNullOrBlank()) dismiss()
        }
    }

    override fun dismiss() {
        parseJob?.cancel()
        scope.cancel()
        super.dismiss()
    }

    /** 原版 AddBillDialog.E0：输入停顿后解析，解析成功才回填分类/金额并点亮添加按钮。 */
    private fun scheduleAnalysis(text: String) {
        parseJob?.cancel()
        if (text.isBlank()) {
            applyAnalysis(null)
            return
        }
        parseJob = scope.launch {
            delay(150)
            val result = withContext(Dispatchers.Default) {
                ClassificationModelService.getInstance()?.parseQuickRecord(text)
            }
            applyAnalysis(result)
        }
    }

    private fun applyAnalysis(result: QuickRecordResult?) {
        val amount = result?.amount
        if (result == null || amount == null || amount <= 0.0) {
            pending = null
            analysisLayout.visibility = View.GONE
            setAddEnabled(false)
            return
        }
        pending = result
        analysisLayout.visibility = View.VISIBLE
        categoryLayout.visibility = View.VISIBLE
        categoryInfo.text = listOfNotNull(result.category, result.subCategory?.takeIf { it.isNotBlank() })
            .joinToString(" ")
        numInfo.text = String.format(Locale.US, "%.2f", amount)
        dateText.text = result.dateTime.dayOfMonth.toString()
        setAddEnabled(true)
    }

    private fun setAddEnabled(enabled: Boolean) {
        addBtn.isClickable = enabled
        addBtn.isFocusable = enabled
        addBackground.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (enabled) R.color.colorPrimary else R.color.colorPrimaryAlpha30),
        )
    }

    private fun save() {
        val result = pending ?: return
        val amount = result.amount ?: return
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
